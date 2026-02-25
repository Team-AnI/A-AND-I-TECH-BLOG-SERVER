package com.aandiclub.tech.blog.infrastructure.user.event

import com.aandiclub.tech.blog.domain.user.User
import com.aandiclub.tech.blog.infrastructure.user.UserRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.time.Instant

@Component
@ConditionalOnProperty(prefix = "app.user-events", name = ["enabled"], havingValue = "true")
class UserEventsSubscriber(
	private val sqsAsyncClient: SqsAsyncClient,
	private val userRepository: UserRepository,
	private val userEventsProperties: UserEventsProperties,
) {
	private val objectMapper: JsonMapper = JsonMapper.builder()
		.findAndAddModules()
		.build()

	@Scheduled(fixedDelayString = "\${app.user-events.poll-delay-ms:1000}")
	fun poll() = runBlocking {
		if (!userEventsProperties.enabled || userEventsProperties.queueUrl.isBlank()) {
			return@runBlocking
		}

		val response = sqsAsyncClient.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl(userEventsProperties.queueUrl)
				.maxNumberOfMessages(userEventsProperties.maxMessages.coerceIn(1, 10))
				.waitTimeSeconds(userEventsProperties.waitTimeSeconds.coerceIn(1, 20))
				.messageAttributeNames("All")
				.build(),
		).await()

		response.messages().forEach { message ->
			processMessage(message)
		}
	}

	private suspend fun processMessage(message: Message) {
		val event = runCatching { parseMessage(message.body()) }
			.getOrElse { throwable ->
				log.error("Failed to parse user event messageId={}", message.messageId(), throwable)
				return
			}

		if (event == null) {
			log.warn("Skipping unsupported user event messageId={}", message.messageId())
			deleteMessage(message.receiptHandle())
			return
		}

		runCatching {
			when {
				event.deleted -> userRepository.deleteById(event.userId)
				else -> upsertUser(event)
			}
			deleteMessage(message.receiptHandle())
		}.onFailure { throwable ->
			log.error("Failed to apply user event messageId={}", message.messageId(), throwable)
		}
	}

	private suspend fun upsertUser(event: UserEventPayload) {
		val existing = userRepository.findById(event.userId)
		val nickname = event.nickname?.takeIf { it.isNotBlank() } ?: existing?.nickname
		if (nickname.isNullOrBlank()) {
			log.warn("Skipping user upsert due to missing nickname userId={}", event.userId)
			return
		}

		val thumbnailUrl = event.thumbnailUrl ?: existing?.thumbnailUrl
		if (existing == null) {
			userRepository.save(
				User(
					id = event.userId,
					nickname = nickname,
					thumbnailUrl = thumbnailUrl,
				),
			)
			return
		}

		if (existing.nickname == nickname && existing.thumbnailUrl == thumbnailUrl) {
			return
		}

		userRepository.save(
			existing.copy(
				nickname = nickname,
				thumbnailUrl = thumbnailUrl,
				updatedAt = Instant.now(),
			),
		)
	}

	private suspend fun deleteMessage(receiptHandle: String?) {
		if (receiptHandle.isNullOrBlank()) return
		sqsAsyncClient.deleteMessage(
			DeleteMessageRequest.builder()
				.queueUrl(userEventsProperties.queueUrl)
				.receiptHandle(receiptHandle)
				.build(),
		).await()
	}

	private fun parseMessage(rawBody: String): UserEventPayload? {
		val parsedBody = objectMapper.readTree(rawBody)
		val eventNode = unwrapSnsNotification(parsedBody)
		val payloadNode = eventNode.path("data").takeIf { !it.isMissingNode && !it.isNull } ?: eventNode

		val userId = readText(payloadNode, "userId")
			?: readText(payloadNode, "id")
			?: return null

		val eventType = readText(eventNode, "eventType")
			?: readText(eventNode, "type")
			?: readText(payloadNode, "eventType")
		val deleted = eventType?.contains("DELETE", ignoreCase = true) == true

		return UserEventPayload(
			userId = userId,
			nickname = readText(payloadNode, "nickname"),
			thumbnailUrl = readText(payloadNode, "thumbnailUrl")
				?: readText(payloadNode, "profileImageUrl"),
			deleted = deleted,
		)
	}

	private fun unwrapSnsNotification(node: JsonNode): JsonNode {
		val messageText = node.path("Message").takeIf { !it.isMissingNode && it.isTextual }?.asText()
			?: return node
		return runCatching { objectMapper.readTree(messageText) }.getOrDefault(node)
	}

	private fun readText(node: JsonNode, fieldName: String): String? {
		val value = node.path(fieldName)
		if (value.isMissingNode || value.isNull) return null
		val text = value.asText().trim()
		return text.ifBlank { null }
	}

	private data class UserEventPayload(
		val userId: String,
		val nickname: String?,
		val thumbnailUrl: String?,
		val deleted: Boolean,
	)

	private companion object {
		private val log = LoggerFactory.getLogger(UserEventsSubscriber::class.java)
	}
}
