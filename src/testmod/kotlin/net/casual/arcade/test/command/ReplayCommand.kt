package net.casual.arcade.test.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.casual.arcade.commands.*
import net.casual.arcade.commands.arguments.EnumArgument
import net.casual.arcade.replay.io.ReplayFormat
import net.casual.arcade.replay.recorder.ReplayRecorder
import net.casual.arcade.replay.recorder.chunk.ChunkArea
import net.casual.arcade.replay.recorder.chunk.ReplayChunkRecorders
import net.casual.arcade.replay.recorder.player.ReplayPlayerRecorders
import net.casual.arcade.replay.util.FileUtils.streamDirectoryEntriesOrEmpty
import net.casual.arcade.replay.viewer.ReplayViewers
import net.casual.arcade.utils.ArcadeUtils
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import java.util.concurrent.CompletableFuture
import kotlin.io.path.createDirectories
import kotlin.io.path.name

object ReplayCommand: CommandTree {
    private val path = ArcadeUtils.path.resolve("replays").createDirectories()

    override fun create(buildContext: CommandBuildContext): LiteralArgumentBuilder<CommandSourceStack> {
        return CommandTree.buildLiteral("replay") {
            literal("start") {
                literal("self") {
                    argument("format", EnumArgument.enumeration<ReplayFormat>()) {
                        executes(::startOwnReplay)
                    }
                }
                literal("chunks") {
                    literal("around") {
                        argument("x", IntegerArgumentType.integer()) {
                            suggests(::suggestChunkX)
                            argument("z", IntegerArgumentType.integer()) {
                                suggests(::suggestChunkZ)
                                argument("radius", IntegerArgumentType.integer()) {
                                    argument("format", EnumArgument.enumeration<ReplayFormat>()) {
                                        executes(::startChunksAround)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            literal("stop") {
                literal("all") {
                    executes(::stopAllReplays)
                }
            }
            literal("view") {
                argument("name", StringArgumentType.string()) {
                    suggests(::suggestViewableReplays)
                    executes(::viewReplay)
                }
            }
        }
    }

    private fun startOwnReplay(context: CommandContext<CommandSourceStack>): Int {
        val player = context.source.playerOrException
        val path = ArcadeUtils.path.resolve("replays").createDirectories()
        val format = EnumArgument.getEnumeration<ReplayFormat>(context, "format")
        val recorder = ReplayPlayerRecorders.create(player, path, format)
        try {
            recorder.start()
        } catch (e: Exception) {
            ArcadeUtils.logger.error("Failed to start replay", e)
            return context.source.fail("Failed to start replay")
        }
        return context.source.success("Successfully started recorder ${recorder.getName()}")
    }

    private fun startChunksAround(context: CommandContext<CommandSourceStack>): Int {
        val level = context.source.level
        val chunkX = IntegerArgumentType.getInteger(context, "x")
        val chunkZ = IntegerArgumentType.getInteger(context, "z")
        val radius = IntegerArgumentType.getInteger(context, "radius")
        val format = EnumArgument.getEnumeration<ReplayFormat>(context, "format")

        val area = ChunkArea.of(level, chunkX, chunkZ, radius)
        val recorder = ReplayChunkRecorders.create(area, this.path, format)
        try {
            recorder.start()
        } catch (e: Exception) {
            ArcadeUtils.logger.error("Failed to start replay", e)
            return context.source.fail("Failed to start replay")
        }
        return context.source.success("Successfully started recorder ${recorder.getName()}")
    }

    private fun stopAllReplays(context: CommandContext<CommandSourceStack>): Int {
        ReplayPlayerRecorders.recorders().forEach(ReplayRecorder::stop)
        ReplayChunkRecorders.recorders().forEach(ReplayRecorder::stop)
        return context.source.success("Successfully stopped all recorders")
    }

    private fun viewReplay(context: CommandContext<CommandSourceStack>) {
        val name = StringArgumentType.getString(context, "name")
        val path = this.path.resolve(name)
        try {
            val viewer = ReplayViewers.create(path, context.source.playerOrException)
            viewer.start()
        } catch (e: Exception) {
            ArcadeUtils.logger.error("Failed to view replay", e)
        }
    }

    private fun suggestViewableReplays(
        @Suppress("UNUSED_PARAMETER") context: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val suggestions = this.path.streamDirectoryEntriesOrEmpty()
            .filter { path -> ReplayFormat.formatOf(path) != null }
            .map { "\"${it.name}\"" }
        return SharedSuggestionProvider.suggest(suggestions, builder)
    }

    private fun suggestChunkX(
        context: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val x = context.source.playerOrException.chunkPosition().x
        return SharedSuggestionProvider.suggest(listOf(x.toString()), builder)
    }

    private fun suggestChunkZ(
        context: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val z = context.source.playerOrException.chunkPosition().z
        return SharedSuggestionProvider.suggest(listOf(z.toString()), builder)
    }
}