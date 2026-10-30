# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Las dependencias se cachean aparte: mientras el pom no cambie, un
# rebuild no vuelve a bajarlas.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -Dmaven.test.skip=true

# ---------- runtime ----------
# Imagen solo con el JRE: mucho mas liviana que la de build.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Campus time zone: the app compares gaps, matches and open tables with the current local time
ENV TZ=America/Bogota

COPY --from=build /build/target/gapfinder-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
