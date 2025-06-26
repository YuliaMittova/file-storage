package com.example.filestorage.controller.response

import com.example.filestorage.repository.model.Visibility
import java.time.Instant
import java.util.UUID

data class FileResponse(
    val id: UUID,
    val fileName: String,
    val contentType: String,
    val visibility: Visibility,
    val tags: Set<String>,
    val size: Long,
    val uploadDate: Instant,
)
