package com.ismartcoding.plain.lib.readability4j.processor

import com.ismartcoding.plain.lib.html2md.HtmlElement
import com.ismartcoding.plain.lib.html2md.HtmlNode
import com.ismartcoding.plain.lib.html2md.HtmlTextNode
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.readability4j.ArticleGrabberOptions
import com.ismartcoding.plain.lib.readability4j.ArticleMetadata
import com.ismartcoding.plain.lib.readability4j.ReadabilityObject
import com.ismartcoding.plain.lib.readability4j.ReadabilityOptions
import com.ismartcoding.plain.lib.readability4j.RegExUtil
import kotlin.math.abs
import kotlin.math.floor

internal class ArticleGrabber(val options: ReadabilityOptions) {

    companion object {
        val DEFAULT_TAGS_TO_SCORE = listOf("section", "h2", "h3", "h4", "h5", "h6", "p", "td", "pre")

        val DIV_TO_P_ELEMS = listOf("a", "blockquote", "dl", "div", "img", "ol", "p", "pre", "table", "ul", "select")

        val ALTER_TO_DIV_EXCEPTIONS = listOf("div", "article", "section", "p")

        val PRESENTATIONAL_ATTRIBUTES = listOf("align", "background", "bgcolor", "border", "cellpadding", "cellspacing", "frame", "hspace", "rules", "style", "valign", "vspace")

        val DEPRECATED_SIZE_ATTRIBUTE_ELEMS = listOf("table", "th", "td", "hr", "pre")

        val EMBEDDED_NODES = listOf("object", "embed", "iframe")

        val DATA_TABLE_DESCENDANTS = listOf("col", "colgroup", "tfoot", "thead", "th")
    }

    private fun HtmlElement.parentElement(): HtmlElement? = parent() as? HtmlElement

    var articleByline: String? = null

    var articleDir: String? = null

    val nbTopCandidates = options.nbTopCandidates
    val wordThreshold = options.wordThreshold

    val readabilityObjects = HashMap<HtmlElement, ReadabilityObject>()
    val readabilityDataTable = HashMap<HtmlElement, Boolean>()

    fun grabArticle(doc: HtmlElement, metadata: ArticleMetadata, options: ArticleGrabberOptions = ArticleGrabberOptions(), pageElement: HtmlElement? = null): HtmlElement? {
        LogCat.d("**** grabArticle ****")

        val isPaging = pageElement != null
        val page = pageElement ?: doc.body()

        if (page == null) {
            LogCat.d("No body found in document. Abort.")
            return null
        }

        val pageCacheHtml = doc.html()

        while (true) {
            val elementsToScore = prepareNodes(doc, options)
            val candidates = scoreElements(elementsToScore, options)
            val topCandidateResult = getTopCandidate(page, candidates, options)
            val topCandidate = topCandidateResult.first
            val neededToCreateTopCandidate = topCandidateResult.second

            var articleContent = createArticleContent(doc, topCandidate, isPaging)
            prepArticle(articleContent, options, metadata)

            if (neededToCreateTopCandidate) {
                topCandidate.attr("id", "readability-page-1")
                topCandidate.addClass("page")
            } else {
                val div = doc.createElement("div")
                div.attr("id", "readability-page-1")
                div.addClass("page")

                articleContent.childNodes().toList().forEach { child ->
                    child.remove()
                    div.appendChild(child)
                }

                articleContent.appendChild(div)
            }

            var parseSuccessful = true
            val attempts = mutableListOf<Pair<HtmlElement, Int>>()

            val textLength = ProcessorHelper.getInnerText(articleContent, true).length
            if (textLength < this.wordThreshold) {
                parseSuccessful = false
                page.html(pageCacheHtml)

                if (options.stripUnlikelyCandidates) {
                    options.stripUnlikelyCandidates = false
                    attempts.add(Pair(articleContent, textLength))
                } else if (options.weightClasses) {
                    options.weightClasses = false
                    attempts.add(Pair(articleContent, textLength))
                } else if (options.cleanConditionally) {
                    options.cleanConditionally = false
                    attempts.add(Pair(articleContent, textLength))
                } else {
                    attempts.add(Pair(articleContent, textLength))
                    attempts.sortBy { it.second }

                    if (attempts.isEmpty() || attempts[0].second <= 0) {
                        return null
                    }

                    articleContent = attempts[0].first
                    parseSuccessful = true
                }
            }

            if (parseSuccessful) {
                getTextDirection(topCandidate, doc)
                return articleContent
            }
        }
    }

    fun prepareNodes(doc: HtmlElement, options: ArticleGrabberOptions): List<HtmlElement> {
        val elementsToScore = mutableListOf<HtmlElement>()
        var node: HtmlElement? = doc

        while (node != null) {
            val matchString = node.className() + " " + node.id()

            if (checkByline(node, matchString)) {
                node = removeAndGetNext(node, "byline")
                continue
            }

            if (options.stripUnlikelyCandidates) {
                if (RegExUtil.isUnlikelyCandidate(matchString) && !RegExUtil.okMaybeItsACandidate(matchString) && node.tagName() != "body" && node.tagName() != "a"
                ) {
                    node = this.removeAndGetNext(node, "Removing unlikely candidate")
                    continue
                }
            }

            if ((node.tagName() == "div" || node.tagName() == "section" || node.tagName() == "header" ||
                        node.tagName() == "h1" || node.tagName() == "h2" || node.tagName() == "h3" ||
                        node.tagName() == "h4" || node.tagName() == "h5" || node.tagName() == "h6") &&
                this.isElementWithoutContent(node)
            ) {
                node = this.removeAndGetNext(node, "node without content")
                continue
            }

            if (DEFAULT_TAGS_TO_SCORE.contains(node.tagName())) {
                elementsToScore.add(node)
            }

            if (node.tagName() == "div") {
                if (this.hasSinglePInsideElement(node)) {
                    val newNode = node.child(0)
                    node.replaceWith(newNode)
                    node = newNode
                    elementsToScore.add(node)
                } else if (!this.hasChildBlockElement(node)) {
                    setNodeTag(node, "p")
                    elementsToScore.add(node)
                } else {
                    node.childNodes().forEach { childNode ->
                        if (childNode is HtmlTextNode && childNode.text().trim().isNotEmpty()) {
                            val p = doc.createElement("p")
                            p.text(childNode.text())
                            p.attr("style", "display: inline;")
                            p.addClass("readability-styled")
                            childNode.replaceWith(p)
                        }
                    }
                }
            }

            node = if (node != null) this.getNextNode(node) else null
        }

        return elementsToScore
    }

    private fun checkByline(node: HtmlElement, matchString: String): Boolean {
        if (this.articleByline != null) {
            return false
        }

        val rel = node.attr("rel")

        if ((rel == "author" || RegExUtil.isByline(matchString)) && isValidByline(node.wholeText())) {
            this.articleByline = node.text().trim()
            return true
        }

        return false
    }

    private fun isValidByline(text: String): Boolean {
        val byline = text.trim()
        return (byline.isNotEmpty()) && (byline.length < 100)
    }

    private fun isElementWithoutContent(node: HtmlElement): Boolean {
        return node.text().isBlank() &&
                (node.children().isEmpty() ||
                        node.children().size == node.getElementsByTag("br").size + node.getElementsByTag("hr").size)
    }

    private fun hasSinglePInsideElement(element: HtmlElement): Boolean {
        if (element.children().size != 1 || element.child(0).tagName() != "p") {
            return false
        }

        element.childNodes().forEach { node ->
            if (node is HtmlTextNode && RegExUtil.hasContent(node.text())) {
                return false
            }
        }

        return true
    }

    private fun hasChildBlockElement(element: HtmlElement): Boolean {
        element.children().forEach { node ->
            if (DIV_TO_P_ELEMS.contains(node.tagName()) || hasChildBlockElement(node)) {
                return true
            }
        }

        return false
    }

    private fun setNodeTag(node: HtmlElement, tagName: String) {
        node.tagName(tagName)
    }

    private fun scoreElements(elementsToScore: List<HtmlElement>, options: ArticleGrabberOptions): List<HtmlElement> {
        val candidates = mutableListOf<HtmlElement>()

        elementsToScore.forEach { elementToScore ->
            if (elementToScore.parentNode() == null) {
                return@forEach
            }

            val innerText = ProcessorHelper.getInnerText(elementToScore)
            if (innerText.length < 25) {
                return@forEach
            }

            val ancestors = this.getNodeAncestors(elementToScore, 3)
            if (ancestors.isEmpty()) {
                return@forEach
            }

            var contentScore = 0.0
            contentScore += 1
            contentScore += innerText.split(',').size
            contentScore += floor(innerText.length / 100.0).coerceAtMost(3.0)

            for (level in ancestors.indices) {
                val ancestor = ancestors[level]
                if (ancestor.tagName().isBlank()) {
                    return@forEach
                }

                if (getReadabilityObject(ancestor) == null) {
                    candidates.add(ancestor)
                    initializeNode(ancestor, options)
                }

                val scoreDivider =
                    if (level == 0) 1
                    else if (level == 1) 2
                    else level * 3

                getReadabilityObject(ancestor)?.let { readability ->
                    readability.contentScore += contentScore / scoreDivider.toDouble()
                }
            }
        }

        return candidates
    }

    private fun initializeNode(node: HtmlElement, options: ArticleGrabberOptions): ReadabilityObject {
        val readability = ReadabilityObject(0.0)
        readabilityObjects[node] = readability

        when (node.tagName()) {
            "div" -> readability.contentScore += 5
            "pre", "td", "blockquote" -> readability.contentScore += 3
            "address", "ol", "ul", "dl", "dd", "dt", "li", "form" -> readability.contentScore -= 3
            "h1", "h2", "h3", "h4", "h5", "h6", "th" -> readability.contentScore -= 5
        }

        readability.contentScore += getClassWeight(node, options)

        return readability
    }

    private fun getClassWeight(e: HtmlElement, options: ArticleGrabberOptions): Int {
        if (!options.weightClasses) {
            return 0
        }

        var weight = 0

        if (e.className().isNotBlank()) {
            if (RegExUtil.isNegative(e.className())) {
                weight -= 25
            }
            if (RegExUtil.isPositive(e.className())) {
                weight += 25
            }
        }

        if (e.id().isNotBlank()) {
            if (RegExUtil.isNegative(e.id())) {
                weight -= 25
            }
            if (RegExUtil.isPositive(e.id())) {
                weight += 25
            }
        }

        return weight
    }

    private fun getNodeAncestors(node: HtmlElement, maxDepth: Int = 0): List<HtmlElement> {
        var i = 0
        val ancestors = mutableListOf<HtmlElement>()
        var next = node

        while (next.parent() != null) {
            val parentElem = next.parent() as HtmlElement
            ancestors.add(parentElem)
            if (++i == maxDepth) {
                break
            }
            next = parentElem
        }

        return ancestors
    }

    private fun getTopCandidate(page: HtmlElement, candidates: List<HtmlElement>, options: ArticleGrabberOptions): Pair<HtmlElement, Boolean> {
        val topCandidates = mutableListOf<HtmlElement>()

        candidates.forEach { candidate ->
            getReadabilityObject(candidate)?.let { readability ->
                val candidateScore = readability.contentScore * (1 - this.getLinkDensity(candidate))
                readability.contentScore = candidateScore

                LogCat.d("Candidate: $candidate with score $candidateScore")

                for (t in 0..<nbTopCandidates) {
                    val aTopCandidate = if (topCandidates.size > t) topCandidates[t] else null
                    val topCandidateReadability = if (aTopCandidate != null) getReadabilityObject(aTopCandidate) else null

                    if (aTopCandidate == null || (topCandidateReadability != null && candidateScore > topCandidateReadability.contentScore)) {
                        topCandidates.add(t, candidate)

                        if (topCandidates.size > this.nbTopCandidates) {
                            topCandidates.removeAt(nbTopCandidates)
                        }
                        break
                    }
                }
            }
        }

        var topCandidate = if (topCandidates.isNotEmpty()) topCandidates[0] else null
        var parentOfTopCandidate: HtmlElement?

        if (topCandidate == null || topCandidate.tagName() == "body") {
            topCandidate = HtmlElement("div")
            page.childNodes().toList().forEach { child ->
                LogCat.d("Moving child out: $child")
                child.remove()
                topCandidate?.appendChild(child)
            }

            page.appendChild(topCandidate)
            this.initializeNode(topCandidate, options)

            return Pair(topCandidate, true)
        } else {
            val alternativeCandidateAncestors = mutableListOf<List<HtmlElement>>()

            getReadabilityObject(topCandidate)?.let { topCandidateReadability ->
                topCandidates.filter { it != topCandidate }.forEach { otherTopCandidate ->
                    if (((getReadabilityObject(otherTopCandidate)?.contentScore ?: 0.0) / topCandidateReadability.contentScore) >= 0.75) {
                        alternativeCandidateAncestors.add(this.getNodeAncestors(otherTopCandidate))
                    }
                }
            }

            val MINIMUM_TOPCANDIDATES = 3
            if (alternativeCandidateAncestors.size >= MINIMUM_TOPCANDIDATES) {
                parentOfTopCandidate = topCandidate.parentElement()

                while (parentOfTopCandidate != null && parentOfTopCandidate.tagName() != "body") {
                    var listsContainingThisAncestor = 0
                    var ancestorIndex = 0
                    while (ancestorIndex < alternativeCandidateAncestors.size && listsContainingThisAncestor < MINIMUM_TOPCANDIDATES) {
                        if (alternativeCandidateAncestors[ancestorIndex].contains(parentOfTopCandidate)) {
                            listsContainingThisAncestor++
                        }
                        ancestorIndex++
                    }

                    if (listsContainingThisAncestor >= MINIMUM_TOPCANDIDATES) {
                        topCandidate = parentOfTopCandidate
                        break
                    }
                    parentOfTopCandidate = parentOfTopCandidate.parentElement()
                }
            }

            topCandidate = topCandidate!!
            if (getReadabilityObject(topCandidate) == null) {
                this.initializeNode(topCandidate, options)
            }

            parentOfTopCandidate = topCandidate.parentElement()
            var lastScore = getReadabilityObject(topCandidate)?.contentScore ?: 0.0
            val scoreThreshold = lastScore / 3.0

            while (parentOfTopCandidate != null && parentOfTopCandidate.tagName() != "body") {
                val parentOfTopCandidateReadability = getReadabilityObject(parentOfTopCandidate)
                if (parentOfTopCandidateReadability == null) {
                    parentOfTopCandidate = parentOfTopCandidate.parentElement()
                    continue
                }

                val parentScore = parentOfTopCandidateReadability.contentScore
                if (parentScore < scoreThreshold) {
                    break
                }
                if (parentScore > lastScore) {
                    topCandidate = parentOfTopCandidate
                    break
                }

                lastScore = parentOfTopCandidateReadability.contentScore
                parentOfTopCandidate = parentOfTopCandidate.parentElement()
            }

            topCandidate = topCandidate!!
            parentOfTopCandidate = topCandidate.parentElement()
            while (parentOfTopCandidate != null && parentOfTopCandidate.tagName() != "body" && parentOfTopCandidate.children().size == 1) {
                topCandidate = parentOfTopCandidate
                parentOfTopCandidate = topCandidate.parentElement()
            }

            topCandidate = topCandidate!!
            if (getReadabilityObject(topCandidate) == null) {
                this.initializeNode(topCandidate, options)
            }

            return Pair(topCandidate, false)
        }
    }

    fun getLinkDensity(element: HtmlElement): Double {
        val textLength = ProcessorHelper.getInnerText(element).length
        if (textLength == 0) {
            return 0.0
        }

        var linkLength = 0
        element.getElementsByTag("a").forEach { linkNode ->
            linkLength += ProcessorHelper.getInnerText(linkNode).length
        }

        return linkLength / textLength.toDouble()
    }

    private fun createArticleContent(doc: HtmlElement, topCandidate: HtmlElement, isPaging: Boolean): HtmlElement {
        val articleContent = doc.createElement("div")
        if (isPaging) {
            articleContent.attr("id", "readability-content")
        }

        val topCandidateReadability = getReadabilityObject(topCandidate) ?: return articleContent

        val siblingScoreThreshold = 10.0.coerceAtLeast(topCandidateReadability.contentScore * 0.2)
        val parentOfTopCandidate = topCandidate.parentElement()
        val siblings = parentOfTopCandidate?.children() ?: emptyList()

        siblings.toList().forEach { sibling ->
            var append = false

            val siblingReadability = getReadabilityObject(sibling)
            LogCat.d("Looking at sibling node: $sibling with score ${siblingReadability?.contentScore ?: 0}")
            LogCat.d("Sibling has score ${siblingReadability?.contentScore?.toString() ?: "Unknown"}")

            if (sibling === topCandidate) {
                append = true
            } else {
                var contentBonus = 0.0

                if (sibling.className() == topCandidate.className() && topCandidate.className() != "")
                    contentBonus += topCandidateReadability.contentScore * 0.2

                if (siblingReadability != null &&
                    ((siblingReadability.contentScore + contentBonus) >= siblingScoreThreshold)
                ) {
                    append = true
                } else if (shouldKeepSibling(sibling)) {
                    val linkDensity = this.getLinkDensity(sibling)
                    val nodeContent = ProcessorHelper.getInnerText(sibling)
                    val nodeLength = nodeContent.length

                    if (nodeLength > 80 && linkDensity < 0.25) {
                        append = true
                    } else if (nodeLength in 1..79 && linkDensity == 0.0 &&
                        nodeContent.contains("\\.( |$)".toRegex())
                    ) {
                        append = true
                    }
                }
            }

            if (append) {
                LogCat.d("Appending node: $sibling")

                if (!ALTER_TO_DIV_EXCEPTIONS.contains(sibling.tagName())) {
                    LogCat.d("Altering sibling: $sibling to div.")
                    setNodeTag(sibling, "div")
                }

                articleContent.appendChild(sibling)
            }
        }

        return articleContent
    }

    private fun shouldKeepSibling(sibling: HtmlElement): Boolean {
        return sibling.tagName() == "p" || containsImageToKeep(sibling)
    }

    private fun containsImageToKeep(element: HtmlElement): Boolean {
        val images = element.select("img")
        if (images.isNotEmpty()) {
            if (isImageElementToKeep(element)) {
                images.forEach { image ->
                    if (!isImageElementToKeep(image)) {
                        return false
                    }
                }

                return true
            }
        }

        return false
    }

    private fun isImageElementToKeep(element: HtmlElement): Boolean {
        val matchString = element.id() + " " + element.className()
        return RegExUtil.keepImage(matchString)
    }

    private fun prepArticle(articleContent: HtmlElement, options: ArticleGrabberOptions, metadata: ArticleMetadata) {
        this.cleanStyles(articleContent)
        markDataTables(articleContent)

        this.cleanConditionally(articleContent, "form", options)
        this.cleanConditionally(articleContent, "fieldset", options)
        this.clean(articleContent, "object")
        this.clean(articleContent, "embed")
        this.clean(articleContent, "footer")
        this.clean(articleContent, "link")

        val shareRegex = "share".toRegex()
        articleContent.children().forEach { topCandidate ->
            cleanMatchedNodes(topCandidate, shareRegex)
        }

        val h2 = articleContent.getElementsByTag("h2")
        if (h2.size == 1) {
            val articleTitle = metadata.title
            if (articleTitle.isNotEmpty()) {
                val lengthSimilarRate = (h2[0].text().length - articleTitle.length) / articleTitle.length.toFloat()
                if (abs(lengthSimilarRate) < 0.5) {
                    val titlesMatch =
                        if (lengthSimilarRate > 0) {
                            h2[0].text().contains(articleTitle)
                        } else {
                            articleTitle.contains(h2[0].text())
                        }

                    if (titlesMatch) {
                        this.clean(articleContent, "h2")
                    }
                }
            }
        }

        this.clean(articleContent, "iframe")
        this.clean(articleContent, "input")
        this.clean(articleContent, "textarea")
        this.clean(articleContent, "select")
        this.clean(articleContent, "button")
        this.cleanHeaders(articleContent, options)

        this.cleanConditionally(articleContent, "table", options)
        this.cleanConditionally(articleContent, "ul", options)
        this.cleanConditionally(articleContent, "div", options)

        ProcessorHelper.removeNodes(articleContent, "p") { paragraph ->
            val imgCount = paragraph.getElementsByTag("img").size
            val embedCount = paragraph.getElementsByTag("embed").size
            val objectCount = paragraph.getElementsByTag("object").size
            val iframeCount = paragraph.getElementsByTag("iframe").size
            val totalCount = imgCount + embedCount + objectCount + iframeCount

            return@removeNodes totalCount == 0 && ProcessorHelper.getInnerText(paragraph, normalizeSpaces = false).isEmpty()
        }

        articleContent.select("br").forEach { br ->
            val next = ProcessorHelper.nextElement(br.nextSibling())
            if (next != null && next.tagName() == "p") {
                br.remove()
            }
        }
    }

    private fun cleanStyles(e: HtmlElement) {
        if (e.tagName() == "svg") {
            return
        }

        if (e.className() != "readability-styled") {
            PRESENTATIONAL_ATTRIBUTES.forEach { attributeName ->
                e.removeAttr(attributeName)
            }

            if (DEPRECATED_SIZE_ATTRIBUTE_ELEMS.contains(e.tagName())) {
                e.removeAttr("width")
                e.removeAttr("height")
            }
        }

        e.children().forEach { child ->
            cleanStyles(child)
        }
    }

    private fun markDataTables(root: HtmlElement) {
        root.getElementsByTag("table").forEach outer@{ table ->
            val role = table.attr("role")
            if (role == "presentation") {
                setReadabilityDataTable(table, false)
                return@outer
            }
            val datatable = table.attr("datatable")
            if (datatable == "0") {
                setReadabilityDataTable(table, false)
                return@outer
            }
            val summary = table.attr("summary")
            if (summary.isNotBlank()) {
                setReadabilityDataTable(table, true)
                return@outer
            }

            val caption = table.getElementsByTag("caption")
            if (caption.isNotEmpty() && caption[0].childNodeSize() > 0) {
                setReadabilityDataTable(table, true)
                return@outer
            }

            DATA_TABLE_DESCENDANTS.forEach { tag ->
                if (table.getElementsByTag(tag).isNotEmpty()) {
                    LogCat.d("Data table because found data-y descendant")
                    setReadabilityDataTable(table, true)
                    return@outer
                }
            }

            if (table.getElementsByTag("table").isNotEmpty()) {
                setReadabilityDataTable(table, false)
                return@outer
            }

            val sizeInfo = getRowAndColumnCount(table)
            if (sizeInfo.first >= 10 || sizeInfo.second > 4) {
                setReadabilityDataTable(table, true)
                return@outer
            }

            setReadabilityDataTable(table, sizeInfo.first * sizeInfo.second > 10)
        }
    }

    private fun getRowAndColumnCount(table: HtmlElement): Pair<Int, Int> {
        var rows = 0
        var columns = 0

        val trs = table.getElementsByTag("tr")
        trs.forEach { tr ->
            rows += try {
                tr.attr("rowspan").toInt()
            } catch (ignored: Exception) {
                1
            }

            var columnsInThisRow = 0
            tr.getElementsByTag("td").forEach { cell ->
                columnsInThisRow += try {
                    cell.attr("colspan").toInt()
                } catch (ignored: Exception) {
                    1
                }
            }

            columns = maxOf(columns, columnsInThisRow)
        }

        return Pair(rows, columns)
    }

    private fun cleanConditionally(e: HtmlElement, tag: String, options: ArticleGrabberOptions) {
        if (!options.cleanConditionally)
            return

        val isList = tag == "ul" || tag == "ol"

        ProcessorHelper.removeNodes(e, tag) { node ->
            val isDataTable: (HtmlElement) -> Boolean = { element ->
                getReadabilityDataTable(element)
            }

            if (hasAncestorTag(node, "table", -1, isDataTable)) {
                return@removeNodes false
            }

            val weight = getClassWeight(node, options)
            val contentScore = 0

            LogCat.d("Cleaning Conditionally $node")

            if (weight + contentScore < 0) {
                return@removeNodes true
            }

            if (getCharCount(node, ',') < 10) {
                val p = node.getElementsByTag("p").size
                val img = node.getElementsByTag("img").size
                val li = node.getElementsByTag("li").size - 100
                val input = node.getElementsByTag("input").size

                var embedCount = 0
                node.getElementsByTag("embed").forEach {
                    if (!RegExUtil.isVideo(it.attr("src"))) {
                        embedCount += 1
                    }
                }

                val linkDensity = getLinkDensity(node)
                val contentLength = ProcessorHelper.getInnerText(node).length

                return@removeNodes (img > 1 && p / img.toFloat() < 0.5 && !hasAncestorTag(node, "figure")) ||
                        (!isList && li > p) ||
                        (input > floor(p / 3.0)) ||
                        (!isList && contentLength < 25 && img == 0 && !hasAncestorTag(node, "figure")) ||
                        (!isList && weight < 25 && linkDensity > 0.2) ||
                        (weight >= 25 && linkDensity > 0.5) ||
                        ((embedCount == 1 && contentLength < 75) || embedCount > 1)
            }

            return@removeNodes false
        }
    }

    private fun hasAncestorTag(node: HtmlElement, tagName: String, maxDepth: Int = 3, filterFn: ((HtmlElement) -> Boolean)? = null): Boolean {
        val tagNameLowerCase = tagName.lowercase()
        var parent = node
        var depth = 0

        while (parent.parent() != null) {
            if (maxDepth in 1..<depth) {
                return false
            }

            val parent2 = parent.parent() as? HtmlElement
            if (parent2 != null && parent2.tagName() == tagNameLowerCase && (filterFn == null || filterFn(parent2))) {
                return true
            }

            if (parent2 == null) break
            parent = parent2
            depth++
        }

        return false
    }

    private fun getCharCount(node: HtmlElement, c: Char = ','): Int {
        return ProcessorHelper.getInnerText(node).split(c).size - 1
    }

    private fun clean(e: HtmlElement, tag: String) {
        val isEmbed = EMBEDDED_NODES.contains(tag)

        ProcessorHelper.removeNodes(e, tag) { element ->
            if (isEmbed) {
                val attributeValues = element.attributes().joinToString("|") { it.value }

                if (RegExUtil.isVideo(attributeValues)) {
                    return@removeNodes false
                }

                if (RegExUtil.isVideo(element.html())) {
                    return@removeNodes false
                }
            }

            return@removeNodes true
        }
    }

    private fun cleanMatchedNodes(e: HtmlElement, regex: Regex) {
        val endOfSearchMarkerNode = getNextNode(e, true)
        var next = getNextNode(e)

        while (next != null && next != endOfSearchMarkerNode) {
            if (regex.containsMatchIn(next.className() + " " + next.id())) {
                next = removeAndGetNext(next, regex.pattern)
            } else {
                next = getNextNode(next)
            }
        }
    }

    private fun cleanHeaders(e: HtmlElement, options: ArticleGrabberOptions) {
        listOf("h1", "h2").forEach {
            ProcessorHelper.removeNodes(e, it) { header ->
                getClassWeight(header, options) < 0
            }
        }
    }

    private fun removeAndGetNext(node: HtmlElement, reason: String = ""): HtmlElement? {
        val nextNode = this.getNextNode(node, true)
        ProcessorHelper.printAndRemove(node, reason)
        return nextNode
    }

    private fun getNextNode(node: HtmlElement, ignoreSelfAndKids: Boolean = false): HtmlElement? {
        if (!ignoreSelfAndKids && node.children().isNotEmpty()) {
            return node.child(0)
        }

        node.nextElementSibling()?.let { return it }

        var parent = node.parent()
        while (parent != null && parent is HtmlElement && parent.nextElementSibling() == null) {
            parent = parent.parent()
        }

        return (parent as? HtmlElement)?.nextElementSibling()
    }

    private fun getTextDirection(topCandidate: HtmlElement, doc: HtmlElement) {
        val parent = topCandidate.parentElement()
        val ancestors = mutableSetOf<HtmlElement?>(parent, topCandidate)
        if (parent != null) {
            ancestors.addAll(getNodeAncestors(parent))
        }
        ancestors.add(doc.body())
        ancestors.add(doc.getElementsByTag("html").firstOrNull() ?: doc)

        ancestors.filterNotNull().forEach { ancestor ->
            val articleDir = ancestor.attr("dir")
            if (articleDir.isNotBlank()) {
                this.articleDir = articleDir
                return
            }
        }
    }

    private fun getReadabilityObject(element: HtmlElement): ReadabilityObject? {
        return readabilityObjects[element]
    }

    private fun getReadabilityDataTable(table: HtmlElement): Boolean {
        return this.readabilityDataTable[table] ?: false
    }

    private fun setReadabilityDataTable(table: HtmlElement, readabilityDataTable: Boolean) {
        this.readabilityDataTable[table] = readabilityDataTable
    }
}
