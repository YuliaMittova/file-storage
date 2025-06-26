package com.example.filestorage.service

import com.example.filestorage.controller.config.StorageConfig
import com.example.filestorage.controller.response.FileResponse
import com.example.filestorage.controller.response.StreamingFileResponse
import com.example.filestorage.exception.FileDuplicatedException
import com.example.filestorage.exception.FileNotFoundException
import com.example.filestorage.exception.FileStorageException
import com.example.filestorage.exception.UnauthorizedAccessException
import com.example.filestorage.exception.ValidationException
import com.example.filestorage.mapper.FileMapper
import com.example.filestorage.repository.FileStorageRepository
import com.example.filestorage.repository.model.FileData
import com.example.filestorage.repository.model.Visibility
import com.example.filestorage.service.util.ContentTypeDetector
import com.mongodb.DuplicateKeyException
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.*

@Service
class FileStorageServiceImpl(
    private val storageRepository: FileStorageRepository,
    private val contentTypeDetector: ContentTypeDetector,
    private val fileMapper: FileMapper,
    storageConfig: StorageConfig,
) : FileStorageService {

    private val logger = LoggerFactory.getLogger(FileStorageService::class.java)
    private val uploadDir = Paths.get(storageConfig.uploadDir)

    @PostConstruct
    fun init() {
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir)
        }
    }

    override fun uploadFile(
        userId: String,
        fileName: String,
        contentType: String,
        visibility: Visibility,
        tags: Set<String>,
        inputStream: InputStream,
    ): FileResponse {
        if (fileName.isEmpty()) throw ValidationException("File name cannot be empty")
        if (contentType.isEmpty()) throw ValidationException("Content type cannot be empty")

        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val fileId = UUID.randomUUID()
        var totalBytes: Long = 0
        val targetFile = File("${uploadDir}/${fileId}")
        val detectedContentType = contentTypeDetector.detect(fileName, inputStream)

        targetFile.outputStream().use { fos ->
            DigestInputStream(inputStream, digest).use { dis ->
                val buffer = ByteArray(PART_SIZE)
                var bytesRead: Int
                while (dis.read(buffer).also { bytesRead = it } != -1) {
                    fos.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                }
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }

        try {
            val correctedTags = unifyTags(tags)
            val entity = storageRepository.save(
                FileData(
                    id = fileId.toString(),
                    fileName = fileName,
                    uploadDate = Instant.now(),
                    externalId = fileId,
                    visibility = visibility,
                    userId = userId,
                    tags = correctedTags,
                    hash = hash,
                    size = totalBytes,
                    contentType = detectedContentType,
                )
            )
            return fileMapper.toResponse(entity)
        } catch (t: Throwable) {
            logger.error("Error occurred while saving file", t)
            deleteFile(fileName)
            if (t is DuplicateKeyException) {
                throw FileDuplicatedException("File $fileId already exists")
            }
            throw t
        }
    }

    override fun fetchPublicFiles(
        tags: Set<String>,
        pageable: Pageable
    ): Page<FileResponse> {
        val result = if (tags.isEmpty()) {
            storageRepository.findAllPublicFiles(pageable)
        } else {
            storageRepository.findAllPublicFilesByTagsIn(unifyTags(tags), pageable)
        }
        return result.map {
            fileMapper.toResponse(it)
        }
    }

    override fun fetchUserFiles(
        userId: String,
        tags: Set<String>,
        pageable: Pageable
    ): Page<FileResponse> {
        val result = if (tags.isEmpty()) {
            storageRepository.findByUserId(userId, pageable)
        } else {
            storageRepository.findByUserIdAndTagsIn(userId, unifyTags(tags), pageable)
        }
        return result.map { fileMapper.toResponse(it) }
    }

    override fun deleteFile(fileId: UUID, userId: String) {
        val fileData = storageRepository.findByExternalId(fileId)
        if (!fileData.isPresent || fileData.isEmpty) {
            throw FileNotFoundException()
        }
        if (fileData.get().userId != userId) {
            throw UnauthorizedAccessException()
        }
        deleteFile(fileId.toString())
        storageRepository.deleteById(fileData.get().id)
    }

    override fun renameFile(
        userId: String,
        fileId: UUID,
        newFilename: String
    ): FileResponse {
        val fileData = storageRepository.findByExternalId(fileId)
        if (!fileData.isPresent || fileData.isEmpty) {
            throw FileNotFoundException()
        }
        if (fileData.get().userId != userId) {
            throw UnauthorizedAccessException()
        }
        if (fileData.get().fileName == newFilename) {
            throw FileDuplicatedException("Cannot rename file. File $fileId already exists.")
        }
        val result = storageRepository.save(fileData.get().copy(fileName = newFilename))
        return fileMapper.toResponse(result)
    }

    override fun getFile(
        fileId: UUID,
        userId: String
    ): StreamingFileResponse {
        val fileData = storageRepository.findByExternalId(fileId)
        if (!fileData.isPresent || fileData.isEmpty) {
            throw FileNotFoundException()
        }
        if (fileData.get().visibility == Visibility.PRIVATE && fileData.get().userId != userId) {
            throw UnauthorizedAccessException()
        }

        try {
            val inputStream = getFileAsStream(fileId.toString())
            return StreamingFileResponse(
                fileData = fileMapper.toResponse(fileData.get()),
                inputStream = inputStream,
            )
        } catch (t: Throwable) {
            if (t is FileStorageException) {
                throw t
            }
            logger.error("Error occurred while reading file", t)
            throw FileStorageException("Error occurred while reading file with id $fileId")
        }
    }

    private fun deleteFile(fileName: String): Boolean {
        val filePath: Path = uploadDir.resolve(fileName)
        return try {
            Files.deleteIfExists(filePath)
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun getFileAsStream(fileId: String): InputStream {
        val filePath = uploadDir.resolve(fileId).normalize()
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw FileNotFoundException()
        }
        return FileInputStream(filePath.toFile())
    }

    private fun unifyTags(tags: Set<String>): Set<String> {
        return tags.filter { it.isNotEmpty() }.map { it.toLowerCase() }.toSet()
    }

    private companion object {
        const val PART_SIZE = 5 * 1024 * 1024
        const val HASH_ALGORITHM = "SHA-256"
    }
}