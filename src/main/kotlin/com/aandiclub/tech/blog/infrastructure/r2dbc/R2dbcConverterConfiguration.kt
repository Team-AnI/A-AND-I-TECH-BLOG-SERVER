package com.aandiclub.tech.blog.infrastructure.r2dbc

import com.aandiclub.tech.blog.domain.post.PostStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.CustomConversions
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.DialectResolver
import org.springframework.r2dbc.core.DatabaseClient

@Configuration
class R2dbcConverterConfiguration {
	@Bean
	fun r2dbcCustomConversions(databaseClient: DatabaseClient): R2dbcCustomConversions {
		val dialect = DialectResolver.getDialect(databaseClient.connectionFactory)
		val storeConverters = ArrayList(dialect.converters)
		storeConverters.addAll(R2dbcCustomConversions.STORE_CONVERTERS)

		return R2dbcCustomConversions(
			CustomConversions.StoreConversions.of(dialect.simpleTypeHolder, storeConverters),
			listOf(PostStatusReadingConverter, PostStatusWritingConverter),
		)
	}
}

@ReadingConverter
object PostStatusReadingConverter : Converter<String, PostStatus> {
	override fun convert(source: String): PostStatus = PostStatus.valueOf(source)
}

@WritingConverter
object PostStatusWritingConverter : Converter<PostStatus, String> {
	override fun convert(source: PostStatus): String = source.name
}
