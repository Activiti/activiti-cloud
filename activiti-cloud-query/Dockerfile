FROM adoptopenjdk/openjdk11:jdk-11.0.2.7-slim

# install missing fontconfig package
RUN apt-get update && apt-get install -y --no-install-recommends fontconfig
RUN ln -s /usr/lib/libfontconfig.so.1 /usr/lib/libfontconfig.so && \
    ln -s /lib/libuuid.so.1 /usr/lib/libuuid.so.1 && \
    ln -s /lib/libc.musl-x86_64.so.1 /usr/lib/libc.musl-x86_64.so.1
ENV LD_LIBRARY_PATH /usr/lib

ENV PORT 8080	
EXPOSE 8080	
COPY target/*.jar /opt/app.jar	
WORKDIR /opt	
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
