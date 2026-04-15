package com.aandiclub.tech.blog.common.logging

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.util.LinkedMultiValueMap

class MultipartRequestLogMapperTest : StringSpec({
	"should convert multipart parts into metadata-only request body" {
		val jsonPart = mockk<FormFieldPart>()
		every { jsonPart.name() } returns "post"
		every { jsonPart.headers() } returns HttpHeaders().apply {
			contentType = MediaType.APPLICATION_JSON
			contentLength = 128
		}

		val filePart = mockk<FilePart>()
		every { filePart.name() } returns "thumbnail"
		every { filePart.filename() } returns "cover.png"
		every { filePart.headers() } returns HttpHeaders().apply {
			contentType = MediaType.IMAGE_PNG
			contentLength = 4096
		}

		val parts = LinkedMultiValueMap<String, org.springframework.http.codec.multipart.Part>().apply {
			add("post", jsonPart)
			add("thumbnail", filePart)
		}

		val requestBody = MultipartRequestLogMapper.toRequestBody(parts)
		requestBody["type"] shouldBe "multipart/form-data"
		requestBody["partCount"] shouldBe 2

		val metadata = (requestBody["parts"] as List<*>).map { it as Map<*, *> }
		metadata[0]["name"] shouldBe "post"
		metadata[0]["kind"] shouldBe "field"
		metadata[0]["filename"] shouldBe null
		metadata[0]["contentType"] shouldBe "application/json"
		metadata[0]["contentLength"] shouldBe 128L

		metadata[1]["name"] shouldBe "thumbnail"
		metadata[1]["kind"] shouldBe "file"
		metadata[1]["filename"] shouldBe "cover.png"
		metadata[1]["contentType"] shouldBe "image/png"
		metadata[1]["contentLength"] shouldBe 4096L
	}
})
