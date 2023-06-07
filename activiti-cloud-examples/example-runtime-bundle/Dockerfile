FROM docker.io/library/alpine:3.18@sha256:02bb6f428431fbc2809c5d1b41eab5a68350194fb508869a33cb1af4444c9b11

RUN apk update && apk add --no-cache openjdk17-jre && rm -rf /var/cache/apk/*

# install missing fontconfig package
RUN apk add --no-cache fontconfig ttf-dejavu
RUN fc-cache -f -v

ENV PORT 8080
EXPOSE 8080
WORKDIR /opt

COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

COPY starter/target/*.jar app.jar

ENTRYPOINT ["./entrypoint.sh"]
CMD ["-jar", "app.jar"]
