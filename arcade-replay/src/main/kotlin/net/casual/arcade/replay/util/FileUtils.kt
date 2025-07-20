/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.util

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.text.NumberFormat
import java.text.ParseException
import java.text.StringCharacterIterator
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Stream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*
import kotlin.math.abs

public object FileUtils {
    private const val KB = 1024L
    private const val MB = 1048576L
    private const val GB = 1073741824L
    private const val TB = 1099511627776L
    private val VALUE_PATTERN = Pattern.compile("([0-9]+([.,][0-9]+)?)\\s*(|K|M|G|T)B?", 2)

    public fun parseSize(string: String): Long? {
        val matcher = VALUE_PATTERN.matcher(string)
        if (matcher.matches()) {
            try {
                val amount = matcher.group(1)
                val quantity = NumberFormat.getNumberInstance(Locale.ROOT).parse(amount).toDouble()
                val unit = matcher.group(3)
                if (unit == null || unit.isEmpty()) {
                    return quantity.toLong()
                } else if (unit.equals("K", ignoreCase = true)) {
                    return (quantity * KB).toLong()
                } else if (unit.equals("M", ignoreCase = true)) {
                    return (quantity * MB).toLong()
                } else if (unit.equals("G", ignoreCase = true)) {
                    return (quantity * GB).toLong()
                } else if (unit.equals("T", ignoreCase = true)) {
                    return (quantity * TB).toLong()
                }
            } catch (_: ParseException) {

            }
        }
        return null
    }

    public fun formatSize(bytes: Long): String {
        val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes.toDouble()).toLong()
        if (absB < KB) {
            return "$bytes B"
        }
        var value = absB
        val ci = StringCharacterIterator("KMGT")
        var i = 40
        while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
            value = value shr 10
            ci.next()
            i -= 10
        }
        value *= java.lang.Long.signum(bytes).toLong()
        return String.format("%.1f %ciB", value / KB.toDouble(), ci.current())
    }

    public fun findNextAvailable(
        original: Path,
        limit: Int = 100
    ): Path {
        if (original.notExists()) {
            return original
        }
        val parent = original.parent
        for (i in 1..limit) {
            val next = parent.resolve("${original.name} (${i})")
            if (next.notExists()) {
                return next
            }
        }
        throw IllegalStateException("Cannot find next available path for ${original.absolutePathString()}")
    }

    public fun zip(source: Path, file: Path) {
        ZipOutputStream(file.outputStream()).use { out ->
            out.setLevel(Deflater.BEST_SPEED)
            for (path in source.walk()) {
                val entry = ZipEntry(source.relativize(path).toString())
                out.putNextEntry(entry)
                Files.copy(path, out)
                out.closeEntry()
            }
        }
    }

    public fun Path.streamDirectoryEntriesOrEmpty(): Stream<Path> {
        if (this.notExists() || !this.isDirectory()) {
            return Stream.empty()
        }
        return try {
            Files.list(this)
        } catch (e: IOException) {
            Stream.empty()
        }
    }
}