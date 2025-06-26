package com.example.filestorage.repository

import com.example.filestorage.repository.model.FileData
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.util.*

interface FileStorageRepository : MongoRepository<FileData, String> {

    fun findByUserId(userId: String, pageable: Pageable): Page<FileData>

    fun findByUserIdAndTagsIn(userId: String, tags: Set<String>, pageable: Pageable): Page<FileData>

    fun findByExternalId(externalFileId: UUID): Optional<FileData>

    @Query("{ 'visibility': 'PUBLIC' }")
    fun findAllPublicFiles(pageable: Pageable): Page<FileData>

    @Query("{ 'tags': { \$in: ?0 }, 'visibility': 'PUBLIC' }")
    fun findAllPublicFilesByTagsIn(tags: Set<String>, pageable: Pageable): Page<FileData>
}