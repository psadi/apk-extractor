package com.psadi.apkextractor.util

import com.psadi.apkextractor.data.model.AppInfo
import com.psadi.apkextractor.data.model.ExtractedApk

object SearchUtil {

    /**
     * Extracts generic search aliases and variations from any app's package name and display label.
     * Generates:
     * - Delimiter-split word tokens
     * - CamelCase-split word tokens
     * - Normalized continuous string without spaces/punctuation (e.g. "Play Station" -> "playstation")
     * - Space-separated CamelCase (e.g. "PlayStation" -> "play station")
     * - Acronym/initials (e.g. "Google Chrome" -> "gc", "Call of Duty" -> "cod")
     * - Non-TLD package segments and sub-tokens
     */
    fun getSearchAliases(packageName: String, appName: String): List<String> {
        val aliases = LinkedHashSet<String>()
        val lowerName = appName.trim().lowercase()
        val lowerPkg = packageName.trim().lowercase()

        // 1. Normalized alphanumeric representation without spaces or punctuation
        val strippedName = lowerName.filter { it.isLetterOrDigit() }
        if (strippedName.isNotEmpty() && strippedName != lowerName) {
            aliases.add(strippedName)
        }

        // 2. CamelCase splitting on app label (e.g. "PlayStation" -> ["play", "station"])
        val camelWords = splitCamelCase(appName)
        if (camelWords.size > 1) {
            val spaceSeparated = camelWords.joinToString(" ").lowercase()
            aliases.add(spaceSeparated)
            aliases.addAll(camelWords.map { it.lowercase() })
        }

        // 3. Delimiter splitting on app label (spaces, dashes, underscores, dots)
        val nameDelimWords = appName.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        if (nameDelimWords.size > 1) {
            aliases.addAll(nameDelimWords.map { it.lowercase() })
            // Generate initials / acronym (e.g. "PS App" -> "psa", "Call of Duty" -> "cod")
            if (nameDelimWords.size in 2..5) {
                val acronym = nameDelimWords.mapNotNull { it.firstOrNull()?.lowercaseChar() }.joinToString("")
                if (acronym.isNotEmpty()) {
                    aliases.add(acronym)
                }
            }
        }

        // 4. Package name segments (ignoring common prefix/TLDs like com, org, net, io, android)
        val commonPrefixes = setOf("com", "org", "net", "io", "edu", "gov", "android", "apps")
        val pkgSegments = lowerPkg.split('.').filter { it.isNotBlank() }
        for (segment in pkgSegments) {
            if (segment !in commonPrefixes) {
                aliases.add(segment)
                val subSegments = segment.split(Regex("[^a-zA-Z]+")).filter { it.length >= 2 }
                aliases.addAll(subSegments)
            }
        }

        val strippedPkg = lowerPkg.filter { it.isLetterOrDigit() }
        if (strippedPkg.isNotEmpty()) {
            aliases.add(strippedPkg)
        }

        return aliases.toList()
    }

    /**
     * Checks whether an installed application matches a user search query generically.
     */
    fun matchesApp(app: AppInfo, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true
        val lowerQuery = trimmed.lowercase()
        val strippedQuery = lowerQuery.filter { it.isLetterOrDigit() }

        // 1. Direct containment in app label or full package name
        if (app.appName.contains(lowerQuery, ignoreCase = true) ||
            app.packageName.contains(lowerQuery, ignoreCase = true)
        ) {
            return true
        }

        // 2. Normalized match on app name ignoring spaces and punctuation
        val strippedName = app.appName.filter { it.isLetterOrDigit() }.lowercase()
        if (strippedQuery.isNotEmpty() && strippedName.contains(strippedQuery)) {
            return true
        }

        // 3. Match against individual package name segments (prevents cross-segment collisions)
        val pkgSegments = app.packageName.lowercase().split('.').filter { it.isNotBlank() }
        if (pkgSegments.any { it.contains(lowerQuery) }) {
            return true
        }

        // 4. Check search aliases (acronyms, CamelCase words, delimiter tokens)
        if (app.searchAliases.any { alias ->
                if (strippedQuery.length <= 3) {
                    alias.equals(lowerQuery, ignoreCase = true) || alias.startsWith(lowerQuery, ignoreCase = true)
                } else {
                    alias.contains(lowerQuery, ignoreCase = true) ||
                            alias.filter { it.isLetterOrDigit() }.contains(strippedQuery)
                }
            }
        ) {
            return true
        }

        // 5. Multi-word query (all tokens in the query must match something in the app)
        val queryWords = lowerQuery.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (queryWords.size > 1) {
            val allWordsMatch = queryWords.all { word ->
                app.appName.contains(word, ignoreCase = true) ||
                        app.packageName.contains(word, ignoreCase = true) ||
                        app.searchAliases.any { it.contains(word, ignoreCase = true) }
            }
            if (allWordsMatch) return true
        }

        return false
    }

    /**
     * Checks whether an extracted backup APK matches a user search query generically.
     */
    fun matchesExtracted(item: ExtractedApk, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true
        val lowerQuery = trimmed.lowercase()
        val strippedQuery = lowerQuery.filter { it.isLetterOrDigit() }

        // 1. Direct containment in app label, package name, or file name
        if (item.appName.contains(lowerQuery, ignoreCase = true) ||
            item.packageName.contains(lowerQuery, ignoreCase = true) ||
            item.fileName.contains(lowerQuery, ignoreCase = true)
        ) {
            return true
        }

        // 2. Normalized match on app name or file name
        val strippedName = item.appName.filter { it.isLetterOrDigit() }.lowercase()
        val strippedFile = item.fileName.filter { it.isLetterOrDigit() }.lowercase()
        if (strippedQuery.isNotEmpty() &&
            (strippedName.contains(strippedQuery) || strippedFile.contains(strippedQuery))
        ) {
            return true
        }

        // 3. Match against package name segments
        val pkgSegments = item.packageName.lowercase().split('.').filter { it.isNotBlank() }
        if (pkgSegments.any { it.contains(lowerQuery) }) {
            return true
        }

        // 4. Check search aliases
        val aliases = getSearchAliases(item.packageName, item.appName)
        if (aliases.any { alias ->
                if (strippedQuery.length <= 3) {
                    alias.equals(lowerQuery, ignoreCase = true) || alias.startsWith(lowerQuery, ignoreCase = true)
                } else {
                    alias.contains(lowerQuery, ignoreCase = true) ||
                            alias.filter { it.isLetterOrDigit() }.contains(strippedQuery)
                }
            }
        ) {
            return true
        }

        // 5. Multi-word query
        val queryWords = lowerQuery.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (queryWords.size > 1) {
            val allWordsMatch = queryWords.all { word ->
                item.appName.contains(word, ignoreCase = true) ||
                        item.packageName.contains(word, ignoreCase = true) ||
                        item.fileName.contains(word, ignoreCase = true) ||
                        aliases.any { it.contains(word, ignoreCase = true) }
            }
            if (allWordsMatch) return true
        }

        return false
    }

    fun getBrandTag(packageName: String, appName: String): String? = null

    private fun splitCamelCase(text: String): List<String> {
        val parts = mutableListOf<String>()
        val sb = StringBuilder()
        for (i in text.indices) {
            val c = text[i]
            if (c.isUpperCase() && sb.isNotEmpty()) {
                val prev = text[i - 1]
                val next = if (i + 1 < text.length) text[i + 1] else null
                if (prev.isLowerCase() || (next != null && next.isLowerCase())) {
                    parts.add(sb.toString())
                    sb.clear()
                }
            }
            sb.append(c)
        }
        if (sb.isNotEmpty()) {
            parts.add(sb.toString())
        }
        return parts.filter { it.isNotBlank() }
    }
}
