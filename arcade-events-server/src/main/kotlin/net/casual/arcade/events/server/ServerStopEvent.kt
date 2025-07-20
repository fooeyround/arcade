/*
 * Copyright (c) 2024 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.events.server

import net.casual.arcade.events.BuiltInEventPhases
import net.casual.arcade.events.common.Event
import net.minecraft.server.MinecraftServer

public data class ServerStopEvent(
    val server: MinecraftServer
): Event {
    public companion object {
        public const val PRE_PHASE: String = BuiltInEventPhases.PRE

        public const val POST_PHASE: String = BuiltInEventPhases.POST
    }
}