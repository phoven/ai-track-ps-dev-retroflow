# syntax=docker/dockerfile:1

############################
# Stage 1: build
############################
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies first so this layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copy sources and build the executable jar
COPY src ./src
RUN mvn -B -q package -DskipTests

# Split the fat jar into layers (dependencies change rarely, application code often)
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

############################
# Stage 2: runtime
############################
FROM eclipse-temurin:21-jre-alpine AS runtime

# Unprivileged system user/group with no login shell
RUN addgroup -S retroflow && adduser -S -G retroflow -H -s /sbin/nologin retroflow

WORKDIR /app

# Least-frequently-changing layers first for better cache reuse
COPY --from=build --chown=retroflow:retroflow /workspace/extracted/dependencies/ ./
COPY --from=build --chown=retroflow:retroflow /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=retroflow:retroflow /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=retroflow:retroflow /workspace/extracted/application/ ./

USER retroflow

EXPOSE 8080

# Container-aware heap sizing
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1

# exec so the JVM is PID 1 and receives SIGTERM directly
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

