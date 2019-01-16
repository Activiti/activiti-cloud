FROM openjdk:11.0-jdk-slim
#RUN apk --update add fontconfig ttf-dejavu
ENV PORT 8080
EXPOSE 8080
COPY target/*.jar /opt/app.jar
WORKDIR /opt
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar

