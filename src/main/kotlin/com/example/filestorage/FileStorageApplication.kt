package com.example.filestorage

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.bind.annotation.RestController

@OpenAPIDefinition
@SpringBootApplication
@RestController
class FileStorageApplication

fun main(args: Array<String>) {
    SpringApplication.run(FileStorageApplication::class.java, *args)
}
