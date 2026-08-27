package com.viralclip.app.util

import android.content.Context
import android.net.Uri
import com.viralclip.app.domain.model.PlatformPreset
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class ExtensionsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
    }

    // ─── Duration Formatting ─────────────────────────────────────

    @Test
    fun `formatDuration formats seconds correctly`() {
        assertEquals("0:00", 0L.formatDuration())
        assertEquals("0:01", 1000L.formatDuration())
        assertEquals("0:30", 30000L.formatDuration())
        assertEquals("1:00", 60000L.formatDuration())
        assertEquals("1:30", 90000L.formatDuration())
    }

    @Test
    fun `formatDuration formats minutes correctly`() {
        assertEquals("2:00", 120000L.formatDuration())
        assertEquals("5:30", 330000L.formatDuration())
        assertEquals("10:00", 600000L.formatDuration())
    }

    @Test
    fun `formatDuration formats hours correctly`() {
        assertEquals("1:00:00", 3600000L.formatDuration())
        assertEquals("1:30:00", 5400000L.formatDuration())
        assertEquals("2:15:30", 8130000L.formatDuration())
    }

    @Test
    fun `formatDurationShort formats correctly`() {
        assertEquals("0:00", 0L.formatDurationShort())
        assertEquals("1:00", 60000L.formatDurationShort())
        assertEquals("10:30", 630000L.formatDurationShort())
    }

    @Test
    fun `formatDurationWithMs formats correctly`() {
        val result = 61500L.formatDurationWithMs()
        assertEquals("1:01.50", result)
    }

    @Test
    fun `formatDurationWithMs zero milliseconds`() {
        val result = 60000L.formatDurationWithMs()
        assertEquals("1:00.00", result)
    }

    @Test
    fun `formatSrtTimestamp formats correctly`() {
        val result = 3723456L.formatSrtTimestamp()
        assertEquals("01:02:03,456", result)
    }

    @Test
    fun `formatSrtTimestamp zero time`() {
        val result = 0L.formatSrtTimestamp()
        assertEquals("00:00:00,000", result)
    }

    @Test
    fun `formatVttTimestamp formats correctly`() {
        val result = 3723456L.formatVttTimestamp()
        assertEquals("01:02:03.456", result)
    }

    @Test
    fun `formatVttTimestamp zero time`() {
        val result = 0L.formatVttTimestamp()
        assertEquals("00:00:00.000", result)
    }

    // ─── Seconds Conversion ──────────────────────────────────────

    @Test
    fun `secondsToTimestamp formats correctly`() {
        assertEquals("0:00", 0.secondsToTimestamp())
        assertEquals("1:30", 90.secondsToTimestamp())
        assertEquals("1:02:03", 3723.secondsToTimestamp())
    }

    @Test
    fun `secondsToDurationLabel formats hours and minutes`() {
        val result = 3720.secondsToDurationLabel()
        assertEquals("1h 2m", result)
    }

    @Test
    fun `secondsToDurationLabel formats hours only`() {
        val result = 7200.secondsToDurationLabel()
        assertEquals("2h", result)
    }

    @Test
    fun `secondsToDurationLabel formats minutes and seconds`() {
        val result = 90.secondsToDurationLabel()
        assertEquals("1m 30s", result)
    }

    @Test
    fun `secondsToDurationLabel formats minutes only`() {
        val result = 120.secondsToDurationLabel()
        assertEquals("2m", result)
    }

    @Test
    fun `secondsToDurationLabel formats seconds only`() {
        val result = 45.secondsToDurationLabel()
        assertEquals("45s", result)
    }

    @Test
    fun `secondsToDurationLabel zero seconds`() {
        val result = 0.secondsToDurationLabel()
        assertEquals("0s", result)
    }

    // ─── Percentage Formatting ───────────────────────────────────

    @Test
    fun `toPercentageString formats float to percentage string`() {
        assertEquals("0%", 0f.toPercentageString())
        assertEquals("50%", 0.5f.toPercentageString())
        assertEquals("75%", 0.756f.toPercentageString())
        assertEquals("100%", 1f.toPercentageString())
    }

    @Test
    fun `toPercentageString handles negative values`() {
        assertEquals("-50%", (-0.5f).toPercentageString())
    }

    @Test
    fun `toPercentageString handles values above 1`() {
        assertEquals("150%", 1.5f.toPercentageString())
    }

    // ─── Score Color ───────────────────────────────────────────

    @Test
    fun `toScoreColor returns green for high scores`() {
        val green = 0.75f.toScoreColor()
        assertEquals(0xFF10B981.toLong(), green)
    }

    @Test
    fun `toScoreColor returns yellow for medium scores`() {
        val yellow = 0.5f.toScoreColor()
        assertEquals(0xFFF59E0B.toLong(), yellow)
    }

    @Test
    fun `toScoreColor returns red for low scores`() {
        val red = 0.25f.toScoreColor()
        assertEquals(0xFFEF4444.toLong(), red)
    }

    @Test
    fun `toScoreColor at boundary values`() {
        assertEquals(0xFF10B981.toLong(), 0.7f.toScoreColor())
        assertEquals(0xFFF59E0B.toLong(), 0.69f.toScoreColor())
        assertEquals(0xFFF59E0B.toLong(), 0.4f.toScoreColor())
        assertEquals(0xFFEF4444.toLong(), 0.39f.toScoreColor())
    }

    @Test
    fun `toScoreColor returns green at 1_0`() {
        assertEquals(0xFF10B981.toLong(), 1.0f.toScoreColor())
    }

    @Test
    fun `toScoreColor returns red at 0_0`() {
        assertEquals(0xFFEF4444.toLong(), 0.0f.toScoreColor())
    }

    // ─── URI Conversion ─────────────────────────────────────────

    @Test
    fun `toUri parses string to Uri`() {
        val uri = "content://test/123".toUri()
        assertEquals("content://test/123", uri.toString())
    }

    @Test
    fun `toUri handles empty string`() {
        val uri = "".toUri()
        assertEquals("", uri.toString())
    }

    @Test
    fun `toUri handles http URL`() {
        val uri = "https://example.com/video.mp4".toUri()
        assertEquals("https://example.com/video.mp4", uri.toString())
    }

    // ─── File Size Formatting ───────────────────────────────────

    @Test
    fun `formatFileSize formats bytes`() {
        assertEquals("0 B", 0L.formatFileSize())
        assertEquals("500 B", 500L.formatFileSize())
        assertEquals("1023 B", 1023L.formatFileSize())
    }

    @Test
    fun `formatFileSize formats kilobytes`() {
        assertEquals("1.0 KB", 1024L.formatFileSize())
        assertEquals("10.0 KB", 10240L.formatFileSize())
        assertEquals("512.5 KB", 524800L.formatFileSize())
    }

    @Test
    fun `formatFileSize formats megabytes`() {
        assertEquals("1.0 MB", (1024 * 1024).toLong().formatFileSize())
        assertEquals("50.5 MB", (50 * 1024 * 1024 + 512 * 1024).toLong().formatFileSize())
    }

    @Test
    fun `formatFileSize formats gigabytes`() {
        assertEquals("1.00 GB", (1024L * 1024 * 1024).formatFileSize())
        assertEquals("2.50 GB", (2.5 * 1024 * 1024 * 1024).toLong().formatFileSize())
    }

    @Test
    fun `formatBitrate formats low bitrates`() {
        assertEquals("500 bps", 500L.formatBitrate())
    }

    @Test
    fun `formatBitrate formats kilobit rates`() {
        val result = 128000L.formatBitrate()
        assertTrue(result.contains("Kbps"))
    }

    @Test
    fun `formatBitrate formats megabit rates`() {
        val result = 8_000_000L.formatBitrate()
        assertTrue(result.contains("Mbps"))
    }

    // ─── Date Formatting ─────────────────────────────────────────

    @Test
    fun `formatDate returns formatted date string`() {
        val timestamp = 1609459200000L // 2021-01-01
        val result = timestamp.formatDate()
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("2021"))
    }

    @Test
    fun `formatDate with custom pattern`() {
        val timestamp = 1609459200000L
        val result = timestamp.formatDate("yyyy-MM-dd")
        assertTrue(result.contains("2021"))
    }

    @Test
    fun `formatDateTime returns formatted datetime string`() {
        val timestamp = 1609459200000L
        val result = timestamp.formatDateTime()
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatRelativeDate returns just now for very recent`() {
        val now = System.currentTimeMillis()
        val result = now.formatRelativeDate()
        assertEquals("Just now", result)
    }

    @Test
    fun `formatRelativeDate returns minutes ago`() {
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        val result = fiveMinutesAgo.formatRelativeDate()
        assertEquals("5m ago", result)
    }

    @Test
    fun `formatRelativeDate returns hours ago`() {
        val threeHoursAgo = System.currentTimeMillis() - (3 * 60 * 60 * 1000)
        val result = threeHoursAgo.formatRelativeDate()
        assertEquals("3h ago", result)
    }

    @Test
    fun `formatRelativeDate returns days ago`() {
        val twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000)
        val result = twoDaysAgo.formatRelativeDate()
        assertEquals("2d ago", result)
    }

    @Test
    fun `formatRelativeDate returns weeks ago`() {
        val twoWeeksAgo = System.currentTimeMillis() - (15 * 24 * 60 * 60 * 1000)
        val result = twoWeeksAgo.formatRelativeDate()
        assertTrue(result.contains("w ago"))
    }

    @Test
    fun `formatRelativeDate returns months ago`() {
        val twoMonthsAgo = System.currentTimeMillis() - (60 * 24 * 60 * 60 * 1000)
        val result = twoMonthsAgo.formatRelativeDate()
        assertTrue(result.contains("mo ago"))
    }

    @Test
    fun `formatProcessingTime formats seconds`() {
        val result = 5000L.formatProcessingTime()
        assertEquals("5s", result)
    }

    @Test
    fun `formatProcessingTime formats minutes and seconds`() {
        val result = 125000L.formatProcessingTime()
        assertEquals("2m 5s", result)
    }

    @Test
    fun `formatProcessingTime formats hours minutes seconds`() {
        val result = 3725000L.formatProcessingTime()
        assertEquals("1h 2m 5s", result)
    }

    @Test
    fun `formatSmartDate returns today for current timestamp`() {
        val now = System.currentTimeMillis()
        val result = now.formatSmartDate()
        assertTrue(result.startsWith("Today"))
    }

    @Test
    fun `isToday returns true for current time`() {
        val now = System.currentTimeMillis()
        assertTrue(now.isToday())
    }

    @Test
    fun `isToday returns false for old timestamp`() {
        val oldTimestamp = 1609459200000L
        assertFalse(oldTimestamp.isToday())
    }

    @Test
    fun `isYesterday returns false for current time`() {
        val now = System.currentTimeMillis()
        assertFalse(now.isYesterday())
    }

    // ─── Platform Preset ────────────────────────────────────────

    @Test
    fun `maxDurationFormatted formats minutes and seconds`() {
        assertEquals("3m 0s", PlatformPreset.TIKTOK.maxDurationFormatted())
        assertEquals("1m 30s", PlatformPreset.YOUTUBE_SHORTS.maxDurationFormatted())
        assertEquals("4m 0s", PlatformPreset.INSTAGRAM_REELS.maxDurationFormatted())
    }

    @Test
    fun `maxDurationFormatted formats seconds only`() {
        assertEquals("140s", PlatformPreset.TWITTER.maxDurationFormatted())
    }

    // ─── Directory Functions ────────────────────────────────────

    @Test
    fun `getOutputDirectory creates directory`() {
        val mockDir = mockk<File>(relaxed = true)
        every { context.getExternalFilesDir(any()) } returns mockDir
        every { mockDir.exists() } returns false
        every { mockDir.mkdirs() } returns true

        val result = context.getOutputDirectory("TestDir")

        assertNotNull(result)
        verify { context.getExternalFilesDir("TestDir") }
    }

    @Test
    fun `getTempDirectory uses cache dir`() {
        val mockDir = mockk<File>(relaxed = true)
        every { context.cacheDir } returns mockDir
        every { mockDir.exists() } returns false
        every { mockDir.mkdirs() } returns true

        val result = context.getTempDirectory()

        assertNotNull(result)
        verify { mockDir.mkdirs() }
    }

    @Test
    fun `getExportDirectory uses output directory`() {
        val mockDir = mockk<File>(relaxed = true)
        every { context.getExternalFilesDir(any()) } returns mockDir
        every { mockDir.exists() } returns true

        val result = context.getExportDirectory()

        assertNotNull(result)
    }

    @Test
    fun `getThumbnailDirectory uses output directory`() {
        val mockDir = mockk<File>(relaxed = true)
        every { context.getExternalFilesDir(any()) } returns mockDir
        every { mockDir.exists() } returns true

        val result = context.getThumbnailDirectory()

        assertNotNull(result)
    }

    @Test
    fun `getCacheDirectory uses cache dir`() {
        val mockDir = mockk<File>(relaxed = true)
        every { context.cacheDir } returns mockDir
        every { mockDir.exists() } returns false
        every { mockDir.mkdirs() } returns true

        val result = context.getCacheDirectory()

        assertNotNull(result)
        verify { context.cacheDir }
    }

    @Test
    fun `getCacheDirectory with custom subdirectory`() {
        val mockDir = mockk<File>(relaxed = true)
        every { context.cacheDir } returns mockDir
        every { mockDir.exists() } returns false
        every { mockDir.mkdirs() } returns true

        val result = context.getCacheDirectory("custom_subdir")

        assertNotNull(result)
    }

    // ─── Content Resolver ───────────────────────────────────────

    @Test
    fun `getFileName returns video for empty cursor`() {
        val uri = mockk<Uri>(relaxed = true)
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every {
            context.contentResolver.query(eq(uri), any(), any(), any(), any())
        } returns mockCursor
        every { mockCursor.moveToFirst() } returns false
        every { mockCursor.close() } returns Unit

        val result = context.getFileName(uri)

        assertEquals("video", result)
    }

    @Test
    fun `getFileSize returns 0 for empty cursor`() {
        val uri = mockk<Uri>(relaxed = true)
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every {
            context.contentResolver.query(eq(uri), any(), any(), any(), any())
        } returns mockCursor
        every { mockCursor.moveToFirst() } returns false
        every { mockCursor.close() } returns Unit

        val result = context.getFileSize(uri)

        assertEquals(0L, result)
    }

    @Test
    fun `getFileName handles null cursor`() {
        val uri = mockk<Uri>(relaxed = true)
        every {
            context.contentResolver.query(eq(uri), any(), any(), any(), any())
        } returns null

        val result = context.getFileName(uri)

        assertEquals("video", result)
    }

    @Test
    fun `getMimeType returns default for null`() {
        val uri = mockk<Uri>(relaxed = true)
        every { context.contentResolver.getType(uri) } returns null

        val result = context.getMimeType(uri)

        assertEquals("video/*", result)
    }

    @Test
    fun `getMimeType returns actual mime type`() {
        val uri = mockk<Uri>(relaxed = true)
        every { context.contentResolver.getType(uri) } returns "video/mp4"

        val result = context.getMimeType(uri)

        assertEquals("video/mp4", result)
    }

    // ─── Clamp Extensions ───────────────────────────────────────

    @Test
    fun `Float clamp restricts to range`() {
        assertEquals(0.5f, 0.5f.clamp(0f, 1f), 0.001f)
        assertEquals(0f, (-0.5f).clamp(0f, 1f), 0.001f)
        assertEquals(1f, 1.5f.clamp(0f, 1f), 0.001f)
    }

    @Test
    fun `Int clamp restricts to range`() {
        assertEquals(5, 5.clamp(0, 10))
        assertEquals(0, (-3).clamp(0, 10))
        assertEquals(10, 15.clamp(0, 10))
    }

    @Test
    fun `Long clamp restricts to range`() {
        assertEquals(5L, 5L.clamp(0L, 10L))
        assertEquals(0L, (-3L).clamp(0L, 10L))
        assertEquals(10L, 15L.clamp(0L, 10L))
    }

    // ─── List Extensions ────────────────────────────────────────

    @Test
    fun `secondOrNull returns second element`() {
        assertEquals(2, listOf(1, 2, 3).secondOrNull())
    }

    @Test
    fun `secondOrNull returns null for single element`() {
        assertNull(listOf(1).secondOrNull())
    }

    @Test
    fun `secondOrNull returns null for empty list`() {
        assertNull(emptyList<Int>().secondOrNull())
    }

    @Test
    fun `lastOrNull with predicate finds last match`() {
        val list = listOf(1, 2, 3, 4, 5)
        assertEquals(4, list.lastOrNull { it % 2 == 0 })
    }

    @Test
    fun `lastOrNull with predicate returns null when no match`() {
        val list = listOf(1, 3, 5)
        assertNull(list.lastOrNull { it % 2 == 0 })
    }

    @Test
    fun `lastOrNull on empty list returns null`() {
        assertNull(emptyList<Int>().lastOrNull { true })
    }

    @Test
    fun `takeRandom returns requested count`() {
        val list = listOf(1, 2, 3, 4, 5)
        val result = list.takeRandom(3)
        assertEquals(3, result.size)
        assertTrue(result.all { it in list })
    }

    @Test
    fun `takeRandom with count greater than list size`() {
        val list = listOf(1, 2)
        val result = list.takeRandom(5)
        assertTrue(result.size <= 2)
    }

    @Test
    fun `takeRandom with empty list`() {
        val result = emptyList<Int>().takeRandom(3)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `groupByCount counts elements correctly`() {
        val list = listOf("a", "b", "a", "c", "b", "a")
        val result = list.groupByCount()
        assertEquals(3, result["a"])
        assertEquals(2, result["b"])
        assertEquals(1, result["c"])
    }

    @Test
    fun `groupByCount on empty list`() {
        val result = emptyList<String>().groupByCount()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `mostCommon returns most frequent element`() {
        val list = listOf("a", "b", "a", "c", "a")
        assertEquals("a", list.mostCommon())
    }

    @Test
    fun `mostCommon on empty list`() {
        assertNull(emptyList<String>().mostCommon())
    }

    @Test
    fun `leastCommon returns least frequent element`() {
        val list = listOf("a", "a", "a", "b")
        assertEquals("b", list.leastCommon())
    }

    @Test
    fun `leastCommon on empty list`() {
        assertNull(emptyList<String>().leastCommon())
    }

    @Test
    fun `mapIndexedSafe executes action for each element`() {
        val list = listOf("a", "b", "c")
        val visited = mutableListOf<Int>()
        list.mapIndexedSafe { index, _ -> visited.add(index) }
        assertEquals(listOf(0, 1, 2), visited)
    }

    @Test
    fun `mapIndexedSafe returns same list`() {
        val list = listOf(1, 2, 3)
        val result = list.mapIndexedSafe { _, _ -> }
        assertEquals(list, result)
    }

    // ─── String Extensions ──────────────────────────────────────

    @Test
    fun `capitalizeWords capitalizes each word`() {
        assertEquals("Hello World", "hello world".capitalizeWords())
        assertEquals("Test Case One", "test case one".capitalizeWords())
    }

    @Test
    fun `capitalizeWords handles single word`() {
        assertEquals("Hello", "hello".capitalizeWords())
    }

    @Test
    fun `capitalizeWords handles empty string`() {
        assertEquals("", "".capitalizeWords())
    }

    @Test
    fun `capitalizeWords handles already capitalized`() {
        assertEquals("Hello World", "HELLO WORLD".capitalizeWords())
    }

    @Test
    fun `removeWhitespace removes all whitespace`() {
        assertEquals("helloworld", "hello world".removeWhitespace())
        assertEquals("abc", "a b c".removeWhitespace())
        assertEquals("test", "  test  ".removeWhitespace())
    }

    @Test
    fun `normalizeWhitespace collapses multiple spaces`() {
        assertEquals("hello world", "hello   world".normalizeWhitespace())
        assertEquals("a b c", "  a   b   c  ".normalizeWhitespace())
    }

    @Test
    fun `normalizeWhitespace trims edges`() {
        assertEquals("hello", "  hello  ".normalizeWhitespace())
    }

    @Test
    fun `extractNumbers extracts integers`() {
        val result = "abc123def456".extractNumbers()
        assertEquals(listOf(123, 456), result)
    }

    @Test
    fun `extractNumbers returns empty for no numbers`() {
        val result = "hello".extractNumbers()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractDecimals extracts decimal numbers`() {
        val result = "temp is 36.5 and 20.0".extractDecimals()
        assertEquals(listOf(36.5f, 20.0f), result)
    }

    @Test
    fun `extractDecimals handles integers`() {
        val result = "value 42".extractDecimals()
        assertEquals(listOf(42f), result)
    }

    @Test
    fun `isValidUrl returns true for http URLs`() {
        assertTrue("https://example.com".isValidUrl())
        assertTrue("http://test.org".isValidUrl())
    }

    @Test
    fun `isValidUrl returns false for non URLs`() {
        assertFalse("ftp://files.com".isValidUrl())
        assertFalse("hello world".isValidUrl())
        assertFalse("".isValidUrl())
    }

    @Test
    fun `isValidEmail returns true for valid emails`() {
        assertTrue("user@example.com".isValidEmail())
        assertTrue("test.name+tag@domain.co".isValidEmail())
    }

    @Test
    fun `isValidEmail returns false for invalid emails`() {
        assertFalse("not-an-email".isValidEmail())
        assertFalse("@missing-local.com".isValidEmail())
        assertFalse("missing-at-sign.com".isValidEmail())
    }

    @Test
    fun `truncate shortens long strings`() {
        val result = "Hello World".truncate(8)
        assertEquals("Hello...", result)
    }

    @Test
    fun `truncate does not shorten short strings`() {
        val result = "Hi".truncate(10)
        assertEquals("Hi", result)
    }

    @Test
    fun `truncate with custom suffix`() {
        val result = "Hello World".truncate(8, "---")
        assertEquals("Hello---", result)
    }

    @Test
    fun `truncate with exact length`() {
        val result = "Hello".truncate(5)
        assertEquals("Hello", result)
    }

    @Test
    fun `repeat string N times`() {
        assertEquals("aaa", "a".repeat(3))
        assertEquals("abab", "ab".repeat(2))
    }

    @Test
    fun `repeat with separator`() {
        assertEquals("a,a,a", "a".repeat(3, ","))
    }

    @Test
    fun `repeat zero times`() {
        assertEquals("", "hello".repeat(0))
    }

    // ─── Map Extensions ─────────────────────────────────────────

    @Test
    fun `merge maps combines entries`() {
        val map1 = mapOf("a" to 1, "b" to 2)
        val map2 = mapOf("c" to 3)
        val result = map1.merge(map2)
        assertEquals(3, result.size)
        assertEquals(1, result["a"])
        assertEquals(3, result["c"])
    }

    @Test
    fun `merge maps overwrites duplicate keys`() {
        val map1 = mapOf("a" to 1)
        val map2 = mapOf("a" to 2)
        val result = map1.merge(map2)
        assertEquals(2, result["a"])
    }

    // ─── Hex / Byte Extensions ─────────────────────────────────

    @Test
    fun `toHexString converts bytes to hex`() {
        val bytes = byteArrayOf(0x0A, 0xFF, 0x01)
        assertEquals("0aff01", bytes.toHexString())
    }

    @Test
    fun `toHexString empty array`() {
        assertEquals("", byteArrayOf().toHexString())
    }

    @Test
    fun `hexToByteArray converts hex to bytes`() {
        val result = "0aff01".hexToByteArray()
        assertEquals(3, result.size)
        assertEquals(0x0A.toByte(), result[0])
        assertEquals(0xFF.toByte(), result[1])
        assertEquals(0x01.toByte(), result[2])
    }

    @Test
    fun `hexToByteArray empty string`() {
        assertEquals(0, "".hexToByteArray().size)
    }

    @Test
    fun `hex roundtrip preserves data`() {
        val original = byteArrayOf(0x00, 0x7F, 0x80, 0xFF.toByte())
        val hex = original.toHexString()
        val restored = hex.hexToByteArray()
        assertArrayEquals(original, restored)
    }

    // ─── dB Conversions ─────────────────────────────────────────

    @Test
    fun `toDecibel converts amplitude to decibels`() {
        val db = 1.0f.toDecibel()
        assertEquals(0f, db, 0.01f)
    }

    @Test
    fun `toDecibel handles small amplitude`() {
        val db = 0.01f.toDecibel()
        assertTrue("Small amplitude should give negative dB", db < 0f)
    }

    @Test
    fun `fromDecibel converts decibels to amplitude`() {
        val amp = 0f.fromDecibel()
        assertEquals(1.0f, amp, 0.01f)
    }

    @Test
    fun `fromDecibel handles negative dB`() {
        val amp = (-20f).fromDecibel()
        assertEquals(0.1f, amp, 0.01f)
    }

    @Test
    fun `normalizeToDb normalizes to 0-1 range`() {
        val result = 0f.normalizeToDb()
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `normalizeToDb at max`() {
        val result = 0f.normalizeToDb()
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `denormalizeFromDb reverses normalizeToDb`() {
        val original = -30f
        val normalized = original.normalizeToDb()
        val denormalized = normalized.denormalizeFromDb()
        assertEquals(original, denormalized, 0.1f)
    }

    @Test
    fun `normalizeToDb and denormalizeFromDb roundtrip`() {
        val values = listOf(-60f, -40f, -20f, -10f, 0f)
        for (value in values) {
            val normalized = value.normalizeToDb()
            val restored = normalized.denormalizeFromDb()
            assertEquals(value, restored, 0.5f)
        }
    }

    // ─── Boolean / Int Conversions ──────────────────────────────

    @Test
    fun `Boolean toInt converts true to 1`() {
        assertEquals(1, true.toInt())
    }

    @Test
    fun `Boolean toInt converts false to 0`() {
        assertEquals(0, false.toInt())
    }

    @Test
    fun `Int toBoolean converts non-zero to true`() {
        assertTrue(1.toBoolean())
        assertTrue(42.toBoolean())
        assertTrue((-1).toBoolean())
    }

    @Test
    fun `Int toBoolean converts zero to false`() {
        assertFalse(0.toBoolean())
    }

    // ─── Timestamp Formatting ───────────────────────────────────

    @Test
    fun `formatTimestamp formats minutes and seconds`() {
        val result = 65010L.formatTimestamp()
        assertEquals("1:05.01", result)
    }

    @Test
    fun `formatTimestamp formats hours minutes seconds`() {
        val result = 3665050L.formatTimestamp()
        assertEquals("1:01:05.05", result)
    }

    @Test
    fun `formatTimestamp zero time`() {
        val result = 0L.formatTimestamp()
        assertEquals("0:00.00", result)
    }

    // ─── Collection Extensions ──────────────────────────────────

    @Test
    fun `mostCommon returns first max on tie`() {
        val list = listOf("a", "b")
        val result = list.mostCommon()
        assertTrue(result == "a" || result == "b")
    }

    @Test
    fun `leastCommon returns first min on tie`() {
        val list = listOf("a", "b")
        val result = list.leastCommon()
        assertTrue(result == "a" || result == "b")
    }

    @Test
    fun `mapIndexedSafe with empty list`() {
        val result = emptyList<String>().mapIndexedSafe { _, _ -> }
        assertTrue(result.isEmpty())
    }

    // ─── TimeFormatter Utilities ────────────────────────────────

    @Test
    fun `estimateRemainingTime returns zero at full progress`() {
        val result = TimeFormatter.estimateRemainingTime(1.0f, 5000L)
        assertEquals(0L, result)
    }

    @Test
    fun `estimateRemainingTime returns zero at zero progress`() {
        val result = TimeFormatter.estimateRemainingTime(0f, 5000L)
        assertEquals(0L, result)
    }

    @Test
    fun `estimateRemainingTime estimates correctly at half progress`() {
        val result = TimeFormatter.estimateRemainingTime(0.5f, 5000L)
        assertEquals(5000L, result)
    }

    @Test
    fun `formatTimestampFromSeconds converts correctly`() {
        val result = TimeFormatter.formatTimestampFromSeconds(1, 30, 45)
        assertEquals(
            3600000L + 30 * 60000L + 45 * 1000L,
            result
        )
    }
}
