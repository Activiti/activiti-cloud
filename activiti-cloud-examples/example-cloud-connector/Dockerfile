FROM adoptopenjdk/openjdk11:jdk-11.0.2.7-alpine-slim
#FROM openjdk:11.0.1-jre-slim-stretch
ENV PORT 8080
EXPOSE 8080

WORKDIR /opt
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

COPY starter/target/*.jar app.jar

ENTRYPOINT ["./entrypoint.sh"]
CMD ["-jar", "app.jar"]
