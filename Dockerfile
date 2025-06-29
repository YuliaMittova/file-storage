FROM openjdk:17-jdk-slim
WORKDIR /opt/storage
COPY build/libs/filestorage-0.0.1-SNAPSHOT.jar filestorage-0.0.1-SNAPSHOT.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "filestorage-0.0.1-SNAPSHOT.jar"]