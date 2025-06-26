package com.example.filestorage.service

import com.example.filestorage.controller.response.FileResponse
import com.example.filestorage.controller.response.StreamingFileResponse
import com.example.filestorage.exception.FileStorageException
import com.example.filestorage.repository.model.Visibility
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.io.InputStream
import java.util.*

interface FileStorageService {

    @Throws(FileStorageException::class)
    fun uploadFile(
        userId: String,
        fileName: String,
        contentType: String,
        visibility: Visibility,
        tags: Set<String>,
        inputStream: InputStream,
    ): FileResponse

    @Throws(FileStorageException::class)
    fun fetchPublicFiles(tags: Set<String>, pageable: Pageable): Page<FileResponse>

    @Throws(FileStorageException::class)
    fun fetchUserFiles(userId: String, tags: Set<String>, pageable: Pageable): Page<FileResponse>

    @Throws(FileStorageException::class)
    fun deleteFile(fileId: UUID, userId: String)

    @Throws(FileStorageException::class)
    fun renameFile(userId: String, fileId: UUID, newFilename: String): FileResponse

    @Throws(FileStorageException::class)
    fun getFile(fileId: UUID, userId: String): StreamingFileResponse
}