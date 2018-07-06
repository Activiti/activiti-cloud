FROM openjdk:8-jdk-alpine
ENV PORT 8080
EXPOSE 8080
COPY target/*.jar /opt/app.jar
WORKDIR /opt
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar