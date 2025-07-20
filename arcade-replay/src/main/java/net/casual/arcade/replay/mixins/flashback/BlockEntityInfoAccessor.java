/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.mixins.flashback;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundLevelChunkPacketData.BlockEntityInfo.class)
public interface BlockEntityInfoAccessor {
    @Accessor
    int getPackedXZ();

    @Accessor
    int getY();

    @Accessor
    BlockEntityType<?> getType();

    @Accessor
    @Nullable CompoundTag getTag();
}
