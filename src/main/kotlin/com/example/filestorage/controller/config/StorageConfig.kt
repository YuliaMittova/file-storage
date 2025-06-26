package com.example.filestorage.controller.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.stereotype.Component

@PropertySource("classpath:application-local.properties")
@Component
class StorageConfig(
    @Value("\${app.storage.upload-dir}")
    val uploadDir: String
)