/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.recorder.player

import com.mojang.authlib.GameProfile
import me.senseiwells.replay.ServerReplay
import net.casual.arcade.replay.recorder.rejoin.RejoinedReplayPlayer
import net.casual.arcade.utils.PlayerUtils.levelServer
import net.casual.arcade.replay.util.processor.RecorderRecoverer
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * This object manages all [ReplayPlayerRecorder]s.
 */
public object PlayerRecorders {
    private val players = LinkedHashMap<UUID, ReplayPlayerRecorder>()
    private val closing = HashMap<UUID, ReplayPlayerRecorder>()

    /**
     * This creates a [ReplayPlayerRecorder] for a given [player].
     *
     * @param player The player you want to record.
     * @return The created recorder.
     * @see create
     */
    @JvmStatic
    public fun create(player: ServerPlayer): ReplayPlayerRecorder {
        if (player is RejoinedReplayPlayer) {
            throw IllegalArgumentException("Cannot create a replay for a rejoining player")
        }
        return create(player.levelServer, player.gameProfile)
    }

    /**
     * This creates a [ReplayPlayerRecorder] for a given [profile].
     *
     * @param server The [MinecraftServer] instance.
     * @param profile The profile of the player you are going to record.
     * @return The created recorder.
     * @see create
     */
    @JvmStatic
    public fun create(server: MinecraftServer, profile: GameProfile): ReplayPlayerRecorder {
        if (players.containsKey(profile.id)) {
            throw IllegalArgumentException("Player already has a recorder")
        }

        val path = ServerReplay.config.getPlayerRecordingLocation(profile)
        val recorder = ReplayPlayerRecorder(
            server,
            profile,
            ServerReplay.config.writerType.create(path)
        )
        players[profile.id] = recorder
        RecorderRecoverer.add(recorder)
        return recorder
    }

    /**
     * Checks whether a player has a recorder.
     *
     * @param player The player to check.
     * @return Whether the player has a recorder.
     */
    @JvmStatic
    public fun has(player: ServerPlayer): Boolean {
        return players.containsKey(player.uuid)
    }

    /**
     * Gets a player recorder if one is present.
     *
     * @param player The player to get the recorder for.
     * @return The [ReplayPlayerRecorder], null if it is not present.
     */
    @JvmStatic
    public fun get(player: ServerPlayer): ReplayPlayerRecorder? {
        return getByUUID(player.uuid)
    }

    /**
     * Gets a player recorder if one is present using the
     * player's UUID.
     *
     * @param uuid The uuid of the player to get the recorder for.
     * @return The [ReplayPlayerRecorder], null if it is not present.
     */
    @JvmStatic
    public fun getByUUID(uuid: UUID): ReplayPlayerRecorder? {
        return players[uuid]
    }

    /**
     * Gets a collection of all the currently recording player recorders.
     *
     * @return A collection of all the player recorders.
     */
    @JvmStatic
    public fun recorders(): Collection<ReplayPlayerRecorder> {
        return Collections.unmodifiableCollection(this.players.values)
    }

    /**
     * Gets a collection of all the currently closing player recorders.
     *
     * @return A collection of all the closing player recorders.
     */
    @JvmStatic
    public fun closing(): Collection<ReplayPlayerRecorder> {
        return Collections.unmodifiableCollection(this.closing.values)
    }

    internal fun close(server: MinecraftServer, recorder: ReplayPlayerRecorder, future: CompletableFuture<Long>) {
        val uuid = recorder.recordingPlayerUUID
        this.players.remove(uuid)
        this.closing[uuid] = recorder
        future.thenRunAsync({
            this.closing.remove(uuid)
        }, server)
    }
}