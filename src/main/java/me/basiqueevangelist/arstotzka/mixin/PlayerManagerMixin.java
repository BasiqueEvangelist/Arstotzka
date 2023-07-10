package me.basiqueevangelist.arstotzka.mixin;

import com.mojang.authlib.GameProfile;
import me.basiqueevangelist.arstotzka.Arstotzka;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Redirect(method = "checkCanJoin", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;canBypassPlayerLimit(Lcom/mojang/authlib/GameProfile;)Z"))
    private boolean enforceBypass(PlayerManager instance, GameProfile profile) {
        return instance.canBypassPlayerLimit(profile) || Arstotzka.CONFIG.joinQueue();
    }
}
