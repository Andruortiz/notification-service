# Notification Service

Componente de notificaciones multicanal. Recibe solicitudes de cualquier sistema, decide por qué
canal y con qué proveedor entregarlas, ejecuta el envío de forma asíncrona y conserva la
trazabilidad completa de cada uno.

El principio que gobierna el diseño es que **dar de alta un canal o un proveedor no debe obligar a
modificar el núcleo**: un canal se agrega en el catálogo, un proveedor se agrega escribiendo su
adaptador.

---

## Requisitos

| Herramienta | Versión | Nota |
|---|---|---|
| JDK | 21 | Obligatorio. Spring Boot 3.5 no soporta Java 25 y Lombok 1.18.38 tampoco |
| Maven | — | No hace falta instalarlo: use el wrapper `./mvnw` |
| Docker | — | Solo para levantar MongoDB y RabbitMQ en local |

---

## Compilar y probar

```bash
./mvnw clean verify
```

Compila los cinco módulos, ejecuta las pruebas y aplica las verificaciones de arquitectura.
En Windows, use `mvnw.cmd`.

## Ejecutar en local

Levante la infraestructura y arranque el servicio desde el IDE con el perfil `local`:

```bash
docker compose up -d mongodb rabbitmq
```

O levante todo, incluido el servicio, en contenedores:

```bash
docker compose up -d --build
```

| Recurso | Dirección |
|---|---|
| API | http://localhost:8080/api/v1/notifications |
| Estado del servicio | http://localhost:8080/actuator/health |
| Consola de RabbitMQ | http://localhost:15672 (guest / guest) |
| MongoDB | mongodb://localhost:27017 (admin / secret)|

### Probar el flujo completo

```bash
curl -i -X POST http://localhost:8080/api/v1/notifications -H "Content-Type: application/json" -H "X-Tenant-Id: INQ-101" -d '{"externalId":"sol-001","channel":"EMAIL","recipient":"destino@ejemplo.com","content":{"asunto":"Hola","cuerpo":"Primer envio"},"priority":"HIGH"}'
```

Responde `202` con el identificador de seguimiento. Con ese identificador:

```bash
curl http://localhost:8080/api/v1/notifications/{id} -H "X-Tenant-Id: INQ-101"
```

El proveedor simulado decide su comportamiento según el destinatario, para poder provocar cada
escenario a voluntad:

| Destinatario contiene | Resultado |
|---|---|
| `retry` | Fallo transitorio: la notificación queda `RECOVERABLE` |
| `fail` | Fallo definitivo: la notificación queda `FAILED` |
| cualquier otro | Envío aceptado: la notificación queda `DELIVERED` |

---

## Estructura

Cinco módulos Maven. La dirección de las dependencias la impone Maven en tiempo de compilación:
`notification-core` no declara a `notification-adapters`, así que una violación no compila.

```
notification-shared       Tipos transversales: códigos de error, correlación
notification-core         Dominio puro: agregado, entidades, objetos de valor, eventos, políticas
notification-application  Casos de uso y puertos. Sin dependencias de Spring
notification-adapters     REST, MongoDB, RabbitMQ, proveedores y su configuración
notification-bootstrap    Arranque y cableado. El único módulo que conoce a todos
```

Dentro de cada módulo:

```
core/         aggregate · entity · valueobject · event · service · exception · repository
application/  command · query · dto · usecase · port · mapper
adapters/     in/{rest,rabbit,scheduler} · out/{mongo,rabbit,provider} · config
bootstrap/    config · startup · exception · security
```

### Por qué los casos de uso no llevan anotaciones

`notification-application` no depende de Spring. Los casos de uso son clases planas y se registran
como beans en `bootstrap/config/UseCaseConfiguration`. Cuesta unas líneas de configuración por caso
de uso, y a cambio la capa de aplicación se prueba sin levantar un contexto.

---

## Agregar un canal

Sin escribir código: agregue una entrada bajo `notification.catalog.channels` en el perfil
correspondiente.

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

## Agregar un proveedor

Implemente `adapters/out/provider/NotificationProvider` y regístrela como `@Component`. El
enrutador la descubre sola. No hay que tocar el núcleo ni el enrutador, y hay pruebas de
arquitectura que impiden reintroducir esa dependencia.

---

## Verificaciones que bloquean la construcción

| Prueba | Qué impide |
|---|---|
| `HexagonalArchitectureTest` | Que el dominio importe Spring, Mongo o AMQP, y que la aplicación conozca los adaptadores |
| `NoHardcodedChannelTest` | Que el nombre de un canal o proveedor aparezca en el núcleo |
| `ModularityTests` | Que un módulo dependa de otro no declarado en su `package-info` |

`ModularityTests` genera además los diagramas C4 de módulos en `docs/modulith/`. No se versionan
porque se regeneran en cada `./mvnw verify`.

---

## Perfiles

| Perfil | Uso |
|---|---|
| `local` | Máquina del desarrollador. Apunta a `localhost` con las credenciales de los contenedores |
| `dev` | Entorno compartido. Sin valores por defecto: si falta una variable, el servicio no arranca |
| `prod` | Producción. Toda credencial llega por variable de entorno |

Ningún perfil contiene secretos. Los valores sensibles se inyectan por entorno y los personales
van en un `.env`, que está ignorado por git.

---

## Estado actual

Esta es la línea base de arquitectura. Está implementado el recorrido completo de una notificación
—aceptación, encolado, despacho, reintentos y consulta— con un proveedor simulado.

Pendiente para las siguientes etapas: proveedores reales, integración con los componentes de
Seguridad, Parámetros y Mensajes, plantillas, envíos programados, observabilidad completa y
despliegue en Kubernetes.

Cómo entran esos tres componentes sin tocar el núcleo:

| Componente | Dónde encaja |
|---|---|
| Seguridad | Sustituye `bootstrap/security/SecurityPlaceholderConfiguration`; el identificador del cliente pasa a leerse del token en lugar de la cabecera `X-Tenant-Id` |
| Parámetros | Reemplaza `ConfigurationChannelCatalogAdapter` por un cliente REST detrás del mismo puerto |
| Mensajes | Es un consumidor más de la API pública; no requiere cambio alguno |

---

## Flujo de trabajo con Git

Ver `GIT_WORKFLOW_NOTIFICATION_SERVICE.md`. En resumen: las funcionalidades salen de `develop` en
ramas `feature/<nombre>`, con commits convencionales, y se integran por Pull Request tras
`./mvnw clean verify`.
