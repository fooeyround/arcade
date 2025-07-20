/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.events

import net.casual.arcade.events.common.Event
import net.casual.arcade.replay.recorder.ReplayRecorder

public class ReplayRecorderCloseEvent(
    public val recorder: ReplayRecorder
): Event