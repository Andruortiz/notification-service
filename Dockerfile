# =============================================================================
# Imagen del componente de notificaciones.
#
# Construccion en dos etapas: la primera compila con el JDK completo y la
# segunda solo lleva el runtime. Asi la imagen que se despliega no arrastra el
# compilador ni el repositorio de dependencias.
# =============================================================================

# ------------------------------ etapa de compilacion ------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Los poms se copian antes que el codigo para que la descarga de dependencias
# quede cacheada: mientras no cambie un pom, esta capa no se vuelve a resolver.
COPY pom.xml .
COPY notification-shared/pom.xml notification-shared/
COPY notification-core/pom.xml notification-core/
COPY notification-application/pom.xml notification-application/
COPY notification-adapters/pom.xml notification-adapters/
COPY notification-bootstrap/pom.xml notification-bootstrap/

RUN mvn -B -ntp dependency:go-offline

COPY notification-shared/src notification-shared/src
COPY notification-core/src notification-core/src
COPY notification-application/src notification-application/src
COPY notification-adapters/src notification-adapters/src
COPY notification-bootstrap/src notification-bootstrap/src

RUN mvn -B -ntp clean package -DskipTests

# ------------------------------ etapa de ejecucion --------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuario sin privilegios: un proceso comprometido no debe correr como root.
RUN addgroup --system notification && adduser --system --ingroup notification notification

WORKDIR /app

COPY --from=build /build/notification-bootstrap/target/notification-bootstrap-*.jar app.jar

RUN chown -R notification:notification /app
USER notification

EXPOSE 8080

# MaxRAMPercentage en lugar de un tamano fijo: la JVM se ajusta al limite de
# memoria que le asigne el orquestador en lugar de ignorarlo.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
