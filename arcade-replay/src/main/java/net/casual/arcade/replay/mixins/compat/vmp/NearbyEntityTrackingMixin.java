/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.mixins.compat.vmp;

import com.ishland.vmp.common.playerwatching.NearbyEntityTracking;
import com.llamalad7.mixinextras.sugar.Local;
import net.casual.arcade.replay.mixins.rejoin.ChunkMapAccessor;
import net.casual.arcade.replay.recorder.chunk.ReplayChunkRecordable;
import net.casual.arcade.replay.recorder.chunk.ReplayChunkRecorders;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NearbyEntityTracking.class, remap = false)
public class NearbyEntityTrackingMixin {
    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/ishland/vmp/common/playerwatching/ServerPlayerEntityExtension;vmpTracking$updatePosition()V"
        )
    )
    private void onPlayerTrackingTick(
        CallbackInfo ci,
        @Local ServerPlayer player,
        @Local boolean positionUpdated
    ) {
        if (positionUpdated) {
            ServerLevel level = player.serverLevel();
            ChunkMap map = level.getChunkSource().chunkMap;
            ChunkMap.TrackedEntity tracked = ((ChunkMapAccessor) map).getEntityMap().get(player.getId());
            ReplayChunkRecorders.updateRecordable((ReplayChunkRecordable) tracked, level.dimension(), player.chunkPosition());
        }
    }
}
