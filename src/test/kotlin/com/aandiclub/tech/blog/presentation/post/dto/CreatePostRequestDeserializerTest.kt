package com.aandiclub.tech.blog.presentation.post.dto

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CreatePostRequestDeserializerTest : StringSpec({
	"should deserialize legacy authorId string payload" {
		val mapper = ObjectMapper().findAndRegisterModules()
		val request = mapper.readValue(
			"""{"title":"title","contentMarkdown":"content","authorId":"u-legacy-1","status":"Draft"}""",
			CreatePostRequest::class.java,
		)

		request.author.shouldBeInstanceOf<PostAuthorRequest>()
		request.author.id shouldBe "u-legacy-1"
		request.status.name shouldBe "Draft"
	}
})
