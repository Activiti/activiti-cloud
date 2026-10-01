FROM docker.io/library/alpine:3.24@sha256:294b683cb724975bec92580e1e685676bd4b50bda910ddb8c51d4cabeaec77e6

# add Amazon Corretto's public key
RUN wget -O /etc/apk/keys/amazoncorretto.rsa.pub  https://apk.corretto.aws/amazoncorretto.rsa.pub

# add the Amazon Corretto APK repository
RUN echo "https://apk.corretto.aws/" >> /etc/apk/repositories

# update the list of packages and install java
RUN apk update && apk add --no-cache amazon-corretto-25 && rm -rf /var/cache/apk/*

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
