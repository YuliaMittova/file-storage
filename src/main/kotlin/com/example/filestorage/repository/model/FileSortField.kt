package com.example.filestorage.repository.model

enum class FileSortField(
    val fieldName: String,
) {
    FILENAME("fileName"),
    UPLOAD_DATE("uploadDate"),
    TAG("tag"),
    CONTENT_TYPE("contentType"),
    FILE_SIZE("file_size"),
}