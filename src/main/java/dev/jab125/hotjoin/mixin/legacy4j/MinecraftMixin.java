package dev.jab125.hotjoin.mixin.legacy4j;

import dev.jab125.hotjoin.HotJoin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "setScreen", at = @At("HEAD"))
	void startSetScreen(Screen screen, CallbackInfo ci) {
		if (HotJoin.legacy4JModCompat != null) {
			HotJoin.legacy4JModCompat.onBeginScreenSet(Minecraft.getInstance().screen, screen);
		}
	}
}
