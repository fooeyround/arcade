/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.mixins.player;

import net.casual.arcade.replay.recorder.player.ReplayPlayerRecorder;
import net.casual.arcade.replay.recorder.player.ReplayPlayerRecorders;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(ServerEntity.class)
public class ServerEntityMixin {
    @Shadow
    @Mutable
    @Final
    private Consumer<Packet<?>> broadcast;
    @Shadow
    @Mutable
    @Final
    private BiConsumer<Packet<?>, List<UUID>> broadcastWithIgnore;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void onInit(
        ServerLevel level,
        Entity entity,
        int updateInterval,
        boolean trackDelta,
        Consumer<Packet<?>> broadcast,
        BiConsumer<Packet<?>, List<UUID>> broadcastWithIgnore,
        CallbackInfo ci
    ) {
        // I previously had this ModifyArg into TrackedEntity<init>
        // into ServerEntity<init>; however, polymer redirects this
        // constructor, so I need another way of doing this...
        // ^ This is actually no longer the case, but this method works fine
        if (entity instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            Consumer<Packet<?>> original = this.broadcast;
            this.broadcast = packet -> {
                ReplayPlayerRecorders.record(uuid, packet);
                original.accept(packet);
            };
            BiConsumer<Packet<?>, List<UUID>> ignorable = this.broadcastWithIgnore;
            this.broadcastWithIgnore = (packet, uuids) -> {
                if (!uuids.contains(uuid)) {
                    ReplayPlayerRecorders.record(uuid, packet);
                }
                ignorable.accept(packet, uuids);
            };
        }
    }
}
