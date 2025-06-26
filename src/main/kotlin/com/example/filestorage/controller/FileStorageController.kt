package com.example.filestorage.controller

import com.example.filestorage.controller.response.FileResponse
import com.example.filestorage.repository.model.FileSortField
import com.example.filestorage.repository.model.Visibility
import com.example.filestorage.service.FileStorageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.apache.coyote.BadRequestException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Async
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream
import java.util.*

@Tag(name = "File Storage Service")
@RestController
@RequestMapping("/api/v1/files")
@Validated
class FileStorageController(
    private val fileStorageService: FileStorageService
) {
    private val logger = LoggerFactory.getLogger(FileStorageController::class.java)

    @Operation(
        summary = "Upload file to the storage", requestBody = RequestBody(
            content = arrayOf(
                Content(
                    mediaType = "application/octet-stream", schema = Schema(type = "string", format = "binary")
                ),
            )
        ), responses = [ApiResponse(
            responseCode = "200", description = "OK",
        ), ApiResponse(
            description = "Error occurred when uploading files",
            content = [Content(mediaType = "application/json", schema = Schema(type = "string"))]
        )]
    )
    @Async
    @PostMapping("/upload")
    fun uploadFile(
        request: HttpServletRequest,
        @RequestParam @NotBlank userId: String,
        @RequestParam @NotBlank fileName: String,
        @RequestParam visibility: Visibility,
        @Size(min = 0, max = 5) @RequestParam tags: Set<String>?,
    ): ResponseEntity<FileResponse> {
        if (request.contentLengthLong == 0L) {
            throw BadRequestException("File must not be empty.")
        }

        val uploadedFile = fileStorageService.uploadFile(
            userId = userId,
            fileName = fileName,
            contentType = request.contentType,
            visibility = visibility,
            tags = tags ?: setOf(),
            inputStream = request.inputStream,
        )
        return ResponseEntity.ok(uploadedFile)
    }

    @Operation(summary = "Get all public files")
    @GetMapping("/public")
    fun fetchPublicFiles(
        @RequestParam(defaultValue = "") tags: Set<String>,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Min(1) @Max(
            value = 50,
            message = "Max {value} page size allowed"
        ) @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "UPLOAD_DATE") sortBy: FileSortField,
        @RequestParam(defaultValue = "true") ascending: Boolean,
    ): ResponseEntity<Page<FileResponse>> {
        val sort: Sort = Sort.by(sortBy.fieldName)
        val pageable: Pageable = PageRequest.of(
            page, size, if (ascending) sort.ascending() else sort.descending()
        )
        val pageOfFiles: Page<FileResponse> = fileStorageService.fetchPublicFiles(tags, pageable)
        return ResponseEntity.ok(pageOfFiles)
    }

    @Operation(summary = "Get user files")
    @GetMapping("/user")
    fun fetchUserFiles(
        @RequestParam @NotBlank userId: String,
        @RequestParam(defaultValue = "") tags: Set<String>,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Min(1) @Max(
            value = 50,
            message = "Max {value} page size allowed"
        ) @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "UPLOAD_DATE") sortBy: FileSortField,
        @RequestParam(defaultValue = "true") ascending: Boolean,
    ): ResponseEntity<Page<FileResponse>> {
        val sort: Sort = Sort.by(sortBy.fieldName)
        val pageable: Pageable = PageRequest.of(
            page, size, if (ascending) sort.ascending() else sort.descending()
        )
        val pageOfFiles: Page<FileResponse> = fileStorageService.fetchUserFiles(userId, tags, pageable)
        return ResponseEntity.ok(pageOfFiles)
    }

    @Operation(summary = "Delete file")
    @DeleteMapping("/{fileId}")
    fun deleteFile(
        @PathVariable fileId: UUID,
        @RequestParam @NotBlank userId: String,
    ): ResponseEntity<Unit> {
        fileStorageService.deleteFile(fileId, userId)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "Rename file")
    @PatchMapping(value = ["/{fileId}"])
    fun renameFile(
        @PathVariable fileId: UUID,
        @RequestParam @NotBlank userId: String,
        @RequestParam @NotBlank fileName: String,
    ): ResponseEntity<FileResponse> {
        val result = fileStorageService.renameFile(userId, fileId, fileName)
        return ResponseEntity.ok(result)
    }

    @Operation
    @Async
    @GetMapping("/{fileId}")
    fun downloadFile(
        @PathVariable fileId: UUID,
        @RequestParam @NotBlank userId: String,
    ): ResponseEntity<StreamingResponseBody> {
        val result = fileStorageService.getFile(fileId, userId)
        val fileData = result.fileData
        val responseBody = StreamingResponseBody { outputStream: OutputStream? ->
            try {
                result.inputStream.use { fileStream ->
                    val buffer = ByteArray(16 * 1024)
                    var byteCount: Int
                    while ((fileStream.read(buffer).also { byteCount = it }) != -1) {
                        outputStream!!.write(buffer, 0, byteCount)
                    }
                }
            } catch (ex: Exception) {
                logger.error("An error occurred during download file $fileId", ex)
                throw ex
            }
        }

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + fileData.fileName
            )
            .header(HttpHeaders.CONTENT_TYPE, fileData.contentType)
            .header(HttpHeaders.CONTENT_LENGTH, fileData.size.toString())
            .body(responseBody)
    }
}