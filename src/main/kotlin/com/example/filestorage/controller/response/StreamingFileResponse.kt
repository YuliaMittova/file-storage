package com.example.filestorage.controller.response

import java.io.InputStream

data class StreamingFileResponse(
    val fileData: FileResponse,
    val inputStream: InputStream,
)
