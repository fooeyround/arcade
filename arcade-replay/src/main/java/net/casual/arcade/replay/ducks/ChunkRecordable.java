/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package net.casual.arcade.replay.ducks;

import net.casual.arcade.replay.recorder.chunk.ReplayChunkRecorder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@ApiStatus.Internal
public interface ChunkRecordable extends net.casual.replay.recorder.chunk.ChunkRecordable {
	@NotNull
	@Override
	default Collection<ReplayChunkRecorder> getRecorders() {
		return this.replay$getRecorders();
	}

	@Override
	default void addRecorder(@NotNull ReplayChunkRecorder recorder) {
		this.replay$addRecorder(recorder);
	}

	@Override
	default void resendPackets(@NotNull ReplayChunkRecorder recorder) {
		this.replay$resendPackets(recorder);
	}

	@Override
	default void removeRecorder(@NotNull ReplayChunkRecorder recorder) {
		this.replay$removeRecorder(recorder);
	}

	@Override
	default void removeAllRecorders() {
		this.replay$removeAllRecorders();
	}

	Collection<ReplayChunkRecorder> replay$getRecorders();

	void replay$addRecorder(ReplayChunkRecorder recorder);

	void replay$resendPackets(ReplayChunkRecorder recorder);

	void replay$removeRecorder(ReplayChunkRecorder recorder);

	void replay$removeAllRecorders();
}
