FROM eclipse-temurin:17-jdk-focal
ENV PORT 8080
EXPOSE 8080

WORKDIR /opt
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh
COPY target/*.jar app.jar
COPY liquibase/target/*.jar liquibase.jar

ENTRYPOINT ["./entrypoint.sh"]
CMD ["-jar", "app.jar"]
