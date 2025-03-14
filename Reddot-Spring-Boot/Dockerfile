# syntax=docker/dockerfile:1
FROM --platform=$BUILDPLATFORM maven:3.9.7-amazoncorretto-21 as builder
LABEL authors="nguyentrongtoan"
WORKDIR /app
RUN uname -a
COPY ./pom.xml .
RUN mvn dependency:go-offline
COPY ./src ./src
RUN mvn clean install -DskipTests

FROM builder as dev-envs
RUN <<EOF
# Use a non-root user to limit root access <by default run as root>
RUN groupadd -s spring && useradd -s reddot -g spring
USER reddot:spring
EOF

FROM builder as prepare-production
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM amazoncorretto:21-alpine as runtime
EXPOSE 8080
ARG DEPENDENCY=/app/target/dependency
COPY --from=prepare-production ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=prepare-production ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=prepare-production ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java", "-cp", "app:/app/lib/*", "com.reddot.app.ReddotSpringApplication"]