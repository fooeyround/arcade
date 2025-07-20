/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.recorder.settings

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.casual.arcade.replay.util.io.FileSize
import kotlin.time.Duration

public interface RecorderSettings {
    public val debug: Boolean

    public val worldName: String
    public val serverName: String
    public val fixedDaylightCycle: Long
    public val includeResourcePacks: Boolean
    public val chunkRecorderLoadRadius: Int
    public val skipWhenChunksUnloaded: Boolean
    public val notifyAdminsOfStatus: Boolean

    public val maxRawRecordingFileSize: FileSize
    public val restartAfterMaxRawRecordingFileSize: Boolean
    public val maxRecordingDuration: Duration
    public val restartAfterMaxRecordingDuration: Boolean

    public val ignoreCustomPayloadPackets: Boolean
    public val ignoreSoundPackets: Boolean
    public val ignoreLightPackets: Boolean
    public val ignoreChatPackets: Boolean
    public val ignoreActionBarPackets: Boolean
    public val ignoreScoreboardPackets: Boolean

    public val optimizeExplosionPackets: Boolean
    public val optimizeEntityPackets: Boolean

    public val recordVoiceChat: Boolean

    public fun asJson(): JsonElement {
        return JsonPrimitive(this.toString())
    }
}