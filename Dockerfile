FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -B -ntp -DskipTests package dependency:copy-dependencies -DincludeScope=runtime

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p /app/classes /app/libs /app/config \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && chown -R app:app /app

COPY --from=build /build/target/classes /app/classes
COPY --from=build /build/target/dependency /app/libs

USER app

CMD ["java", "-cp", "/app/config:/app/classes:/app/libs/*", "com.litovskiy.Main"]
