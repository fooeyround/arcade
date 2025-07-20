/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.mixins.rejoin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.TrackedEntity.class)
public interface TrackedEntityAccessor {
	@Accessor("entity")
	Entity getEntity();

	@Accessor("serverEntity")
	ServerEntity getServerEntity();

	@Invoker("getEffectiveRange")
	int getRange();
}
