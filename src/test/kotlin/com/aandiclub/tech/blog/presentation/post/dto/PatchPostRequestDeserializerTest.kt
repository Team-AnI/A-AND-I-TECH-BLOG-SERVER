package com.aandiclub.tech.blog.presentation.post.dto

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PatchPostRequestDeserializerTest : StringSpec({
	"should deserialize collaborators payload" {
		val mapper = ObjectMapper().findAndRegisterModules()
		val request = mapper.readValue(
			"""{"title":"updated","collaborators":["u-collab-1",{"id":"u-collab-2","nickname":"collab2"}]}""",
			PatchPostRequest::class.java,
		)

		request.collaborators?.size shouldBe 2
		request.collaborators?.get(0)?.id shouldBe "u-collab-1"
		request.collaborators?.get(1)?.id shouldBe "u-collab-2"
	}

	"should deserialize summary when provided" {
		val mapper = ObjectMapper().findAndRegisterModules()
		val request = mapper.readValue(
			"""{"summary":"patched summary"}""",
			PatchPostRequest::class.java,
		)

		request.summary shouldBe "patched summary"
	}

	"should deserialize type when provided" {
		val mapper = ObjectMapper().findAndRegisterModules()
		val request = mapper.readValue(
			"""{"type":"Lecture"}""",
			PatchPostRequest::class.java,
		)

		request.type?.name shouldBe "Lecture"
	}
})
