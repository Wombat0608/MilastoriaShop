# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:21-jre-alpine
# Базовый imagemagick в Alpine без JPEG/HEIC — без subpackages convert
# не декодирует .jpg/.heic («no decode delegate»).
# ffmpeg — превью/постеры видео в медиатеке и при attach к лоту.
RUN apk add --no-cache \
        imagemagick \
        imagemagick-jpeg \
        imagemagick-heic \
        imagemagick-webp \
        imagemagick-tiff \
        libheif-jpeg \
        ffmpeg
# IMv7: magick вместо устаревшего convert
ENV CONVERT_BIN=magick
WORKDIR /app
COPY --from=build /app/target/milastoria-app.jar app.jar

VOLUME /app/data
ENV DB_PATH=/app/data/milastoria.db
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
