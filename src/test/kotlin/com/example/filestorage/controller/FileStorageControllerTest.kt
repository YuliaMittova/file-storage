package com.example.filestorage.controller

import com.example.filestorage.mapper.FileMapper
import com.example.filestorage.repository.model.FileData
import com.example.filestorage.repository.model.FileSortField
import com.example.filestorage.repository.model.Visibility
import com.example.filestorage.service.FileStorageService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.random.Random

@WebMvcTest(controllers = [FileStorageController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(FileMapper::class)
class FileStorageControllerTest {


    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var fileStorageService: FileStorageService

    @Autowired
    private lateinit var fileMapper: FileMapper

    @Test
    fun whenPublicFilesExistThenShouldFetchPublicFiles() {
        val testUser = "TEST_USER"

        val testFile = generateTestFileData(
            userId = testUser,
            visibility = Visibility.PUBLIC,
            tags = setOf(),
        )

        given(
            fileStorageService.fetchPublicFiles(
                tags = setOf(),
                pageable = PageRequest.of(
                    0, 20, Sort.by(FileSortField.UPLOAD_DATE.fieldName).ascending()
                )
            )
        ).willReturn(PageImpl(listOf(fileMapper.toResponse(testFile))))

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files/public").queryParam("userId", testUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun whenPrivateFilesExistThenShouldFetchPrivateFiles() {
        val testUser = "ANOTHER_USER"

        val testFile = generateTestFileData(
            userId = testUser,
            visibility = Visibility.PRIVATE,
            tags = setOf(),
        )

        given(
            fileStorageService.fetchUserFiles(
                userId = testUser,
                tags = setOf(),
                pageable = PageRequest.of(
                    0, 20, Sort.by(FileSortField.UPLOAD_DATE.fieldName).ascending()
                )
            )
        ).willReturn(PageImpl(listOf(fileMapper.toResponse(testFile))))

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files/user").queryParam("userId", testUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun whenValidFilePassedThenShouldRenameFile() {
        val testUser = "ANOTHER_USER"
        val newFileName = "new_file_name"
        val testFile = generateTestFileData(
            userId = testUser,
            visibility = Visibility.PRIVATE,
            tags = setOf(),
        )
        val responseFile = fileMapper.toResponse(testFile.copy(fileName = newFileName))

        given(
            fileStorageService.renameFile(
                userId = testUser,
                fileId = responseFile.id,
                newFilename = newFileName
            )
        ).willReturn(responseFile)

        mockMvc.perform(
            patch("/api/v1/files/{id}", responseFile.id)
                .queryParam("userId", testUser).queryParam("fileName", newFileName)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.fileName").value(newFileName))
    }

    @Test
    fun whenValidFilePassedThenShouldDeleteFile() {
        val testUser = "JUST_PERSON"
        val testFile = generateTestFileData(
            userId = testUser,
            visibility = Visibility.PRIVATE,
            tags = setOf(),
        )
        mockMvc.perform(
            delete("/api/v1/files/{id}", testFile.id)
                .queryParam("userId", testUser)
        ).andExpect(status().isOk)
    }

    private fun generateTestFileData(
        userId: String,
        visibility: Visibility,
        tags: Set<String>
    ): FileData {
        val random = Random(100)

        return FileData(
            id = UUID.randomUUID().toString(),
            externalId = UUID.randomUUID(),
            fileName = "testFile_${random.nextInt()}",
            userId = userId,
            hash = "randomHash${random.nextInt()}",
            tags = tags,
            size = random.nextLong(Long.Companion.MAX_VALUE),
            visibility = visibility,
            contentType = "application/octet-stream",
            uploadDate = Instant.now().truncatedTo(ChronoUnit.MILLIS),
        )
    }
}