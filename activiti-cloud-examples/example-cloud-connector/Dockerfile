FROM eclipse-temurin:17-jdk-focal
ENV PORT 8080
EXPOSE 8080

WORKDIR /opt
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

COPY starter/target/*.jar app.jar

ENTRYPOINT ["./entrypoint.sh"]
CMD ["-jar", "app.jar"]
