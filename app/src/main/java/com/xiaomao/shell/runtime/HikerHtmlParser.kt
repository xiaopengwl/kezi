package com.xiaomao.shell.runtime

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URL

class HikerHtmlParser(
    private val currentUrl: String = "",
) {
    private val noAddIndexPattern =
        Regex(":eq|:lt|:gt|:first|:last|:not|:even|:odd|:has|:contains|:matches|:empty|^body$|^#")
    private val urlJoinAttrPattern =
        Regex("(url|src|href|-original|-src|-play|-url|style)$|^(data-|url-|src-)", RegexOption.IGNORE_CASE)
    private val specialUrlPattern = Regex("^(ftp|magnet|thunder|ws):", RegexOption.IGNORE_CASE)
    private val adjacentLtGtPattern = Regex(":gt\\((\\d+)\\):lt\\((\\d+)\\)")

    fun pd(html: String, parse: String, baseUrl: String = currentUrl): String {
        return pdfh(html, parse, baseUrl)
    }

    fun pdfa(html: String, parse: String): List<String> {
        if (html.isBlank() || parse.isBlank()) return emptyList()
        val doc = Jsoup.parse(html)
        val tokens = parseHikerToJq(parse).split(' ')
        var ret: Elements? = null
        for (token in tokens) {
            ret = parseOneRule(doc, token, ret)
            if (ret == null || ret.isEmpty()) return emptyList()
        }
        return ret.orEmpty().map(Element::outerHtml)
    }

    fun pdfl(
        html: String,
        parse: String,
        listText: String,
        listUrl: String,
        urlKey: String = "",
    ): List<String> {
        if (html.isBlank() || parse.isBlank()) return emptyList()
        val doc = Jsoup.parse(html)
        val tokens = parseHikerToJq(parse).split(' ')
        var ret: Elements? = null
        for (token in tokens) {
            ret = parseOneRule(doc, token, ret)
            if (ret == null || ret.isEmpty()) return emptyList()
        }
        return ret.orEmpty().map { element ->
            val itemHtml = element.outerHtml()
            val title = pdfh(itemHtml, listText, urlKey)
            val url = pdfh(itemHtml, listUrl, urlKey)
            title + url
        }
    }

    fun pdfh(html: String, parse: String, baseUrl: String = currentUrl): String {
        if (html.isBlank() || parse.isBlank()) return ""
        val doc = Jsoup.parse(html)

        if (parse == "body&&Text" || parse == "Text") {
            return normalizeText(doc.text())
        }
        if (parse == "body&&Html" || parse == "Html") {
            return doc.body().html()
        }

        var actualParse = parse
        var option: String? = null
        if (actualParse.contains("&&")) {
            val parts = actualParse.split("&&")
            option = parts.lastOrNull()
            actualParse = parts.dropLast(1).joinToString("&&")
        }

        val tokens = parseHikerToJq(actualParse, first = true).split(' ')
        var ret: Elements? = null
        for (token in tokens) {
            ret = parseOneRule(doc, token, ret)
            if (ret == null || ret.isEmpty()) return ""
        }
        val elements = ret ?: return ""

        return when (option) {
            null -> elements.firstOrNull()?.outerHtml().orEmpty()
            "Text" -> normalizeText(elements.text())
            "Html" -> elements.firstOrNull()?.html().orEmpty()
            else -> extractOption(elements, option, baseUrl)
        }
    }

    private fun extractOption(elements: Elements, option: String, baseUrl: String): String {
        val original = elements.map(Element::clone)
        val options = option.split("||")
        for (candidate in options) {
            val temp = Elements()
            original.forEach(temp::add)
            var value = temp.attr(candidate).orEmpty()
            if (candidate.contains("style", ignoreCase = true) && value.contains("url(")) {
                Regex("url\\((.*?)\\)").find(value)?.groupValues?.getOrNull(1)?.let {
                    value = it.replace(Regex("^['\"]|['\"]$"), "")
                }
            }
            if (value.isNotBlank() && baseUrl.isNotBlank() && urlJoinAttrPattern.containsMatchIn(candidate) &&
                !specialUrlPattern.containsMatchIn(value)
            ) {
                value = if (value.contains("http")) {
                    value.substring(value.indexOf("http"))
                } else {
                    joinUrl(baseUrl, value)
                }
            }
            if (value.isNotBlank()) {
                return value
            }
        }
        return ""
    }

    private fun parseOneRule(doc: Document, token: String, ret: Elements?): Elements {
        var (rule, eqIndex, excludes) = parseInfo(token)
        rule = reorderAdjacentLtAndGt(rule)
        var next = if (ret == null) {
            doc.select(rule)
        } else {
            ret.select(rule)
        }
        if (eqIndex != null) {
            next = next.eq(eqIndex)
        }
        if (excludes.isNotEmpty()) {
            val cloned = Elements()
            next.forEach { element ->
                val copy = element.clone()
                excludes.forEach { exclude ->
                    copy.select(exclude).remove()
                }
                cloned.add(copy)
            }
            next = cloned
        }
        return next
    }

    private fun parseInfo(token: String): Triple<String, Int?, List<String>> {
        var rule = token
        var eqIndex: Int? = null
        var excludes = emptyList<String>()

        if (rule.contains(":eq")) {
            val beforeEq = rule.substringBefore(":eq")
            var afterEq = rule.substringAfter(":eq")
            rule = beforeEq
            if (beforeEq.contains("--")) {
                excludes = beforeEq.split("--").drop(1)
                rule = beforeEq.substringBefore("--")
            } else if (afterEq.contains("--")) {
                excludes = afterEq.split("--").drop(1)
                afterEq = afterEq.substringBefore("--")
            }
            eqIndex = afterEq.substringAfter('(', "0").substringBefore(')').toIntOrNull()
        } else if (rule.contains("--")) {
            excludes = rule.split("--").drop(1)
            rule = rule.substringBefore("--")
        }
        return Triple(rule, eqIndex, excludes)
    }

    private fun parseHikerToJq(parse: String, first: Boolean = false): String {
        if (!parse.contains("&&")) {
            val last = parse.substringAfterLast(' ')
            return if (first && !noAddIndexPattern.containsMatchIn(last)) "$parse:eq(0)" else parse
        }
        val parts = parse.split("&&")
        val rebuilt = parts.mapIndexed { index, raw ->
            val last = raw.substringAfterLast(' ')
            val shouldAdd = !noAddIndexPattern.containsMatchIn(last) &&
                (first || index < parts.lastIndex)
            if (shouldAdd) "$raw:eq(0)" else raw
        }
        return rebuilt.joinToString(" ")
    }

    private fun reorderAdjacentLtAndGt(selector: String): String {
        var result = selector
        while (true) {
            val match = adjacentLtGtPattern.find(result) ?: break
            val replacement = ":lt(${match.groupValues[2]}):gt(${match.groupValues[1]})"
            result = result.replaceRange(match.range, replacement)
        }
        return result
    }

    private fun normalizeText(text: String): String {
        return text.replace(Regex("\\s+"), "\n")
            .replace(Regex("\\n+"), "\n")
            .replace(Regex("^\\s+"), "")
            .replace("\n", " ")
    }

    private fun joinUrl(base: String, relative: String): String {
        return try {
            URL(URL(base), relative).toString()
        } catch (_: Throwable) {
            relative
        }
    }
}
