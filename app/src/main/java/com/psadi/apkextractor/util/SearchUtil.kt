package com.psadi.apkextractor.util

object SearchUtil {

    fun getSearchAliases(packageName: String, appName: String): List<String> {
        val aliases = mutableListOf<String>()
        val pkg = packageName.lowercase()
        val name = appName.lowercase()

        // PlayStation ecosystem
        if (pkg.contains("scee") || pkg.contains("playstation") || pkg.contains("psx") ||
            name.contains("ps app") || name == "ps"
        ) {
            aliases.addAll(listOf("playstation", "play station", "sony", "psn", "ps app", "ps", "ps5", "ps4"))
        }

        // Google Play Store
        if (pkg == "com.android.vending") {
            aliases.addAll(listOf("google play store", "play store", "google play", "market", "vending"))
        }

        // Google Play Services
        if (pkg == "com.google.android.gms") {
            aliases.addAll(listOf("google play services", "play services", "gms"))
        }

        // YouTube / YouTube Music
        if (pkg.contains("youtube")) {
            aliases.addAll(listOf("youtube", "yt"))
        }

        // Meta apps
        if (pkg.contains("facebook.katana") || pkg.contains("facebook.orca")) {
            aliases.addAll(listOf("facebook", "fb", "meta", "messenger"))
        }
        if (pkg.contains("whatsapp")) {
            aliases.addAll(listOf("whatsapp", "wa"))
        }
        if (pkg.contains("instagram")) {
            aliases.addAll(listOf("instagram", "ig", "insta"))
        }

        // X / Twitter
        if (pkg.contains("twitter") || pkg == "com.x.android") {
            aliases.addAll(listOf("twitter", "x"))
        }

        return aliases
    }

    fun getBrandTag(packageName: String, appName: String): String? {
        val pkg = packageName.lowercase()
        val name = appName.lowercase()
        return when {
            pkg.contains("scee") || pkg.contains("playstation") || pkg.contains("psx") || name.contains("ps app") -> "PlayStation"
            pkg == "com.android.vending" -> "Play Store"
            pkg == "com.google.android.gms" -> "Play Services"
            pkg.contains("twitter") || pkg == "com.x.android" -> "Twitter / X"
            else -> null
        }
    }
}
