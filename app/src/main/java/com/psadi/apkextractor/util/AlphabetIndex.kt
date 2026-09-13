package com.psadi.apkextractor.util

import com.psadi.apkextractor.data.model.AppInfo

/**
 * Pure helpers backing the vertical A-Z fast-scroller.
 *
 * Keeping this math out of the composable lets us unit test the
 * "scroll to the alphabetical section" behaviour without an Android runtime.
 */

/**
 * Maps a vertical touch offset (in pixels, relative to the top of the letter
 * track) to an index in the [letterCount]-sized alphabet.
 *
 * The track lays the letters out evenly, so the offset is converted to a
 * fraction of the track height and scaled by the number of letters. Both
 * bounds are clamped so touches above/below the track resolve to the
 * first/last letter instead of crashing.
 */
internal fun letterIndexForOffset(
    offsetY: Float,
    trackHeight: Float,
    letterCount: Int
): Int {
    if (letterCount <= 0) return 0
    if (trackHeight <= 0f) return 0
    val fraction = (offsetY / trackHeight).coerceIn(0f, 0.999999f)
    return (fraction * letterCount).toInt().coerceIn(0, letterCount - 1)
}

/**
 * Resolves the index of the app list section that should be scrolled to for
 * the letter at [index].
 *
 * When the active letter has no section of its own (for example the user
 * dragged across a letter with no apps), the next populated section is used.
 * If there is nothing at or after [index], the last populated section wins.
 */
internal fun resolveTargetListIndex(
    alphabet: List<Char>,
    index: Int,
    alphabetMap: Map<Char, Int>
): Int? {
    if (alphabetMap.isEmpty()) return null
    if (index !in alphabet.indices) return alphabetMap.values.lastOrNull()
    return alphabetMap[alphabet[index]]
        ?: alphabet.drop(index).firstNotNullOfOrNull { alphabetMap[it] }
        ?: alphabetMap.values.lastOrNull()
}

/**
 * Computes the first list position for each alphabetical section.
 *
 * Apps whose display name does not start with an A-Z character are grouped
 * under the '#' section. Only the first index of each section is stored.
 */
internal fun computeAlphabetIndexMap(apps: List<AppInfo>): Map<Char, Int> {
    val map = LinkedHashMap<Char, Int>()
    apps.forEachIndexed { index, app ->
        val firstChar = app.appName.firstOrNull()?.uppercaseChar() ?: '#'
        val key = if (firstChar in 'A'..'Z') firstChar else '#'
        if (!map.containsKey(key)) {
            map[key] = index
        }
    }
    return map
}
