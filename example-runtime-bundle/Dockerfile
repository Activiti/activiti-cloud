
# FROM adoptopenjdk/openjdk11:jdk-11.0.2.7-alpine-slim
# # Install the font required for runtime bundle BPMN diagram renderer. This will be removed later
# RUN apk --update add fontconfig ttf-dejavu msttcorefonts-installer fontconfig && update-ms-fonts && fc-cache -f
# ENV PORT 8080
# EXPOSE 8080
# COPY target/*.jar /opt/app.jar
# WORKDIR /opt
# ENTRYPOINT ["sh","-c", "java $JAVA_OPTS -jar app.jar"]

FROM adoptopenjdk/openjdk11:jdk-11.0.2.9-slim
RUN  apt update
RUN apt -y install ttf-mscorefonts-installer fontconfig
RUN fc-cache -f -v
ENV PORT 8080
EXPOSE 8080
COPY target/*.jar /opt/app.jar
WORKDIR /opt
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
