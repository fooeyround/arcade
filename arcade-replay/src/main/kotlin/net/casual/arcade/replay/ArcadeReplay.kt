/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay

import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.replay.recorder.chunk.ReplayChunkRecorder
import net.casual.arcade.replay.recorder.chunk.ReplayChunkRecorders
import net.casual.arcade.replay.recorder.player.ReplayPlayerRecorder
import net.casual.arcade.replay.recorder.player.PlayerRecorders
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import net.fabricmc.loader.impl.metadata.AbstractModMetadata

public object ArcadeReplay: ModInitializer {
    override fun onInitialize() {
        ReplayChunkRecorders.registerEvents()

        GlobalEventHandler.Server.register<ServerTickEvent> {
            PlayerRecorders.recorders().forEach(ReplayPlayerRecorder::tick)
            ReplayChunkRecorders.recorders().forEach(ReplayChunkRecorder::tick)
        }
    }

    internal fun getLoadedMods(): Map<String, String> {
        return FabricLoader.getInstance().allMods
            .filter { it.origin.kind != ModOrigin.Kind.NESTED }
            .filter { it.metadata.type != AbstractModMetadata.TYPE_BUILTIN }
            .associateBy({ it.metadata.id }, { it.metadata.version.friendlyString })
    }
}