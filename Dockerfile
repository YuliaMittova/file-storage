FROM openjdk:21-jdk-slim
WORKDIR /opt/storage
COPY --from=build /project/target/filestorage-0.0.1-SNAPSHOT.jar filestorage-0.0.1-SNAPSHOT.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/opt/storage/filestorage-0.0.1-SNAPSHOT.jar"]