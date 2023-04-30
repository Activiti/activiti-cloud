FROM docker.io/library/alpine:3.17@sha256:124c7d2707904eea7431fffe91522a01e5a861a624ee31d03372cc1d138a3126

RUN apk update && apk add --no-cache openjdk17-jre && rm -rf /var/cache/apk/*

# install missing fontconfig package
RUN apk add --no-cache fontconfig ttf-dejavu
RUN fc-cache -f -v
ENV PORT 8080

EXPOSE 8080

WORKDIR /opt
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

COPY liquibase/target/*.jar liquibase.jar
COPY starter/target/*.jar app.jar

ENTRYPOINT ["./entrypoint.sh"]
CMD ["-jar", "app.jar"]
