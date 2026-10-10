# Hackathon: Eclipse OpenJ9 (IBM Semeru) instead of Amazon Corretto
FROM docker.io/library/ibm-semeru-runtimes:open-25-jre-noble@sha256:8278bc81988426202699630593d49aeb811c611e22dd205c3cb0ffb572bdef38

# install missing fontconfig package
RUN apt-get update && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core && rm -rf /var/lib/apt/lists/*
RUN fc-cache -f -v
ENV PORT 8080

EXPOSE 8080

WORKDIR /opt
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

COPY liquibase/target/*.jar liquibase.jar
COPY starter/target/*.jar app.jar
COPY rest/target/*.jar rest.jar
COPY consumer/target/*.jar consumer.jar

ENTRYPOINT ["./entrypoint.sh"]
CMD ["-jar", "app.jar"]
