# Stage 1: extract layered JAR
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace
ARG JAR_FILE=student-grade-manager-core/target/student-grade-manager-core-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 2: runtime + build tools for BuildWrapper
FROM eclipse-temurin:17-jdk-alpine
RUN apk add --no-cache maven

# App layers
WORKDIR /app
COPY --from=builder /workspace/dependencies/ ./
COPY --from=builder /workspace/spring-boot-loader/ ./
COPY --from=builder /workspace/snapshot-dependencies/ ./
COPY --from=builder /workspace/application/ ./

# Source + scripts for BuildWrapper (mvn verify + claude CLI)
WORKDIR /project
COPY pom.xml ./
COPY student-grade-manager-core/pom.xml student-grade-manager-core/pom.xml
COPY student-grade-manager-core/src/ student-grade-manager-core/src/
COPY student-grade-manager-core/spotbugs-exclude.xml student-grade-manager-core/spotbugs-exclude.xml
COPY scripts/ scripts/

WORKDIR /app
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
