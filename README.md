# Notification Service

Componente de notificaciones multicanal. Recibe solicitudes de cualquier sistema, determina el canal y el proveedor configurado, ejecuta el envío de forma asíncrona y conserva la trazabilidad completa de cada notificación y sus intentos.

El principio que gobierna el diseño es que **agregar un canal o un proveedor no debe obligar a modificar el núcleo**: el canal se configura en el catálogo y el proveedor se incorpora mediante un adaptador que implementa el contrato común.

---

## Requisitos

| Herramienta | Versión | Nota                                                                             |
| ----------- | ------- | -------------------------------------------------------------------------------- |
| JDK         | 21      | Obligatorio                                                                      |
| Maven       | —       | No hace falta instalarlo: use el wrapper `./mvnw`                                |
| Docker      | —       | Necesario para levantar MongoDB, RabbitMQ y, opcionalmente, el servicio completo |

---

## Compilar y probar

```bash
./mvnw clean verify
```

Compila los módulos, ejecuta las pruebas y aplica las verificaciones de arquitectura.

En Windows:

```bash
mvnw.cmd clean verify
```

---

## Ejecutar en local

### Opción 1: infraestructura en Docker y servicio desde el IDE

Levante únicamente MongoDB y RabbitMQ:

```bash
docker compose up -d mongodb rabbitmq
```

Después ejecute `notification-service` desde el IDE con el perfil `local`.

### Opción 2: todo mediante Docker

```bash
docker compose up -d --build
```

Para ver los logs del servicio:

```bash
docker logs -f notification-service
```

Para detener los servicios:

```bash
docker compose down
```

### Variables de entorno

Las credenciales y secretos no se almacenan en el código ni en los archivos versionados.

Para el entorno local se utilizan variables como:

```env
MONGO_DATABASE=notification
MONGO_USERNAME=Tu_usuario
MONGO_PASSWORD=Tu_contraseña

RABBITMQ_USERNAME=Tu_usuario
RABBITMQ_PASSWORD=Tu_contraseña

BREVO_API_KEY=<api-key-de-brevo>
BREVO_SENDER_EMAIL=<correo-verificado-en-brevo>
BREVO_SENDER_NAME=Notification Service
```

El archivo `.env` debe permanecer fuera del control de versiones.

El `docker-compose.yml` recibe estas variables y las inyecta al contenedor `notification-service`. El perfil `local` las consume mediante las propiedades de Spring.

---

## Recursos locales

| Recurso             | Dirección                                  |
| ------------------- | ------------------------------------------ |
| API                 | http://localhost:8080/api/v1/notifications |
| Estado del servicio | http://localhost:8080/actuator/health      |
| Consola de RabbitMQ | http://localhost:15672                     |
| RabbitMQ AMQP       | localhost:5672                             |
| MongoDB             | mongodb://localhost:27017                  |

Las credenciales de MongoDB, RabbitMQ y Brevo dependen de las variables de entorno configuradas para el entorno local.

---

## Probar el flujo completo

Una solicitud de ejemplo:

```bash
curl -i -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-1" \
  -d '{
    "externalId": "brevo-test-001",
    "channel": "EMAIL",
    "recipient": "destino@ejemplo.com",
    "content": {
      "subject": "Prueba Notification Service + Brevo",
      "body": "<h1>Hola</h1><p>Este correo fue enviado desde Notification Service.</p>"
    },
    "priority": "MEDIUM"
  }'
```

La API responde `202 Accepted` con el identificador de la notificación.

Después puede consultar su estado:

```bash
curl http://localhost:8080/api/v1/notifications/{id} \
  -H "X-Tenant-Id: demo-1"
```

### Estados principales

| Estado        | Significado                                                  |
| ------------- | ------------------------------------------------------------ |
| `PENDING`     | La notificación fue aceptada y está pendiente de despacho    |
| `IN_PROCESS`  | El worker está procesando el envío                           |
| `DELIVERED`   | El proveedor aceptó el envío                                 |
| `RECOVERABLE` | Ocurrió un fallo recuperable y puede ejecutarse otro intento |
| `FAILED`      | El envío terminó en un fallo definitivo                      |
| `DISCARDED`   | La notificación fue descartada sin continuar el envío        |

Cada intento queda registrado en la notificación, permitiendo conservar la trazabilidad del despacho.

---

# Arquitectura

El sistema está organizado en cinco módulos Maven:

```text
notification-shared       Tipos transversales: códigos de error, correlación
notification-core         Dominio puro: agregado, entidades, objetos de valor, eventos y políticas
notification-application  Casos de uso, DTOs y puertos
notification-adapters     REST, MongoDB, RabbitMQ, proveedores y adaptadores externos
notification-bootstrap    Arranque, configuración y cableado de la aplicación
```

La dirección de las dependencias se mantiene hacia el núcleo:

```text
bootstrap
    │
    ├── adapters
    │      │
    │      └── application
    │              │
    │              └── core
    │
    └── application
             │
             └── core
```

El dominio no conoce Spring, MongoDB, RabbitMQ, Brevo ni ningún otro proveedor externo.

Dentro de cada módulo:

```text
core/
    aggregate
    entity
    valueobject
    event
    service
    exception
    repository

application/
    command
    query
    dto
    usecase
    port
    mapper

adapters/
    in/
        rest
        rabbit
        scheduler
    out/
        mongo
        rabbit
        provider
    config

bootstrap/
    config
    startup
    exception
    security
```

### Por qué los casos de uso no llevan anotaciones de Spring

`notification-application` no depende de Spring.

Los casos de uso son clases planas y se registran como beans desde:

```text
bootstrap/config/UseCaseConfiguration
```

Esto permite probar la capa de aplicación sin levantar un contexto completo de Spring.

---

# Flujo de una notificación

El recorrido principal es:

```text
Cliente
   │
   ▼
POST /api/v1/notifications
   │
   ▼
NotificationController
   │
   ▼
SendNotificationUseCase
   │
   ▼
Notification
   │
   ├── se crea en PENDING
   │
   ▼
MongoDB
   │
   ▼
RabbitMQ
   │
   ▼
NotificationDispatchListener
   │
   ▼
DispatchNotificationUseCase
   │
   ▼
ChannelCatalogPort
   │
   │  "¿qué proveedor atiende EMAIL?"
   ▼
ChannelRoute
   │
   │  EMAIL → brevo
   ▼
ProviderRegistrySender
   │
   │  busca providerId = brevo
   ▼
BrevoNotificationProvider
   │
   ▼
Brevo API
   │
   ▼
Notification
   │
   └── DELIVERED / RECOVERABLE / FAILED
```

La aplicación no contiene una condición del tipo:

```java
if (channel == EMAIL) {
    brevo.send(...);
}
```

El canal y el proveedor se relacionan mediante el catálogo.

---

# Catálogo de canales

El catálogo determina **qué proveedor debe utilizarse para cada canal**.

Ejemplo:

```yaml
notification:
  catalog:
    channels:
      EMAIL:
        provider-id: brevo
        enabled: true
        max-attempts: 3
        initial-backoff: PT30S
        backoff-multiplier: 2.0

      SMS:
        provider-id: simulated
        enabled: true
        max-attempts: 2
        initial-backoff: PT15S
        backoff-multiplier: 2.0

      PUSH:
        provider-id: simulated
        enabled: true
        max-attempts: 3
        initial-backoff: PT10S
        backoff-multiplier: 1.5
```

Por ejemplo:

```text
EMAIL → brevo
SMS   → simulated
PUSH  → simulated
```

Por lo tanto, cuando llega una notificación `EMAIL`, el sistema no necesita conocer directamente a Brevo. Consulta el catálogo y obtiene:

```text
provider-id = brevo
```

Después `ProviderRegistrySender` localiza el proveedor registrado con ese identificador.

---

# Proveedores

Los proveedores implementan el contrato:

```text
adapters/out/provider/NotificationProvider
```

El contrato define:

```java
ProviderDescriptor descriptor();

Mono<NotificationSenderPort.SendOutcome> send(Notification notification);
```

Cada proveedor declara:

* su identificador;
* los canales que soporta;
* si está habilitado;
* la operación concreta de envío.

Actualmente existen:

| Proveedor   | Canales                             | Función                                         |
| ----------- | ----------------------------------- | ----------------------------------------------- |
| `simulated` | EMAIL, SMS, PUSH, WHATSAPP, WEBHOOK | Desarrollo, pruebas y simulación                |
| `brevo`     | EMAIL                               | Envío real de correo electrónico mediante Brevo |

### Proveedor simulado

`SimulatedNotificationProvider` permite probar el flujo completo sin depender de un servicio externo.

Su comportamiento se puede provocar mediante el destinatario:

| Destinatario contiene | Resultado         |
| --------------------- | ----------------- |
| `retry`               | Fallo recuperable |
| `fail`                | Fallo definitivo  |
| cualquier otro valor  | Envío aceptado    |

### Proveedor Brevo

`BrevoNotificationProvider` adapta el modelo interno de `Notification` al API de Brevo.

Para EMAIL utiliza:

```text
notification.content.subject
notification.content.body
notification.recipient
```

y construye la solicitud correspondiente para el endpoint SMTP de Brevo.

Las credenciales y datos del remitente llegan mediante variables de entorno:

```text
BREVO_API_KEY
BREVO_SENDER_EMAIL
BREVO_SENDER_NAME
```

La API Key nunca se almacena en el repositorio.

---

# Agregar un canal

Agregar un canal debe ser principalmente una operación de configuración.

Por ejemplo:

```yaml
notification:
  catalog:
    channels:
      WHATSAPP:
        provider-id: simulated
        enabled: true
        max-attempts: 3
        initial-backoff: PT30S
        backoff-multiplier: 2.0
```

La implementación concreta del proveedor determina si realmente puede atender ese canal.

El objetivo es que el núcleo no necesite conocer detalles específicos de cada canal.

---

# Agregar un proveedor

Agregar un proveedor consiste en implementar:

```text
adapters/out/provider/NotificationProvider
```

y registrarlo como bean de Spring:

```java
@Component
public class NuevoProveedor implements NotificationProvider {
    ...
}
```

El proveedor debe declarar su descriptor:

```java
@Override
public ProviderDescriptor descriptor() {
    return new ProviderDescriptor(
            "nuevo-proveedor",
            Set.of("EMAIL"),
            true
    );
}
```

Después implementa:

```java
@Override
public Mono<NotificationSenderPort.SendOutcome> send(
        Notification notification) {
    ...
}
```

El `ProviderRegistrySender` descubre automáticamente todos los `NotificationProvider` registrados como beans.

Por lo tanto, **no es necesario modificar el registry para cada nuevo proveedor**.

El flujo es:

```text
NuevoProveedor
      │
      ▼
@Component
      │
      ▼
Spring lo descubre
      │
      ▼
ProviderRegistrySender
      │
      ▼
providersById["nuevo-proveedor"]
```

Finalmente, el catálogo determina qué canal utiliza el nuevo proveedor:

```yaml
notification:
  catalog:
    channels:
      EMAIL:
        provider-id: nuevo-proveedor
```

De esta forma, cambiar de proveedor no requiere modificar el dominio ni los casos de uso.

---

# Configuración de proveedores

La configuración del proveedor y el enrutamiento del canal son responsabilidades diferentes.

### Configuración del proveedor

Define **cómo conectarse al proveedor**:

```yaml
notification:
  providers:
    brevo:
      api-key: ${BREVO_API_KEY}
      sender-email: ${BREVO_SENDER_EMAIL}
      sender-name: ${BREVO_SENDER_NAME:Notification Service}
```

### Catálogo

Define **qué proveedor utiliza el canal**:

```yaml
notification:
  catalog:
    channels:
      EMAIL:
        provider-id: brevo
```

Por lo tanto:

```text
notification.providers
        │
        └── "¿cómo me conecto con Brevo?"

notification.catalog.channels
        │
        └── "¿quién atiende EMAIL?"
```

Esta separación permite cambiar el proveedor sin modificar el código del núcleo.

---

# Reintentos y fallos

Cada canal puede definir su política:

```yaml
max-attempts: 3
initial-backoff: PT30S
backoff-multiplier: 2.0
```

Los fallos se clasifican según su naturaleza:

```text
                 Fallo
                   │
          ┌────────┴────────┐
          ▼                 ▼
      Recuperable       Definitivo
          │                 │
          ▼                 ▼
    RECOVERABLE           FAILED
          │
          ▼
       Reintento
          │
          ▼
       PENDING
```

El proveedor conoce los códigos y errores específicos de su API, mientras que el caso de uso trabaja con el resultado normalizado:

```text
accepted
recoverableFailure
permanentFailure
```

Esto evita que la aplicación tenga conocimiento de los códigos particulares de Brevo, SendGrid u otros proveedores.

---

# Verificaciones que bloquean la construcción

| Prueba                      | Qué impide                                                                |
| --------------------------- | ------------------------------------------------------------------------- |
| `HexagonalArchitectureTest` | Que el dominio importe Spring, MongoDB, RabbitMQ u otros adaptadores      |
| `NoHardcodedChannelTest`    | Que los canales o proveedores queden acoplados al núcleo                  |
| `ModularityTests`           | Que un módulo dependa de otro sin declarar la dependencia correspondiente |

`ModularityTests` genera además los diagramas C4 de módulos en:

```text
docs/modulith/
```

Estos diagramas se regeneran durante `./mvnw verify` y no se versionan.

---

# Perfiles

| Perfil  | Uso                                                                                                        |
| ------- | ---------------------------------------------------------------------------------------------------------- |
| `local` | Desarrollo local con MongoDB y RabbitMQ en Docker y proveedores configurados mediante variables de entorno |
| `dev`   | Entorno compartido; las variables obligatorias son inyectadas por el entorno                               |
| `prod`  | Producción; las credenciales y secretos son proporcionados por la plataforma                               |

Ningún perfil debe contener secretos.

Los valores sensibles llegan mediante variables de entorno. En desarrollo local pueden mantenerse en `.env`, archivo que debe permanecer ignorado por Git.

---

# Integraciones actuales

### MongoDB

Responsable de la persistencia de las notificaciones, estados e intentos de entrega.

### RabbitMQ

Responsable del despacho asíncrono de las notificaciones.

### Proveedores

Actualmente:

```text
Brevo      → EMAIL real
Simulated  → pruebas y desarrollo
```

La arquitectura permite incorporar proveedores adicionales sin modificar el dominio ni el enrutador.

---

# Estado actual

La línea base actual implementa el recorrido completo:

```text
Aceptación
   ↓
Persistencia
   ↓
Encolado
   ↓
Despacho asíncrono
   ↓
Selección de canal/proveedor
   ↓
Envío
   ↓
Resultado normalizado
   ↓
DELIVERED / RECOVERABLE / FAILED
   ↓
Trazabilidad de intentos
   ↓
Consulta de estado
```

Actualmente están integrados:

* dominio de notificaciones;
* casos de uso de aceptación, despacho y consulta;
* persistencia reactiva con MongoDB;
* despacho asíncrono con RabbitMQ;
* catálogo de canales;
* registro dinámico de proveedores;
* proveedor simulado;
* integración real con Brevo para EMAIL;
* política de reintentos;
* clasificación de resultados de envío;
* trazabilidad de intentos;
* verificaciones de arquitectura y modularidad.

### Próximas etapas

Pendiente para las siguientes etapas:

* integración con el componente de Seguridad;
* integración con el componente de Parámetros;
* integración con el componente de Mensajes;
* plantillas;
* envíos programados;
* observabilidad completa;
* nuevos proveedores y canales;
* pruebas de integración;
* despliegue en Kubernetes.

Cómo entran los componentes externos sin tocar el núcleo:

| Componente | Integración                                                                                                                  |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Seguridad  | Sustituye `bootstrap/security/SecurityPlaceholderConfiguration`; el identificador del cliente pasa de `X-Tenant-Id` al token |
| Parámetros | Puede reemplazar `ConfigurationChannelCatalogAdapter` mediante un cliente REST detrás del mismo puerto                       |
| Mensajes   | Consume la API pública de notificaciones y no requiere modificaciones al dominio                                             |

---

# Flujo de trabajo con Git

Ver `GIT_WORKFLOW_NOTIFICATION_SERVICE.md`.

En resumen:

1. Las funcionalidades parten de `develop`.
2. Se crean ramas `feature/<nombre>`.
3. Se utilizan commits convencionales.
4. Cada cambio debe pasar:

```bash
./mvnw clean verify
```

5. Los cambios se integran mediante Pull Request.
6. No se deben subir secretos, archivos `.env` ni credenciales de proveedores.
git