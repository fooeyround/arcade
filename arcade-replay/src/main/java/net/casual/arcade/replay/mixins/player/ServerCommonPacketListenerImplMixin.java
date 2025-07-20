/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.mixins.player;

import com.mojang.authlib.GameProfile;
import net.casual.arcade.replay.ducks.ReplayViewable;
import net.casual.arcade.replay.recorder.player.ReplayPlayerRecorder;
import net.casual.arcade.replay.recorder.player.PlayerRecorders;
import net.casual.arcade.replay.viewer.ReplayViewer;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// We want to apply our #send mixin *LAST*, any
// other mods which modify the packs should come first
@Mixin(value = ServerCommonPacketListenerImpl.class, priority = 5000)
public abstract class ServerCommonPacketListenerImplMixin {
	@Shadow protected abstract GameProfile playerProfile();

	@Inject(
		method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
		at = @At("HEAD")
	)
	private void onPacket(Packet<?> packet, PacketSendListener listener, CallbackInfo ci) {
		ReplayPlayerRecorder recorder = PlayerRecorders.getByUUID(this.playerProfile().getId());
		if (recorder != null) {
			recorder.record(packet);
		}
	}

	@Inject(
		method = "onDisconnect",
		at = @At("TAIL")
	)
	private void onDisconnect(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
		ReplayPlayerRecorder recorder = PlayerRecorders.getByUUID(this.playerProfile().getId());
		if (recorder != null) {
			recorder.stop();
		}

		if (this instanceof ReplayViewable viewable) {
			ReplayViewer viewer = viewable.replay$getViewingReplay();
			if (viewer != null) {
				viewer.close();
			}
		}
	}
}
