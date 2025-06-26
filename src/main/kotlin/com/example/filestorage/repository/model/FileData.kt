package com.example.filestorage.repository.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.*

@Document(collection = "files_data")
@CompoundIndex(name = "owner_id_filename_unique_idx", def = "{'userId': 1, 'fileName': 1}", unique = true)
@CompoundIndex(name = "tags_visibility_idx", def = "{'tags': 1, 'visibility': 1}")
data class FileData(
    @Id
    val id: String,

    @Indexed(unique = true)
    val externalId: UUID,

    val fileName: String,

    val userId: String,

    val hash: String,

    val tags: Collection<String> = TreeSet<String>(),

    val size: Long,

    val visibility: Visibility,

    val contentType: String,

    val uploadDate: Instant,
)

enum class Visibility {
    PRIVATE, PUBLIC
}