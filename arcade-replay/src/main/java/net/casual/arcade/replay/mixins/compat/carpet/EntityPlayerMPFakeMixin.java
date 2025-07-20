/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.mixins.compat.carpet;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import me.senseiwells.replay.ServerReplay;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityPlayerMPFake.class)
public class EntityPlayerMPFakeMixin extends ServerPlayer {
	public EntityPlayerMPFakeMixin(MinecraftServer minecraftServer, ServerLevel serverLevel, GameProfile gameProfile, ClientInformation clientInformation) {
		super(minecraftServer, serverLevel, gameProfile, clientInformation);
	}

	@Override
	public int requestedViewDistance() {
		if (ServerReplay.getConfig().getFixCarpetBotViewDistance()) {
			return this.server.getPlayerList().getViewDistance();
		}
		return super.requestedViewDistance();
	}
}
