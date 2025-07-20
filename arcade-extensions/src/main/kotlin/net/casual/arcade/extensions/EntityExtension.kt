/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.extensions

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import org.jetbrains.annotations.ApiStatus.Internal

public abstract class EntityExtension private constructor(
    private val provider: () -> Entity
): TransferableEntityExtension {
    public val entity: Entity
        get() = this.provider.invoke()

    public constructor(entity: Entity): this(entityToProvider(entity))

    private companion object {
        @Internal
        @JvmField
        @Suppress("unused")
        val SHOULD_ATTACH_EXTENSION: ThreadLocal<Boolean> = ThreadLocal.withInitial { true }

        fun entityToProvider(entity: Entity): () -> Entity {
            if (entity is ServerPlayer) {
                val connection = entity.connection
                return { connection.player }
            }
            return { entity }
        }
    }
}