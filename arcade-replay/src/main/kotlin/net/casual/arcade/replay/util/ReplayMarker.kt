/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.util

import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import kotlin.time.Duration

public data class ReplayMarker(
    val name: String?,
    val position: Vec3?,
    val rotation: Vec2?,
    val timestamp: Duration,
    val color: Int
)