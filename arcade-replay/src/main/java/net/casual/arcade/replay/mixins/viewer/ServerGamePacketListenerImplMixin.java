/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.mixins.viewer;

import net.casual.arcade.replay.ducks.ReplayViewable;
import net.casual.arcade.replay.viewer.ReplayViewer;
import net.casual.arcade.replay.viewer.ReplayViewerPackets;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.LastSeenMessagesValidator;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl implements ReplayViewable {
	@Mutable
	@Shadow
	@Final
	private LastSeenMessagesValidator lastSeenMessages;
	@Shadow
	@Nullable
	private RemoteChatSession chatSession;

	@Shadow private int nextChatIndex;
	@Unique
	private ReplayViewer replay$viewer = null;

	public ServerGamePacketListenerImplMixin(MinecraftServer minecraftServer, Connection connection, CommonListenerCookie commonListenerCookie) {
		super(minecraftServer, connection, commonListenerCookie);
	}

	@Inject(
		method = "shouldHandleMessage",
		at = @At("HEAD"),
		cancellable = true
	)
	private void canAcceptPacket(Packet<?> packet, CallbackInfoReturnable<Boolean> cir) {
		if (this.replay$viewer != null && !ReplayViewerPackets.serverboundBypass(packet)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(
		method = {
			"handleAnimate",
			"handleClientCommand",
			"handleContainerButtonClick",
			"handleContainerClick",
			"handlePlaceRecipe",
			"handleContainerClose",
			"handleInteract",
			"handleMovePlayer",
			"handlePlayerAbilities",
			"handlePlayerAction",
			"handlePlayerCommand",
			"handlePlayerInput",
			"handleSetCarriedItem",
			"handleSetCreativeModeSlot",
			"handleSignUpdate",
			"handleUseItemOn",
			"handleUseItem",
			"handleTeleportToEntityPacket",
			"handlePaddleBoat",
			"handleMoveVehicle",
			"handleAcceptTeleportPacket",
			"handleRecipeBookSeenRecipePacket",
			"handleRecipeBookChangeSettingsPacket",
			"handleSeenAdvancements",
			"handleCustomCommandSuggestions",
			"handleSetCommandBlock",
			"handleSetCommandMinecart",
			"handlePickItemFromBlock",
			"handlePickItemFromEntity",
			"handleRenameItem",
			"handleSetBeaconPacket",
			"handleSetStructureBlock",
			"handleSelectTrade",
			"handleEditBook",
			"handleEntityTagQuery",
			"handleContainerSlotStateChanged",
			"handleBlockEntityTagQuery",
			"handleSetJigsawBlock",
			"handleJigsawGenerate",
			"handleChangeDifficulty",
			"handleLockDifficulty",
			"handleChatSessionUpdate",
			"handleChunkBatchReceived"
		},
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
			shift = At.Shift.AFTER
		),
		cancellable = true
	)
	private void onServerboundPacket(@Coerce Packet<ServerGamePacketListener> packet, CallbackInfo ci) {
		if (this.replay$viewer != null) {
			this.replay$viewer.onServerboundPacket(packet);
			ci.cancel();
		}
	}

	@Inject(
		method = {
			"handleChat",
			"handleChatCommand",
			"handleSignedChatCommand"
		},
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;tryHandleChat(Ljava/lang/String;Ljava/lang/Runnable;)V"
		),
		cancellable = true
	)
	private void onServerboundChatPacket(@Coerce Packet<ServerGamePacketListener> packet, CallbackInfo ci) {
		if (this.replay$viewer != null) {
			ci.cancel();
			this.server.execute(() -> this.replay$viewer.onServerboundPacket(packet));
		}
	}

	@Override
	public void replay$startViewingReplay(ReplayViewer viewer) {
		this.replay$viewer = viewer;
	}

	@Override
	public void replay$stopViewingReplay() {
		if (this.replay$viewer != null) {
			this.nextChatIndex = 0;
			this.lastSeenMessages = new LastSeenMessagesValidator(20);
			this.replay$viewer = null;
			// Reset chat session
			this.chatSession = null;
		}
	}

	@Override
	public void replay$sendReplayViewerPacket(Packet<?> packet) {
		super.send(packet, null);
	}

	@Override
	public ReplayViewer replay$getViewingReplay() {
		return this.replay$viewer;
	}

	@Override
	public void send(Packet<?> packet, @Nullable PacketSendListener packetSendListener) {
		if (this.replay$viewer == null || ReplayViewerPackets.clientboundBypass(packet)) {
			super.send(packet, packetSendListener);
		}
	}
}
