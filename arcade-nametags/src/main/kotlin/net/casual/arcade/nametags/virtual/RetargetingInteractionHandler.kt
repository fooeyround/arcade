/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.nametags.virtual

import eu.pb4.polymer.virtualentity.api.elements.VirtualElement.InteractionHandler
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

public class RetargetingInteractionHandler(
    public val owner: Entity
): InteractionHandler {
    override fun attack(player: ServerPlayer) {
        player.connection.handleInteract(
            ServerboundInteractPacket.createAttackPacket(this.owner, player.isShiftKeyDown)
        )
    }

    override fun interact(player: ServerPlayer, hand: InteractionHand) {
        player.connection.handleInteract(
            ServerboundInteractPacket.createInteractionPacket(this.owner, player.isShiftKeyDown, hand)
        )
    }

    override fun interactAt(player: ServerPlayer, hand: InteractionHand, pos: Vec3) {
        player.connection.handleInteract(
            ServerboundInteractPacket.createInteractionPacket(this.owner, player.isShiftKeyDown, hand, pos)
        )
    }
}