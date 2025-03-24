# syntax=docker/dockerfile:1
FROM --platform=$BUILDPLATFORM maven:3.9.7-amazoncorretto-21 as builder
LABEL authors="nguyentrongtoan"
WORKDIR /app
COPY pom.xml /app/pom.xml
RUN mvn dependency:go-offline
COPY src /app/src
RUN mvn install -DskipTests

FROM builder as prepare-production
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM amazoncorretto:21-alpine as runtime
EXPOSE 8080
# Use a non-root user to limit root access <by default run as root>
RUN addgroup -S spring && adduser -Ds /bin/sh -u 1001 reddot -G spring
ARG DEPENDENCY=/app/target/dependency
COPY --from=prepare-production ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=prepare-production ${DEPENDENCY}/BOOT-INF/classes /app
COPY --from=prepare-production ${DEPENDENCY}/META-INF /app/META-INF
RUN chown -R reddot:spring /app
USER reddot:spring
ENTRYPOINT ["java", "-cp", "app:/app/lib/*", "com.reddot.app.ReddotSpringApplication"]