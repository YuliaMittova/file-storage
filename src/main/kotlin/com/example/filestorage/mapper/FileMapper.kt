package com.example.filestorage.mapper

import com.example.filestorage.controller.response.FileResponse
import com.example.filestorage.repository.model.FileData
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class FileMapper {

    fun toResponse(entity: FileData) = FileResponse(
        id = entity.externalId,
        fileName = entity.fileName,
        tags = entity.tags.toSet(),
        size = entity.size,
        visibility = entity.visibility,
        contentType = entity.contentType,
        uploadDate = Instant.now(),
    )
}