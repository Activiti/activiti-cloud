#FROM openjdk:11.0-jdk-slim
FROM adoptopenjdk/openjdk11:jdk-11.0.2.7-alpine-slim 
RUN apk --update add fontconfig ttf-dejavu msttcorefonts-installer fontconfig update-ms-fonts fc-cache -f

ENV PORT 8080
EXPOSE 8080
COPY target/*.jar /opt/app.jar
WORKDIR /opt
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar

