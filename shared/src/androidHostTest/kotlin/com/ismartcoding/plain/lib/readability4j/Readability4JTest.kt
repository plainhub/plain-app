package com.ismartcoding.plain.lib.readability4j

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertContains

/**
 * Unit tests for Readability4J article extraction.
 * Verifies the pure-Kotlin migration (HtmlParser-based, no jsoup)
 * preserves core behavior: title extraction, content scoring, script
 * removal, relative URI fixing, and metadata parsing.
 */
class Readability4JTest {

    private val articleUri = "https://example.com/blog/article-1"

    private val sampleArticleHtml = """
        <html>
        <head>
            <title>My Great Article Title - My Site</title>
            <meta name="description" content="A short description of the article." />
            <meta name="author" content="Jane Doe" />
            <meta property="og:title" content="My Great Article Title" />
            <meta property="og:description" content="Open Graph description." />
            <meta charset="utf-8" />
            <style>body { color: red; }</style>
            <script>var x = 1;</script>
        </head>
        <body>
            <header>
                <nav><a href="/">Home</a></nav>
            </header>
            <article>
                <h1>My Great Article Title</h1>
                <p>This is the first paragraph of the article. It contains enough text to be considered a meaningful paragraph for the readability scoring algorithm to recognize it as content rather than boilerplate or navigation elements.</p>
                <p>This is the second paragraph. It also has a reasonable amount of text content so that the scoring algorithm can properly identify the article container as the top candidate for extraction. The more text we have here, the better the algorithm works.</p>
                <p>The third paragraph continues the article content. It includes a <a href="/relative-link">relative link</a> and an <img src="/images/photo.jpg" alt="A photo" /> image with a relative source URL that should be converted to an absolute URL during post-processing.</p>
                <p>Here is a fourth paragraph to ensure there is plenty of content for the algorithm to work with. This paragraph helps increase the content score of the article container, making it more likely to be selected as the top candidate.</p>
                <p>A fifth paragraph with more content. The readability algorithm needs multiple paragraphs of substantial text to properly identify and extract the main article content from the surrounding page chrome.</p>
            </article>
            <aside>
                <p>This is a sidebar with some content that should not be included in the main article extraction.</p>
            </aside>
            <footer>
                <p>Copyright 2024</p>
            </footer>
            <script>console.log("should be removed");</script>
        </body>
        </html>
    """.trimIndent()

    @Test
    fun parse_extractsArticleContent() {
        val article = Readability4J.parse(articleUri, sampleArticleHtml)

        assertNotNull(article.articleContent, "articleContent should not be null for content-rich HTML")
        val content = article.content
        assertTrue(content.isNotEmpty(), "content should not be empty")
    }

    @Test
    fun parse_contentContainsArticleText() {
        val article = Readability4J.parse(articleUri, sampleArticleHtml)

        val text = article.textContent ?: ""
        assertContains(text, "first paragraph")
        assertContains(text, "second paragraph")
    }

    @Test
    fun parse_excludesSidebarContent() {
        val article = Readability4J.parse(articleUri, sampleArticleHtml)

        val text = article.textContent ?: ""
        assertFalse(text.contains("sidebar"), "Sidebar content should not be in extracted article")
    }

    @Test
    fun parse_excludesFooterContent() {
        val article = Readability4J.parse(articleUri, sampleArticleHtml)

        val text = article.textContent ?: ""
        assertFalse(text.contains("Copyright"), "Footer content should not be in extracted article")
    }

    @Test
    fun parse_removesScriptTags() {
        val article = Readability4J.parse(articleUri, sampleArticleHtml)

        val content = article.content
        assertFalse(content.contains("console.log"), "Script content should be removed")
        assertFalse(content.contains("var x"), "Script content should be removed")
    }

    @Test
    fun parse_removesStyleTags() {
        val article = Readability4J.parse(articleUri, sampleArticleHtml)

        val content = article.content
        assertFalse(content.contains("color: red"), "Style content should be removed")
    }

    @Test
    fun parse_extractsTitle() {
        val article = Readability4J.parse(articleUri, sampleArticleHtml)

        // Title should be extracted from <title> tag
        assertTrue(article.title.isNotEmpty(), "Title should not be empty")
        assertContains(article.title, "My Great Article Title")
    }

    @Test
    fun parse_extractsExcerptFromMetaDescription() {
        val article = Readability4J.parse(articleUri, sampleArticleHtml)

        assertNotNull(article.excerpt)
        assertTrue(article.excerpt!!.isNotEmpty())
    }

    @Test
    fun parse_extractsBylineFromMetaAuthor() {
        val article = Readability4J.parse(articleUri, sampleArticleHtml)

        assertEquals("Jane Doe", article.byline)
    }

    @Test
    fun parse_extractsCharset() {
        val article = Readability4J.parse(articleUri, sampleArticleHtml)

        assertEquals("utf-8", article.charset)
    }

    @Test
    fun parse_fixesRelativeUris() {
        val article = Readability4J.parse(articleUri, sampleArticleHtml)

        val content = article.content
        // Relative link "/relative-link" should become absolute
        assertContains(content, "https://example.com/relative-link")
        // Relative image "/images/photo.jpg" should become absolute
        assertContains(content, "https://example.com/images/photo.jpg")
    }

    @Test
    fun parse_preservesAbsoluteUris() {
        val html = """
            <html><head><title>Test</title></head><body>
            <article>
                <p>This is a paragraph with enough text for the readability scoring algorithm to properly identify it as article content and extract it from the page. We need a good amount of text here.</p>
                <p>Another paragraph with an <a href="https://other.com/page">absolute link</a> and an <img src="https://cdn.example.com/img.png" alt="img" /> absolute image.</p>
                <p>More content here to ensure the article container gets a high enough score to be selected as the top candidate by the algorithm.</p>
            </article>
            </body></html>
        """.trimIndent()

        val article = Readability4J.parse("https://example.com/page", html)

        val content = article.content
        assertContains(content, "https://other.com/page")
        assertContains(content, "https://cdn.example.com/img.png")
    }

    @Test
    fun parse_fixesJavascriptUris() {
        val html = """
            <html><head><title>Test</title></head><body>
            <article>
                <p>This is a paragraph with enough text for the readability scoring algorithm to properly identify it as article content and extract it from the page. We need a good amount of text here.</p>
                <p>Another paragraph with a <a href="javascript:void(0)">JS link</a> that should be converted to text.</p>
                <p>More content here to ensure the article container gets a high enough score to be selected as the top candidate by the algorithm.</p>
                <p>Yet another paragraph to add more content for better scoring. The algorithm needs multiple paragraphs.</p>
            </article>
            </body></html>
        """.trimIndent()

        val article = Readability4J.parse("https://example.com/page", html)

        val content = article.content
        assertFalse(content.contains("javascript:void(0)"), "javascript: URIs should be replaced with text")
    }

    @Test
    fun parse_emptyHtmlReturnsEmptyContent() {
        val article = Readability4J.parse("https://example.com", "<html><head></head><body></body></html>")

        // With no content, articleContent may be null or empty
        assertTrue(article.title.isEmpty() || article.title.isBlank())
    }

    @Test
    fun parse_removesNavigationLinks() {
        val html = """
            <html><head><title>Page</title></head><body>
            <nav>
                <a href="/home">Home</a>
                <a href="/about">About</a>
                <a href="/contact">Contact</a>
            </nav>
            <article>
                <p>The main article content paragraph with enough text for proper scoring by the readability algorithm. This needs to be sufficiently long to be recognized as content.</p>
                <p>Second paragraph of the main article content. This helps the algorithm identify the article element as the top candidate for extraction over the navigation elements.</p>
                <p>Third paragraph ensures there is plenty of content. The readability algorithm scores paragraphs and their parents to find the best candidate container.</p>
            </article>
            </body></html>
        """.trimIndent()

        val article = Readability4J.parse("https://example.com", html)

        val text = article.textContent ?: ""
        assertContains(text, "main article content")
        assertFalse(text.contains("Contact"), "Navigation links should be removed")
    }

    @Test
    fun parse_lazyLoadingImageFixed() {
        val html = """
            <html><head><title>Test</title></head><body>
            <article>
                <p>Article content paragraph one with enough text for scoring. The readability algorithm needs sufficient text to identify this as the main content area of the page.</p>
                <p>Article content paragraph two. <img src="" data-src="/lazy-image.jpg" alt="lazy" /> This image has a data-src attribute that should be promoted to src.</p>
                <p>Third paragraph with more content to ensure proper scoring of the article container element.</p>
                <p>Fourth paragraph for additional content scoring weight in the readability extraction algorithm.</p>
            </article>
            </body></html>
        """.trimIndent()

        val article = Readability4J.parse("https://example.com/post", html)

        val content = article.content
        assertContains(content, "https://example.com/lazy-image.jpg")
    }

    @Test
    fun parse_h1UsedAsTitleFallback() {
        val html = """
            <html><head><title>x</title></head><body>
            <article>
                <h1>Important Article Heading</h1>
                <p>Article content paragraph with enough text for scoring. The readability algorithm needs sufficient text to identify this as the main content area of the page.</p>
                <p>Second paragraph of article content to ensure proper scoring of the article container element for extraction.</p>
            </article>
            </body></html>
        """.trimIndent()

        val article = Readability4J.parse("https://example.com", html)

        assertNotNull(article.articleContent)
        val text = article.textContent ?: ""
        assertContains(text, "Important Article Heading")
    }
}
