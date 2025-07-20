/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.ducks;

import net.casual.arcade.replay.viewer.ReplayViewer;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public interface ReplayViewable {
    void replay$startViewingReplay(ReplayViewer viewer);

    void replay$stopViewingReplay();

    @Nullable ReplayViewer replay$getViewingReplay();

    void replay$sendReplayViewerPacket(Packet<?> packet);
}
