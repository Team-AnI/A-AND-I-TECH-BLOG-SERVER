package com.aandiclub.tech.blog.infrastructure.post

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ScheduledPostPublisher(
	private val postRepository: PostRepository,
) {
	@Scheduled(fixedDelayString = "\${app.posts.scheduled-publish-delay-ms:30000}")
	fun publishDuePosts() = runBlocking {
		val publishedCount = postRepository.publishScheduledPosts(Instant.now())
		if (publishedCount > 0) {
			log.info("Published {} scheduled posts", publishedCount)
		}
	}

	private companion object {
		private val log = LoggerFactory.getLogger(ScheduledPostPublisher::class.java)
	}
}
