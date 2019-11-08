#
# Build exec jar
#
FROM openjdk:11.0.4-jdk-stretch as build

# update and install misc packages
RUN apt-get update

# set working directory
WORKDIR /app

# From sources build the artifact
COPY . .
RUN sh ./mvnw -U clean package && mv target/francetransfert-download-api-*.jar target/francetransfert-download-api.jar

#
# Build delivered image
#
FROM openjdk:11.0.4-jre-stretch

# update
RUN apt-get update

# set working directory
WORKDIR /app

# Production env var
ENV NEO4J_URL ""
ENV NEO4J_USER ""
ENV NEO4J_PASS ""

# Copy artifact into image
COPY --from=build /app/target/francetransfert-download-api.jar ./

EXPOSE 8080
CMD ["java","-jar","francetransfert-download-api.jar"] 
