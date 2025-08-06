package net.casual.arcade.dimensions.mixins.level;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerWaypointManager.class)
public class ServerWaypointManagerMixin {

    @ModifyReceiver(method = "isLocatorBarEnabledFor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private static GameRules fixVanillaLocatorBarGameRuleCheck(GameRules instance, GameRules.Key<GameRules.BooleanValue> key, @Local(argsOnly = true) ServerPlayer player) {
        return player.level().getGameRules();
    }

}
