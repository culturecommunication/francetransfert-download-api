FROM openjdk:11.0.4-jre-stretch
VOLUME /tmp
ADD target/FTR-download-api-0.2.0.jar francetransfert-download-api.jar
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/francetransfert-download-api.jar"]
