FROM adoptopenjdk/openjdk11:jdk-11.0.2.7-alpine-slim
#FROM openjdk:11.0.1-jre-slim-stretch
#FROM gcr.io/distroless/java:11-debug
ENV PORT 8080	
EXPOSE 8080	
COPY target/*.jar /opt/app.jar	
WORKDIR /opt	
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
