package com.ismartcoding.lib.readability4j

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.readability4j.processor.ArticleGrabber
import com.ismartcoding.lib.readability4j.processor.MetadataParser
import com.ismartcoding.lib.readability4j.processor.Postprocessor
import com.ismartcoding.lib.readability4j.processor.Preprocessor
import org.jsoup.Jsoup

object Readability4J {
    private val options = ReadabilityOptions()
    private val preprocessor = Preprocessor()
    private val metadataParser = MetadataParser()
    private val articleGrabber = ArticleGrabber(options)
    private val postprocessor = Postprocessor()

    fun parse(uri: String, html: String): Article {
        val document = Jsoup.parse(html, uri)

        // Avoid parsing too large documents, as per configuration option
        if (options.maxElemsToParse > 0) {
            val numTags = document.getElementsByTag("*").size
            if (numTags > options.maxElemsToParse) {
                throw Exception("Aborting parsing document; $numTags elements found, but ReadabilityOption.maxElemsToParse is set to ${options.maxElemsToParse}")
            }
        }

        val article = Article(uri)

        preprocessor.prepareDocument(document)

        val metadata = metadataParser.getArticleMetadata(document)

        val articleContent = articleGrabber.grabArticle(document, metadata)
        LogCat.d("Grabbed: $articleContent")

        articleContent?.let {
            postprocessor.postProcessContent(document, articleContent, uri)
            article.articleContent = articleContent
        }

        // If we haven't found an excerpt in the article's metadata, use the article's
        // first paragraph as the excerpt. This is used for displaying a preview of
        // the article's content.
        if (metadata.excerpt.isNullOrBlank()) {
            articleContent?.getElementsByTag("p")?.first()?.let { firstParagraph ->
                metadata.excerpt = firstParagraph.text().trim()
            }
        }

        article.title = metadata.title
        article.byline = if (metadata.byline.isNullOrBlank()) articleGrabber.articleByline else metadata.byline
        article.dir = articleGrabber.articleDir
        article.excerpt = metadata.excerpt
        article.charset = metadata.charset

        return article
    }
}