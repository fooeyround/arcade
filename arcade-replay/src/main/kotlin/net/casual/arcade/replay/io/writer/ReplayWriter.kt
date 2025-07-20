/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.io.writer

import io.netty.buffer.ByteBuf
import net.casual.arcade.replay.api.network.RecordablePayload
import net.casual.arcade.replay.mixins.network.IdDispatchCodecAccessor
import net.casual.arcade.replay.recorder.ReplayRecorder
import net.casual.arcade.replay.util.FileUtils
import net.casual.arcade.replay.util.ReplayMarker
import net.casual.arcade.utils.ArcadeUtils
import net.minecraft.ChatFormatting
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.ProtocolInfo
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.CommonPacketTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.fileSize
import kotlin.time.Duration

public interface ReplayWriter {
    public val recorder: ReplayRecorder
    public val path: Path
    public val closed: Boolean

    public val markers: Int
        get() = 0

    public val cacheChunksOnUnload: Boolean
        get() = false

    public fun tick() {

    }

    public fun prePacketRecord(packet: Packet<*>): Boolean

    public fun writePacket(
        packet: Packet<*>,
        protocol: ProtocolInfo<*>,
        timestamp: Duration,
        offThread: Boolean
    ): CompletableFuture<Int?>

    public fun postPacketRecord(packet: Packet<*>)

    public fun writePlayer(player: ServerPlayer, packets: Collection<Packet<*>>) {
        for (packet in packets) {
            this.recorder.record(packet)
        }
    }

    public fun writeCachedChunk(pos: ChunkPos): Boolean {
        return false
    }

    public fun writeMarker(marker: ReplayMarker) {

    }

    public fun getRawRecordingSize(): Long

    public fun getOutputPath(): Path

    public fun close(duration: Duration, save: Boolean): CompletableFuture<Long>

    public companion object {
        public const val ENTRY_SERVER_REPLAY_META: String = "server_replay_meta.json"

        public val ReplayWriter.name: String
            get() = this.recorder.getName()

        public fun ReplayWriter.broadcastToOps(message: Component) {
            if (this.recorder.settings.notifyAdminsOfStatus) {
                this.recorder.server.execute {
                    this.recorder.server.playerList.players.filter {
                        this.recorder.server.playerList.isOp(it.gameProfile)
                    }.forEach { it.sendSystemMessage(message) }
                }
            }
        }

        public fun ReplayWriter.broadcastToOpsAndConsole(message: String) {
            this.broadcastToOps(Component.literal(message))
            ArcadeUtils.logger.info(message)
        }

        public fun ReplayWriter.broadcastToOpsAndConsole(message: Component) {
            this.broadcastToOps(message)
            ArcadeUtils.logger.info(message.string)
        }

        public fun encodePacket(packet: Packet<*>, protocol: ProtocolInfo<*>, buf: FriendlyByteBuf) {
            @Suppress("UNCHECKED_CAST")
            val codec = (protocol.codec() as StreamCodec<ByteBuf, Packet<*>>)

            if (packet is ClientboundCustomPayloadPacket) {
                val payload = packet.payload
                if (payload is RecordablePayload) {
                    @Suppress("UNCHECKED_CAST")
                    codec as IdDispatchCodecAccessor<PacketType<*>>

                    val id = codec.typeToIdMap.getInt(CommonPacketTypes.CLIENTBOUND_CUSTOM_PAYLOAD)
                    buf.writeVarInt(id)
                    buf.writeResourceLocation(payload.type().id)
                    payload.record(buf)
                    return
                }
            }

            codec.encode(buf, packet)
        }

        internal fun ReplayWriter.closeWithFeedback(
            save: Boolean,
            writer: () -> Unit,
            closer: () -> Unit
        ): Long {
            var size = 0L
            try {
                val additional = Component.empty()
                if (save) {
                    this.broadcastToOpsAndConsole("Starting to save replay ${this.name}, please do not stop the server!")
                    writer.invoke()
                    val output = this.getOutputPath()
                    size = output.fileSize()

                    val click = ClickEvent.SuggestCommand(this.recorder.getViewingCommand())
                    val hover = HoverEvent.ShowText(Component.literal("Click to view replay"))
                    additional.append(" and saved to ")
                        .append(Component.literal(output.toString()).withStyle {
                            it.withClickEvent(click).withHoverEvent(hover).withColor(ChatFormatting.GREEN)
                        })
                        .append(", compressed to ${FileUtils.formatSize(size)}")
                }
                try {
                    closer.invoke()
                    this.broadcastToOpsAndConsole(
                        Component.literal("Successfully closed replay ${this.name}").append(additional)
                    )
                } catch (exception: Exception) {
                    val message = "Failed to close replay writer"
                    this.broadcastToOps(Component.literal(message).append(additional))
                    ArcadeUtils.logger.error(message, exception)
                }
            } catch (exception: Exception) {
                val message = "Failed to write replay ${this.name}"
                val hover = HoverEvent.ShowText(Component.literal(exception.stackTraceToString()))
                this.broadcastToOps(Component.literal(message).withStyle {
                    it.withHoverEvent(hover)
                })
                ArcadeUtils.logger.error(message, exception)
                throw exception
            }
            return size
        }
    }
}