package dev.jab125.hotjoin.mixin.legacy4j;

import dev.jab125.hotjoin.HotJoin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyResourceManager;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

@Mixin(value = LoadingOverlay.class, priority = 999)
public class LoadingOverlayMixin {
	// TODO
//	@Unique
//	private boolean hotjoin$cleared = false;
//	@Inject(method = "render", at = @At("HEAD"))
//	void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) throws IllegalAccessException {
//		if (HotJoin.hotjoinClient && !hotjoin$cleared) {
//			Optional<Field> loadIntroLocation = Arrays.stream(LoadingOverlay.class.getDeclaredFields()).filter(a -> a.getName().contains("loadIntroLocation")).findFirst();
//			Field field = loadIntroLocation.orElseThrow();
//			field.setAccessible(true);
//			field.set((LoadingOverlay) (Object) this, true);
//			LegacyResourceManager.INTROS.clear();
//			LegacyResourceManager.INTROS.add(ResourceLocation.fromNamespaceAndPath("legacy", "textures/gui/intro/background.png"));
//			hotjoin$cleared = true;
//		}
//	}
}
