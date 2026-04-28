package com.aandiclub.tech.blog.infrastructure.post

import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class ScheduledPostPublisherTest : StringSpec({
	"publishDuePosts should trigger repository bulk publish" {
		val postRepository = mockk<PostRepository>()
		val publisher = ScheduledPostPublisher(postRepository)

		coEvery { postRepository.publishScheduledPosts(any()) } returns 2

		publisher.publishDuePosts()

		coVerify(exactly = 1) { postRepository.publishScheduledPosts(any()) }
	}
})
