/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.viewer

import net.minecraft.server.level.ServerPlayer
import java.nio.file.Path
import java.util.*

public object ReplayViewers {
    private val viewers = LinkedHashMap<UUID, ReplayViewer>()

    @JvmStatic
    public fun start(path: Path, player: ServerPlayer): ReplayViewer {
        val viewer = ReplayViewer(path, player.connection)
        this.viewers[player.uuid] = viewer
        viewer.start()
        return viewer
    }

    @JvmStatic
    public fun remove(uuid: UUID): ReplayViewer? {
        return this.viewers.remove(uuid)
    }

    @JvmStatic
    public fun viewers(): Collection<ReplayViewer> {
        return Collections.unmodifiableCollection(this.viewers.values)
    }
}