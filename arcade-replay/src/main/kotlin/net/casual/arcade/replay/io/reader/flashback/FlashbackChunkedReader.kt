/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.io.reader.flashback

import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.casual.arcade.replay.util.flashback.FlashbackAction
import net.casual.arcade.replay.util.flashback.FlashbackMeta
import net.casual.arcade.utils.JsonUtils
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.*
import kotlin.io.path.notExists
import kotlin.io.path.readBytes
import kotlin.io.path.reader

public class FlashbackChunkedReader(
    private val system: FileSystem,
    private val access: RegistryAccess
) {
    private val chunks = TreeMap<Int, PlayableChunk>()
    private var current: Map.Entry<Int, PlayableChunk>

    private val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), this.access)
    private var snapshotIndex: Int = 0
    private var playbackIndex: Int = 0

    private val actions = Int2ObjectOpenHashMap<FlashbackAction>()
    private val ignored = IntOpenHashSet()

    public val meta: FlashbackMeta = this.readMeta()

    init {
        var ticks = 0
        for ((name, meta) in this.meta.chunks) {
            this.chunks[ticks] = PlayableChunk(this.system.getPath(name), meta)
            ticks += meta.duration
        }
        this.current = this.chunks.floorEntry(0)
        this.readHeader()
    }

    public fun shouldPlaySnapshot(): Boolean {
        return this.current.value.meta.forcePlayerSnapshot
    }

    public fun consumeSnapshot(consumer: (FlashbackAction, RegistryFriendlyByteBuf) -> Unit) {
        this.buffer.readerIndex(this.snapshotIndex)
        while (this.buffer.readerIndex() < this.playbackIndex) {
            this.consumeAction(consumer)
        }
    }

    public fun consumeNextAction(consumer: (FlashbackAction, RegistryFriendlyByteBuf) -> Unit): Boolean {
        if (this.buffer.readerIndex() >= this.buffer.writerIndex()) {
            return false
        }
        if (this.buffer.readerIndex() < this.playbackIndex) {
            this.buffer.readerIndex(this.playbackIndex)
        }

        this.consumeAction(consumer)
        return true
    }

    public fun jumpTo(tick: Int, current: Int): Int? {
        val entry = this.chunks.floorEntry(tick)
        if (entry == this.current && current < tick) {
            return null
        }
        this.current = entry
        this.readHeader()
        return entry.key
    }

    public fun moveToNextChunk(): Boolean {
        val duration = this.current.value.meta.duration
        val tick = this.current.key + duration
        val entry = this.chunks.floorEntry(tick)
        if (entry == this.current) {
            return false
        }
        this.current = entry
        this.readHeader()
        return true
    }

    public fun close() {
        this.buffer.release()
        this.chunks.clear()
    }

    private fun consumeAction(consumer: (FlashbackAction, RegistryFriendlyByteBuf) -> Unit) {
        val id = this.buffer.readVarInt()
        val action = this.actions.get(id)
        if (action == null) {
            if (!this.ignored.contains(id)) {
                throw IllegalStateException("Replay tried to play unregistered action $id")
            }
            val size = this.buffer.readInt()
            this.buffer.skipBytes(size)
            return
        }

        val size = this.buffer.readInt()
        val slice = this.buffer.readSlice(size)
        consumer.invoke(action, RegistryFriendlyByteBuf(slice, this.access))

        if (slice.readerIndex() < slice.writerIndex()) {
            throw IllegalStateException("Action ${action.id} wasn't processed correctly")
        }
    }

    private fun readHeader() {
        this.actions.clear()
        this.ignored.clear()

        this.buffer.readerIndex(0)
        this.buffer.writerIndex(0)
        this.buffer.writeBytes(this.current.value.path.readBytes())

        val magic = this.buffer.readInt()
        if (magic != net.casual.arcade.replay.io.FlashbackIO.MAGIC_NUMBER) {
            throw IllegalStateException("Flashback chunk has invalid magic!")
        }

        val actions = this.buffer.readVarInt()
        for (i in 0..<actions) {
            val id = this.buffer.readResourceLocation()
            val action = FlashbackAction.from(id)
            if (action == null) {
                if (id.path.endsWith("optional")) {
                    this.ignored.add(i)
                } else {
                    throw IllegalStateException("Unknown action $id")
                }
            } else {
                this.actions.put(i, action)
            }
        }

        val snapshotSize = this.buffer.readInt()
        this.snapshotIndex = this.buffer.readerIndex()
        this.buffer.skipBytes(snapshotSize)
        this.playbackIndex = this.buffer.readerIndex()
    }

    private fun readMeta(): FlashbackMeta {
        var metadata = this.system.getPath(net.casual.arcade.replay.io.FlashbackIO.METADATA)
        if (metadata.notExists()) {
            metadata = this.system.getPath(net.casual.arcade.replay.io.FlashbackIO.METADATA_OLD)
            if (metadata.notExists()) {
                throw IllegalStateException("Flashback file has no metadata!")
            }
        }

        return metadata.reader().use {
            JsonUtils.decodeWith(FlashbackMeta.CODEC, it)
        }.orThrow
    }

    private data class PlayableChunk(
        val path: Path,
        val meta: FlashbackMeta.ChunkMeta
    )
}