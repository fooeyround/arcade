/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.recorder.settings

import net.casual.arcade.replay.io.ReplayFormat
import net.casual.arcade.replay.util.io.FileSize
import kotlin.time.Duration

public data class SimpleRecorderSettings(
    override val debug: Boolean = false,
    override val format: ReplayFormat = ReplayFormat.ReplayMod,
    override val worldName: String = "World",
    override val serverName: String = "Server",
    override val fixedDaylightCycle: Long = -1,
    override val includeResourcePacks: Boolean = true,
    override val chunkRecorderLoadRadius: Int = -1,
    override val skipWhenChunksUnloaded: Boolean = false,
    override val notifyAdminsOfStatus: Boolean = true,
    override val maxRawRecordingFileSize: FileSize = FileSize(0),
    override val restartAfterMaxRawRecordingFileSize: Boolean = false,
    override val maxRecordingDuration: Duration = Duration.ZERO,
    override val restartAfterMaxRecordingDuration: Boolean = false,
    override val ignoreCustomPayloadPackets: Boolean = false,
    override val ignoreSoundPackets: Boolean = false,
    override val ignoreLightPackets: Boolean = false,
    override val ignoreChatPackets: Boolean = false,
    override val ignoreActionBarPackets: Boolean = false,
    override val ignoreScoreboardPackets: Boolean = false,
    override val optimizeExplosionPackets: Boolean = true,
    override val optimizeEntityPackets: Boolean = false,
    override val recordVoiceChat: Boolean = false,
): RecorderSettings {
    public companion object {
        public val DEFAULT: SimpleRecorderSettings = SimpleRecorderSettings()
    }
}
