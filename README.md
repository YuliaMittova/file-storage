# File Storage Project

## Overview
This is a basic implementation of file storage service. It was written in Kotlin using Spring Boot, MongoDB, Gradle.
Local file storage was used as a storage. But if I had more time I would integrate MinIO for it because it is more
effective and scalable.

## Available features:
* Upload file with tags
* Fetch all public files. Results are paginated and sorted.
* Fetch files that belong only to a specified user. Results are paginated and sorted.
* Automatic detection of content type using Apache Tika library.
* Download file
* Delete file
* Rename file.

## How to start
There are 2 ways how you can start the application:

* Run it locally in your IntellijIdea:
![Local Intellij configuration](/documentation/application_config.png)
* Start service in Docker by running the command:
```ssh
docker-compose up --build
```
You can access API via Swagger link: http://localhost:8080/swagger-ui/index.html
