/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.util.flashback

import com.mojang.serialization.Codec
import net.casual.arcade.utils.codec.OrderedRecordCodecBuilder
import net.minecraft.SharedConstants
import net.minecraft.core.UUIDUtil
import java.util.*

public data class FlashbackMeta(
    val uuid: UUID = UUID.randomUUID(),
    val name: String = "Unnamed",
    val version: String = SharedConstants.getCurrentVersion().name(),
    val worldName: String = "World",
    val dataVersion: Int = SharedConstants.getCurrentVersion().dataVersion().version,
    val protocolVersion: Int = SharedConstants.getCurrentVersion().protocolVersion(),
    val totalTicks: Int = 0,
    val markers: Map<String, FlashbackMarker> = mapOf(),
    val chunks: Map<String, ChunkMeta> = mapOf()
) {
    public fun completeChunk(totalTicks: Int, chunkName: String): FlashbackMeta {
        val duration = totalTicks - this.totalTicks
        val copy = LinkedHashMap(this.chunks)
        copy[chunkName] = ChunkMeta(duration)
        return this.copy(totalTicks = totalTicks, chunks = copy)
    }

    public data class ChunkMeta(
        val duration: Int,
        /**
         * This property is only ever used when merging two
         * replays, and for our purposes will always be `false`
         */
        val forcePlayerSnapshot: Boolean = false
    ) {
        public companion object {
            public val CODEC: Codec<ChunkMeta> = OrderedRecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.INT.fieldOf("duration").forGetter(ChunkMeta::duration),
                    Codec.BOOL.fieldOf("forcePlayerSnapshot").forGetter(ChunkMeta::forcePlayerSnapshot)
                ).apply(instance, ::ChunkMeta)
            }
        }
    }

    public companion object {
        public val CODEC: Codec<FlashbackMeta> = OrderedRecordCodecBuilder.create { instance ->
            instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(FlashbackMeta::uuid),
                Codec.STRING.fieldOf("name").forGetter(FlashbackMeta::name),
                Codec.STRING.fieldOf("version").forGetter(FlashbackMeta::version),
                Codec.STRING.fieldOf("world_name").forGetter(FlashbackMeta::worldName),
                Codec.INT.fieldOf("data_version").forGetter(FlashbackMeta::dataVersion),
                Codec.INT.fieldOf("protocol_version").forGetter(FlashbackMeta::protocolVersion),
                Codec.INT.fieldOf("total_ticks").forGetter(FlashbackMeta::totalTicks),
                Codec.unboundedMap(Codec.STRING, FlashbackMarker.CODEC).fieldOf("markers").forGetter(FlashbackMeta::markers),
                Codec.unboundedMap(Codec.STRING, ChunkMeta.CODEC).fieldOf("chunks").forGetter(FlashbackMeta::chunks)
            ).apply(instance, ::FlashbackMeta)
        }
    }
}