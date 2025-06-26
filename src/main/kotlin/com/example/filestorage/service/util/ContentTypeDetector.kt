package com.example.filestorage.service.util

import org.apache.tika.Tika
import org.springframework.stereotype.Component
import java.io.BufferedInputStream
import java.io.InputStream

@Component
class ContentTypeDetector {

    private val tika by lazy { Tika() }

    fun detect(name: String, inputStream: InputStream): String {
        val buffered = BufferedInputStream(inputStream)
        buffered.mark(20_000) // allow Tika to read first bytes without damaging the input stream
        val type = tika.detect(buffered)
        buffered.reset() // reset back to beginning
        return type ?: DEFAULT_CONTENT_TYPE
    }

    companion object {
        const val DEFAULT_CONTENT_TYPE = "application/octet-stream"
    }
}