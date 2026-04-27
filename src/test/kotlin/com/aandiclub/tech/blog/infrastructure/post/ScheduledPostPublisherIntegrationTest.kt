package com.aandiclub.tech.blog.infrastructure.post

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.EnabledIf
import java.time.Instant
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@EnabledIf(
	expression = "#{T(org.testcontainers.DockerClientFactory).instance().isDockerAvailable()}",
	loadContext = false,
	reason = "Requires Docker daemon for Testcontainers-backed R2DBC",
)
class ScheduledPostPublisherIntegrationTest {

	@Autowired
	lateinit var databaseClient: DatabaseClient

	@Autowired
	lateinit var postRepository: PostRepository

	@Autowired
	lateinit var scheduledPostPublisher: ScheduledPostPublisher

	@BeforeEach
	fun setUp() = runBlocking {
		recreatePostsTable()
	}

	@Test
	fun `publishDuePosts should publish only due scheduled posts and keep future ones scheduled`() = runBlocking {
		val duePostId = UUID.randomUUID()
		val futurePostId = UUID.randomUUID()
		val now = Instant.now()
		val dueScheduleAt = now.minusSeconds(60)
		val futureScheduleAt = now.plusSeconds(3600)

		insertPost(
			id = duePostId,
			type = PostType.Lecture,
			status = PostStatus.Scheduled,
			scheduledPublishAt = dueScheduleAt,
		)
		insertPost(
			id = futurePostId,
			type = PostType.Lecture,
			status = PostStatus.Scheduled,
			scheduledPublishAt = futureScheduleAt,
		)

		scheduledPostPublisher.publishDuePosts()

		val publishedPost = postRepository.findById(duePostId).shouldNotBeNull()
		publishedPost.status shouldBe PostStatus.Published
		publishedPost.scheduledPublishAt.shouldBeNull()
		publishedPost.publishedAt.shouldNotBeNull()

		val futurePost = postRepository.findById(futurePostId).shouldNotBeNull()
		futurePost.status shouldBe PostStatus.Scheduled
		futurePost.scheduledPublishAt shouldBe futureScheduleAt
		futurePost.publishedAt.shouldBeNull()
	}

	private suspend fun recreatePostsTable() {
		databaseClient.sql("DROP TABLE IF EXISTS posts").fetch().rowsUpdated().awaitSingle()
		databaseClient.sql(
			"""
			CREATE TABLE posts (
			  id UUID PRIMARY KEY,
			  title VARCHAR(200) NOT NULL,
			  summary VARCHAR(300) NOT NULL DEFAULT '',
			  content_markdown TEXT NOT NULL,
			  thumbnail_url VARCHAR(500) NULL,
			  author_id VARCHAR(100) NOT NULL,
			  type VARCHAR(20) NOT NULL,
			  status VARCHAR(20) NOT NULL,
			  scheduled_publish_at TIMESTAMPTZ NULL,
			  published_at TIMESTAMPTZ NULL,
			  created_at TIMESTAMPTZ NOT NULL,
			  updated_at TIMESTAMPTZ NOT NULL
			)
			""".trimIndent(),
		).fetch().rowsUpdated().awaitSingle()
	}

	private suspend fun insertPost(
		id: UUID,
		type: PostType,
		status: PostStatus,
		scheduledPublishAt: Instant?,
	) {
		val createdAt = Instant.parse("2026-04-27T14:00:00Z")
		val updatedAt = createdAt
		var spec = databaseClient.sql(
			"""
			INSERT INTO posts (
			  id,
			  title,
			  summary,
			  content_markdown,
			  thumbnail_url,
			  author_id,
			  type,
			  status,
			  scheduled_publish_at,
			  published_at,
			  created_at,
			  updated_at
			) VALUES (
			  :id,
			  :title,
			  :summary,
			  :contentMarkdown,
			  :thumbnailUrl,
			  :authorId,
			  :type,
			  :status,
			  :scheduledPublishAt,
			  :publishedAt,
			  :createdAt,
			  :updatedAt
			)
			""".trimIndent(),
		)
			.bind("id", id)
			.bind("title", "scheduled lecture")
			.bind("summary", "summary")
			.bind("contentMarkdown", "content")
			.bindNull("thumbnailUrl", String::class.java)
			.bind("authorId", "u-scheduler")
			.bind("type", type.name)
			.bind("status", status.name)
			.bind("createdAt", createdAt)
			.bind("updatedAt", updatedAt)

		spec = if (scheduledPublishAt != null) {
			spec.bind("scheduledPublishAt", scheduledPublishAt)
		} else {
			spec.bindNull("scheduledPublishAt", Instant::class.java)
		}
		spec.bindNull("publishedAt", Instant::class.java)
			.fetch()
			.rowsUpdated()
			.awaitSingle()
	}
}
