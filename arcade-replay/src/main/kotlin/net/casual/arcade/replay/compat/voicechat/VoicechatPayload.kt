/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.compat.voicechat

import net.casual.arcade.replay.api.network.RecordablePayload
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

internal class VoicechatPayload private constructor(
    private val type: CustomPacketPayload.Type<*>,
    private val writer: (FriendlyByteBuf) -> Unit
): CustomPacketPayload, RecordablePayload {
    override fun shouldRecord(): Boolean {
        return true
    }

    override fun record(buf: FriendlyByteBuf) {
        this.writer.invoke(buf)
    }

    override fun type(): CustomPacketPayload.Type<*> {
        return this.type
    }

    companion object {
        fun of(type: CustomPacketPayload.Type<*>, writer: (FriendlyByteBuf) -> Unit): VoicechatPayload {
            return VoicechatPayload(type, writer)
        }
    }
}