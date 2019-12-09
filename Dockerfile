FROM openjdk:11.0.4-jre-stretch
VOLUME /tmp
ADD target/francetransfert-download-api-0.0.1-SNAPSHOT.jar francetransfert-download-api.jar
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/francetransfert-download-api.jar"]
