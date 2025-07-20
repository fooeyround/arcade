/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.mixins.common;

import me.senseiwells.replay.ServerReplay;
import net.casual.arcade.replay.recorder.chunk.ReplayChunkRecorder;
import net.casual.arcade.replay.recorder.chunk.ChunkRecorders;
import net.casual.arcade.replay.recorder.player.ReplayPlayerRecorder;
import net.casual.arcade.replay.recorder.player.PlayerRecorders;
import me.senseiwells.replay.util.processor.RecorderFixerUpper;
import net.casual.arcade.replay.util.processor.RecorderRecoverer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	@Inject(
		method = "runServer",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;",
			shift = At.Shift.AFTER
		)
	)
	private void onServerLoaded(CallbackInfo ci) {
		MinecraftServer instance = (MinecraftServer) (Object) this;
		RecorderRecoverer.tryRecover(instance);

		if (ServerReplay.getConfig().getEnabled()) {
			ServerReplay.getConfig().startChunks(instance);
		}
	}

	@Inject(
		method = "stopServer",
		at = @At("TAIL")
	)
	private void onServerStopped(CallbackInfo ci) {
		for (ReplayPlayerRecorder recorder : PlayerRecorders.recorders()) {
			recorder.stop();
		}

		for (ReplayChunkRecorder recorder : ChunkRecorders.recorders()) {
			recorder.stop();
		}

		RecorderRecoverer.INSTANCE.waitForRecovering();
		RecorderFixerUpper.INSTANCE.waitForFixingUp();
	}
}
