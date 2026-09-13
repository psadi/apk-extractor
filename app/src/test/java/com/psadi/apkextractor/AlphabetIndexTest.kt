package com.psadi.apkextractor

import com.psadi.apkextractor.data.model.AppInfo
import com.psadi.apkextractor.util.computeAlphabetIndexMap
import com.psadi.apkextractor.util.letterIndexForOffset
import com.psadi.apkextractor.util.resolveTargetListIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for the math behind the vertical A-Z fast-scroller and its floating
 * letter-preview bubble.
 */
class AlphabetIndexTest {

    private val alphabet = listOf('#') + ('A'..'Z').toList()

    private fun app(name: String) = AppInfo(
        appName = name,
        packageName = "com.example.${name.lowercase()}",
        versionName = "1.0",
        versionCode = 1L,
        minSdkVersion = 26,
        targetSdkVersion = 35,
        apkPath = "/data/app/base.apk",
        apkSize = 1L,
        isSystemApp = false,
        firstInstallTime = 0L,
        lastUpdateTime = 0L
    )

    @Test
    fun `touch offset maps to first and last letter`() {
        assertEquals(0, letterIndexForOffset(0f, 1000f, alphabet.size))
        assertEquals(alphabet.lastIndex, letterIndexForOffset(1000f, 1000f, alphabet.size))
    }

    @Test
    fun `touch offset maps to the middle of the track`() {
        // Halfway down a 27-letter track lands on index 13 (M).
        assertEquals(13, letterIndexForOffset(500f, 1000f, alphabet.size))
    }

    @Test
    fun `touch offset outside the track is clamped`() {
        assertEquals(0, letterIndexForOffset(-250f, 1000f, alphabet.size))
        assertEquals(alphabet.lastIndex, letterIndexForOffset(9000f, 1000f, alphabet.size))
    }

    @Test
    fun `degenerate track sizes do not crash`() {
        assertEquals(0, letterIndexForOffset(10f, 0f, alphabet.size))
        assertEquals(0, letterIndexForOffset(10f, 100f, 0))
    }

    @Test
    fun `resolves the exact section for a populated letter`() {
        val map = mapOf('A' to 0, 'M' to 5, 'Z' to 9)
        assertEquals(5, resolveTargetListIndex(alphabet, alphabet.indexOf('M'), map))
    }

    @Test
    fun `skips forward to the next populated section`() {
        val map = mapOf('A' to 0, 'M' to 5, 'Z' to 9)
        // C has no section of its own, so the next one (M) is used.
        assertEquals(5, resolveTargetListIndex(alphabet, alphabet.indexOf('C'), map))
    }

    @Test
    fun `falls back to the last populated section past the end`() {
        val map = mapOf('A' to 0, 'M' to 5)
        assertEquals(5, resolveTargetListIndex(alphabet, alphabet.indexOf('Z'), map))
    }

    @Test
    fun `empty index map resolves to nothing`() {
        assertNull(resolveTargetListIndex(alphabet, 0, emptyMap()))
    }

    @Test
    fun `computes the first index of every alphabetical section`() {
        val apps = listOf(
            app("Alpha"),
            app("Alarm"),
            app("Bravo"),
            app("123 Notes"),
            app("Zulu")
        )
        val map = computeAlphabetIndexMap(apps)
        assertEquals(0, map['A'])
        assertEquals(2, map['B'])
        assertEquals(3, map['#'])
        assertEquals(4, map['Z'])
        assertEquals(setOf('A', 'B', '#', 'Z'), map.keys)
    }
}
