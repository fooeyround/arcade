/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.util.io

import net.casual.arcade.replay.util.FileUtils

public class FileSize(public val bytes: Long) {
    public fun formatted(): String {
        return FileUtils.formatSize(this.bytes)
    }

    override fun toString(): String {
        return this.formatted()
    }
}