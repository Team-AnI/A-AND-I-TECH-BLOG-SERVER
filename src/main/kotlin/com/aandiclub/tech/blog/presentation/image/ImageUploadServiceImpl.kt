package com.aandiclub.tech.blog.presentation.image

import com.aandiclub.tech.blog.infrastructure.s3.S3Properties
import com.aandiclub.tech.blog.presentation.image.dto.ImageUploadResponse
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.util.UUID

@Service
class ImageUploadServiceImpl(
	private val imageUploadValidator: ImageUploadValidator,
	private val s3AsyncClient: S3AsyncClient,
	private val s3Properties: S3Properties,
) : ImageUploadService {
	override suspend fun upload(filePart: FilePart): ImageUploadResponse {
		if (s3Properties.bucket.isBlank()) {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "s3 bucket is not configured")
		}

		val contentType = filePart.headers().contentType?.toString()
			?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "missing content type")
		val bytes = filePart.readBytes()
		imageUploadValidator.validate(contentType, bytes.size.toLong())

		val key = "posts/${UUID.randomUUID()}.${contentType.substringAfter('/', "bin")}"
		val request = PutObjectRequest.builder()
			.bucket(s3Properties.bucket)
			.key(key)
			.contentType(contentType)
			.contentLength(bytes.size.toLong())
			.build()
		try {
			s3AsyncClient.putObject(request, AsyncRequestBody.fromBytes(bytes)).await()
		} catch (e: S3Exception) {
			log.error("S3 putObject failed for bucket={}, key={}", s3Properties.bucket, key, e)
			throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "s3 upload failed: ${e.awsErrorDetails()?.errorCode() ?: "s3_error"}", e)
		} catch (e: SdkException) {
			log.error("S3 client failed before putObject for bucket={}, key={}", s3Properties.bucket, key, e)
			throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "s3 client configuration error", e)
		}

		return ImageUploadResponse(
			url = resolvePublicUrl(key),
			key = key,
			contentType = contentType,
			size = bytes.size.toLong(),
		)
	}

	private suspend fun FilePart.readBytes(): ByteArray {
		val joined = DataBufferUtils.join(content()).awaitSingle()
		try {
			val bytes = ByteArray(joined.readableByteCount())
			joined.read(bytes)
			return bytes
		} finally {
			DataBufferUtils.release(joined)
		}
	}

	private fun resolvePublicUrl(key: String): String {
		if (s3Properties.publicBaseUrl.isNotBlank()) {
			return "${s3Properties.publicBaseUrl.trimEnd('/')}/$key"
		}
		return "https://${s3Properties.bucket}.s3.${s3Properties.region}.amazonaws.com/$key"
	}

	private companion object {
		private val log = LoggerFactory.getLogger(ImageUploadServiceImpl::class.java)
	}
}
