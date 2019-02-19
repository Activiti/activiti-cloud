#FROM openjdk:11.0-jdk-slim
FROM adoptopenjdk/openjdk11:jdk-11.0.2.9-slim
#FROM adoptopenjdk/openjdk11:jdk-11.0.2.7-alpine-slim
#FROM openjdk:11.0.1-jre-slim-stretch
#RUN apk --update add fontconfig ttf-dejavu msttcorefonts-installer fontconfig update-ms-fonts fc-cache -f
RUN  apt update
RUN apt -y install ttf-mscorefonts-installer fontconfig
RUN fc-cache -f -v
ENV PORT 8080
EXPOSE 8080
COPY target/*.jar /opt/app.jar
WORKDIR /opt
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar

