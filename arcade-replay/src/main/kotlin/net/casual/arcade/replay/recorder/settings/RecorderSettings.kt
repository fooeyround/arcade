/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.recorder.settings

import com.google.gson.JsonElement
import com.google.gson.JsonObject
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
        val json = JsonObject()
        json.addProperty("debug", this.debug)
        json.addProperty("world_name", this.worldName)
        json.addProperty("server_name", this.serverName)
        json.addProperty("fixed_daylight_cycle", this.fixedDaylightCycle)
        json.addProperty("include_resource_packs", this.includeResourcePacks)
        json.addProperty("chunk_recorder_load_radius", this.chunkRecorderLoadRadius)
        json.addProperty("skip_when_chunks_unloaded", this.skipWhenChunksUnloaded)
        json.addProperty("max_raw_recording_file_size", this.maxRawRecordingFileSize.bytes)
        json.addProperty("restart_after_max_raw_recording_file_size", this.restartAfterMaxRawRecordingFileSize)
        json.addProperty("max_recording_duration_ms", this.maxRecordingDuration.inWholeMilliseconds)
        json.addProperty("restart_after_max_recording_duration", this.restartAfterMaxRecordingDuration)
        json.addProperty("ignore_custom_payload_packets", this.ignoreCustomPayloadPackets)
        json.addProperty("ignore_sound_packets", this.ignoreSoundPackets)
        json.addProperty("ignore_light_packets", this.ignoreLightPackets)
        json.addProperty("ignore_chat_packets", this.ignoreChatPackets)
        json.addProperty("ignore_action_bar_packets", this.ignoreActionBarPackets)
        json.addProperty("ignore_scoreboard_packets", this.ignoreScoreboardPackets)
        json.addProperty("optimize_explosion_packets", this.optimizeExplosionPackets)
        json.addProperty("optimize_entity_packets", this.optimizeEntityPackets)
        json.addProperty("record_voice_chat", this.recordVoiceChat)
        return json
    }
}