FROM clojure:openjdk-8-lein-2.9.1 AS build

WORKDIR /build
COPY . /build

RUN lein uberjar

FROM openjdk:8-jre-slim

WORKDIR /app

COPY --from=build /build/target/uberjar/tankbattle-0.2.0-SNAPSHOT-standalone.jar /app/tankbattle.jar

EXPOSE 3000

CMD ["java", "-jar", "/app/tankbattle.jar"]
