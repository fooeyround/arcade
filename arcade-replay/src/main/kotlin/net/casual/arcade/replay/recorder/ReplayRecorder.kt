/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.recorder

import com.google.gson.JsonObject
import com.mojang.authlib.GameProfile
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.replay.api.network.RecordablePayload
import net.casual.arcade.replay.recorder.chunk.ReplayChunkRecorder
import net.casual.arcade.replay.recorder.player.ReplayPlayerRecorder
import net.casual.arcade.utils.DateTimeUtils.formatHHMMSS
import net.casual.arcade.utils.getDebugName
import net.casual.arcade.replay.ArcadeReplay
import net.casual.arcade.replay.events.ReplayRecorderStartEvent
import net.casual.arcade.replay.events.ReplayRecorderStopEvent
import net.casual.arcade.replay.io.writer.ReplayWriter
import net.casual.arcade.replay.io.writer.ReplayWriter.Companion.broadcastToOpsAndConsole
import net.casual.arcade.replay.util.DebugPacketData
import net.casual.arcade.replay.util.FileUtils
import net.casual.arcade.replay.util.ReplayMarker
import net.casual.arcade.replay.util.ReplayOptimizerUtils
import net.casual.arcade.replay.recorder.settings.RecorderSettings
import net.casual.arcade.utils.ArcadeUtils
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.ProtocolInfo
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.configuration.ConfigurationProtocols
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.GameProtocols
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket
import net.minecraft.network.protocol.login.LoginProtocols
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.builder.StandardToStringStyle
import org.apache.commons.lang3.builder.ToStringBuilder
import org.jetbrains.annotations.ApiStatus.Internal
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.pathString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * This is the abstract class representing a replay recorder.
 *
 * This class is responsible for starting, stopping, and saving
 * the replay files as well as recording all the packets.
 *
 * @param server The [MinecraftServer] instance.
 * @param profile The profile of the player being recorded.
 * @see ReplayPlayerRecorder
 * @see ReplayChunkRecorder
 */
public abstract class ReplayRecorder(
    public val server: MinecraftServer,
    public val profile: GameProfile,
    public val settings: RecorderSettings,
    provider: (ReplayRecorder) -> ReplayWriter
) {
    private val packets by lazy { Object2ObjectOpenHashMap<String, DebugPacketData>() }

    private var start: Long = 0

    private var protocol: ProtocolInfo<*> = LoginProtocols.CLIENTBOUND
    private var lastPacket = Duration.ZERO

    internal var started = false
        private set

    private var ignore = false

    @Suppress("LeakingThis")
    protected val writer: ReplayWriter = provider.invoke(this)

    /**
     * The directory at which all the temporary replay
     * files will be stored.
     * This also determines the final location of the replay file.
     */
    public val location: Path
        get() = this.writer.path

    /**
     * Whether the replay recorder has stopped and
     * is no longer recording any packets.
     */
    public val stopped: Boolean
        get() = this.writer.closed

    /**
     * The number of markers the replay has recorded.
     */
    public val markers: Int
        get() = this.writer.markers

    /**
     * Whether the recorder is currently paused
     */
    public open val paused: Boolean
        get() = false

    /**
     * The [UUID] of the player the recording is of.
     */
    public val recordingPlayerUUID: UUID
        get() = this.profile.id

    /**
     * The level that the replay recording is currently in.
     */
    public abstract val level: ServerLevel

    /**
     * The current position of the recorder.
     */
    public abstract val position: Vec3

    /**
     * The current rotation of the recorder.
     */
    public abstract val rotation: Vec2

    /**
     * This records an outgoing clientbound packet to the
     * replay file.
     *
     * This method will throw an exception if the recorder
     * has not started recording yet.
     *
     * This method **is** thread-safe; however, it should be noted
     * that any packet optimizations cannot be done if called off
     * the main thread, therefore only calling this method on
     * the main thread is preferable.
     *
     * @param outgoing The outgoing [Packet].
     */
    public open fun record(outgoing: Packet<*>) {
        if (!this.started) {
            throw IllegalStateException("Cannot record packets if recorder not started")
        }
        if (this.ignore || this.stopped) {
            return
        }
        val safe = this.server.isSameThread
        if (this.settings.debug && !safe) {
            ArcadeUtils.logger.warn("Trying to record packet off-thread ${outgoing.getDebugName()}")
        }

        if (ReplayOptimizerUtils.shouldIgnorePacket(this, outgoing)) {
            return
        }

        if (outgoing is ClientboundBundlePacket) {
            for (sub in outgoing.subPackets()) {
                this.record(sub)
            }
            return
        }
        if (this.writer.prePacketRecord(outgoing)) {
            return
        }
        if (!this.canRecordPacket(outgoing)) {
            return
        }

        val protocol = this.protocol
        val timestamp = this.getTimestamp()
        this.lastPacket = timestamp

        this.writer.writePacket(outgoing, protocol, timestamp, !safe).thenApply { bytes ->
            if (this.settings.debug && bytes != null) {
                val type = outgoing.getDebugName()
                this.packets.getOrPut(type) { DebugPacketData(type, 0, 0) }.increment(bytes)
            }
        }

        this.writer.postPacketRecord(outgoing)
        this.checkRecordingStatus()
    }

    /**
     * This tries to start this replay recorder and returns
     * whether it was successful in doing so.
     *
     * @param mode Whether this is restarting a previous recording, [StartingMode.Start] by default.
     * @return `true` if the recording started successfully `false` otherwise.
     */
    public fun start(mode: StartingMode = StartingMode.Start): Boolean {
        if (!this.started && this.initialize()) {
            this.onStart(mode)
            return true
        }

        return false
    }

    /**
     * Fires when a replay has started/restarted.
     *
     * @param mode Whether the recording is being started or restarted.
     */
    @Internal
    @JvmOverloads
    public fun onStart(mode: StartingMode = StartingMode.Start) {
        GlobalEventHandler.Server.broadcast(ReplayRecorderStartEvent(this, mode))
        this.writer.broadcastToOpsAndConsole("${mode.getContinuousVerb()} replay for ${this.getName()}")
    }

    /**
     * Stops the replay recorder and returns a future which will be completed
     * when the file has completed saving or closing.
     *
     * A failed future will be returned if the replay is already stopped.
     *
     * @param save Whether the recorded replay should be saved to disk, `true` by default.
     * @return A future which will be completed after the recording has finished saving or
     *     closing, this completes with the file size of the final compressed replay in bytes.
     */
    @JvmOverloads
    public fun stop(save: Boolean = true): CompletableFuture<Long> {
        if (this.stopped) {
            return CompletableFuture.failedFuture(IllegalStateException("Cannot stop replay after already stopped"))
        }

        if (this.settings.debug) {
            this.writer.broadcastToOpsAndConsole("Replay ${this.getName()} Debug Packet Data:\n${this.getDebugPacketData()}")
        }

        // We only save if the player has actually logged in...
        val future = this.writer.close(this.lastPacket, save && this.protocol.id() == ConnectionProtocol.PLAY)
        this.onClosing(future)

        GlobalEventHandler.Server.broadcast(ReplayRecorderStopEvent(this, future))

        return future
    }

    /**
     * Adds a marker to the replay file which can be viewed in ReplayMod.
     *
     * @param name The name of the marker, null for unnamed.
     * @param position The marked position.
     * @param rotation The marked rotation.
     * @param timestamp The timestamp of the marker (milliseconds).
     */
    public fun addMarker(
        name: String? = null,
        position: Vec3 = this.position,
        rotation: Vec2 = this.rotation,
        timestamp: Duration = this.getTimestamp(),
        color: Int = 0xFF0000
    ) {
        this.addMarker(ReplayMarker(name, position, rotation, timestamp, color))
    }

    /**
     * Adds a marker to the replay file.
     *
     * @param marker The marker to add.
     */
    public fun addMarker(marker: ReplayMarker) {
        this.writer.writeMarker(marker)
    }

    /**
     * This returns the total amount of time (in milliseconds) that
     * has elapsed since the recording has started, this does not
     * account for any pauses.
     *
     * @return The total amount of time (in milliseconds) that has
     *     elapsed since the start of the recording.
     */
    public fun getTotalRecordingTime(): Duration {
        return (System.currentTimeMillis() - this.start).milliseconds
    }

    /**
     * This returns the raw (uncompressed) file size of the replay in bytes.
     *
     * @return The raw file size of the replay in bytes.
     */
    public fun getRawRecordingSize(): Long {
        return this.writer.getRawRecordingSize()
    }

    /**
     * This creates a future which will provide the status of the
     * replay recorder as a formatted string.
     *
     * @return A future that will provide the status of the replay recorder.
     */
    public fun getStatusWithSize(): CompletableFuture<String> {
        val builder = ToStringBuilder(this, StandardToStringStyle().apply {
            fieldSeparator = ", "
            fieldNameValueSeparator = " = "
            isUseClassName = false
            isUseIdentityHashCode = false
        })

        val time = this.getTotalRecordingTime().formatHHMMSS()
        builder.append("name", this.getName())
        builder.append("time", time)

        this.appendToStatus(builder)

        builder.append("raw_size", FileUtils.formatSize(this.getRawRecordingSize()))
        return CompletableFuture.completedFuture(builder.toString())
    }

    /**
     * This gets the current timestamp (in milliseconds) of the replay recording.
     *
     * By default, this is the same as [getTotalRecordingTime] however this
     * may be overridden to account for pauses in the replay.
     *
     * @return The timestamp of the recording (in milliseconds).
     */
    public open fun getTimestamp(): Duration {
        return this.getTotalRecordingTime()
    }

    /**
     * Returns whether a given player should be hidden from the player tab list.
     *
     * @return Whether the player should be hidden
     */
    public open fun shouldHidePlayerFromTabList(player: ServerPlayer): Boolean {
        return false
    }

    /**
     * This allows you to add any additional metadata which will be
     * saved in the replay file.
     *
     * @param meta The JSON metadata map which can be mutated.
     */
    public open fun addMetadata(meta: JsonObject) {
        // TODO: Add some way for external meta
        meta.addProperty("name", this.getName())
        meta.addProperty("location", this.location.pathString)
        meta.addProperty("epoch_time_ms", System.currentTimeMillis())

        val mods = JsonObject()
        for ((mod, version) in ArcadeReplay.getLoadedMods()) {
            mods.addProperty(mod, version)
        }
        meta.add("mods", mods)

        meta.addProperty("version", ArcadeUtils.version)
        meta.add("settings", this.settings.asJson())
    }

    protected fun spawnPlayer(player: ServerPlayer, packets: Collection<Packet<*>>) {
        this.writer.writePlayer(player, packets)
    }

    /**
     * This appends any additional data to the status.
     *
     * @param builder The [ToStringBuilder] which is used to build the status.
     * @see getStatusWithSize
     */
    protected open fun appendToStatus(builder: ToStringBuilder) {

    }

    /**
     * This method tries to restart the replay recorder by creating
     * a new instance of itself.
     *
     * @return Whether it successfully restarted.
     */
    public abstract fun restart(): Boolean

    /**
     * This gets the name of the replay recording.
     *
     * @return The name of the replay recording.
     */
    public abstract fun getName(): String

    /**
     * This gets the viewing command for this replay for after it's saved.
     *
     * @return The command to view this replay.
     */
    public abstract fun getViewingCommand(): String

    /**
     * This starts the replay recording, note this is **not** called
     * to start a replay if a player is being recorded from the login phase.
     *
     * This method should just simulate
     */
    protected abstract fun initialize(): Boolean

    /**
     * This gets called when the replay is closing.
     *
     * @param future The future that will complete once the replay has closed.
     */
    protected abstract fun onClosing(future: CompletableFuture<Long>)

    /**
     * Determines whether a given packet is able to be recorded.
     *
     * @param packet The packet that is going to be recorded.
     * @return Whether this recorded should record it.
     */
    protected open fun canRecordPacket(packet: Packet<*>): Boolean {
        if (packet is ClientboundCustomPayloadPacket) {
            val payload = packet.payload
            if (payload is RecordablePayload && !payload.shouldRecord()) {
                return false
            }
            // FIXME: Add distant horizons support?
            if (payload.type().id.namespace == "distant_horizons") {
                return false
            }
        }
        return true
    }

    /**
     * Calling this ignores any packets that would've been
     * recorded by this recorder inside the [block] function.
     *
     * @param block The function to call while ignoring packets.
     */
    public fun ignore(block: () -> Unit) {
        val previous = this.ignore
        try {
            this.ignore = true
            block()
        } finally {
            this.ignore = previous
        }
    }

    @Internal
    public abstract fun takeSnapshot()

    @Internal
    public fun tick() {
        this.writer.tick()
    }

    /**
     * This method formats all the debug packet data
     * into a string.
     *
     * @return The formatted debug packet data.
     */
    @Internal
    public fun getDebugPacketData(): String {
        return this.packets.values
            .sortedByDescending { it.size }
            .joinToString(separator = "\n", transform = DebugPacketData::format)
    }

    /**
     * This method should be called after the player that is being
     * recorded has logged in.
     * This will mark the replay recorder as being started and will
     * change the replay recording phase into `CONFIGURATION`.
     */
    @Internal
    public fun afterLogin() {
        if (!this.started) {
            this.started = true
            this.start = System.currentTimeMillis()
        }

        this.protocol = LoginProtocols.CLIENTBOUND
        // We will not have recorded this, so we need to do it manually.
        this.record(ClientboundLoginFinishedPacket(this.profile))

        this.protocol = ConfigurationProtocols.CLIENTBOUND
    }

    /**
     * This method should be called after the player has finished
     * their configuration phase, and this will mark the player
     * as playing the game - actually in the Minecraft world.
     */
    @Internal
    public fun afterConfigure() {
        this.protocol = GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(this.server.registryAccess()))
    }

    private fun checkRecordingStatus() {
        val maxDuration = this.settings.maxRecordingDuration
        if (maxDuration.isPositive()) {
            if (this.getTimestamp() > maxDuration) {
                this.stop(true)
                this.writer.broadcastToOpsAndConsole(
                    "Stopped recording replay for ${this.getName()}, past duration limit ${maxDuration}!"
                )
                if (this.settings.restartAfterMaxRecordingDuration) {
                    this.restart()
                }
                return
            }
        }

        val maxFileSize = this.settings.maxRawRecordingFileSize
        if (maxFileSize.bytes > 0) {
            if (this.getRawRecordingSize() > maxFileSize.bytes) {
                this.stop(true)
                this.writer.broadcastToOpsAndConsole(
                    "Stopped recording replay for ${this.getName()}, past file size limit ${maxDuration}!"
                )
                if (this.settings.restartAfterMaxRawRecordingFileSize) {
                    this.restart()
                }
                return
            }
        }
    }

    public enum class StartingMode {
        Start, Restart;

        internal fun getContinuousVerb(): String {
            return when (this) {
                Start -> "Starting"
                Restart -> "Restarting"
            }
        }
    }
}