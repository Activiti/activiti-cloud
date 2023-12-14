FROM docker.io/library/alpine:3.18@sha256:02bb6f428431fbc2809c5d1b41eab5a68350194fb508869a33cb1af4444c9b11

# add Azul's public key
RUN wget -P /etc/apk/keys/ https://cdn.azul.com/public_keys/alpine-signing@azul.com-5d5dc44c.rsa.pub

# add the Azul APK repository
RUN echo "https://repos.azul.com/zulu/alpine" | tee -a /etc/apk/repositories

# update the list of packages and install java
RUN apk update && apk add --no-cache zulu21-jre && rm -rf /var/cache/apk/*

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
