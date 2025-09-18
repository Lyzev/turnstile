FROM eclipse-temurin:24-alpine AS jre-build

RUN $JAVA_HOME/bin/jlink \
         --add-modules java.base,java.naming,java.net.http,jdk.crypto.ec,jdk.unsupported,java.xml \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime

FROM alpine:latest
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME
RUN mkdir /opt/app
WORKDIR /opt/app
COPY build/libs/turnstile-all.jar /opt/app/turnstile-all.jar

# Expose the default port (8080)
EXPOSE 8080

# Set environment variables with default values
ENV WEB_SERVER_HOST=0.0.0.0
ENV WEB_SERVER_PORT=8080

CMD ["java", "-jar", "/opt/app/turnstile-all.jar"]