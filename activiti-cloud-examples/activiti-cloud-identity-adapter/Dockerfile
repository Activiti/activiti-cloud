FROM docker.io/library/alpine:3.24@sha256:be2656061b536c1883c4034790e21d0a09c5aaaf51968b89368a3c0371ea2c9a

# add Amazon Corretto's public key
RUN wget -O /etc/apk/keys/amazoncorretto.rsa.pub  https://apk.corretto.aws/amazoncorretto.rsa.pub

# add the Amazon Corretto APK repository
RUN echo "https://apk.corretto.aws/" >> /etc/apk/repositories

# update the list of packages and install java
RUN apk update && apk add --no-cache amazon-corretto-25 && rm -rf /var/cache/apk/*

ENV PORT 8080
EXPOSE 8080

WORKDIR /opt
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh
COPY target/*.jar app.jar

ENTRYPOINT ["./entrypoint.sh"]
CMD ["-jar", "app.jar"]
