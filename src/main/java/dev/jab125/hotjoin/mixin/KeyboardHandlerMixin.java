package dev.jab125.hotjoin.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.jab125.hotjoin.client.Screenshot;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.function.Consumer;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
	@Redirect(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"))
	void h(File file, RenderTarget renderTarget, Consumer<Component> consumer) {
		// Screenshot.grab(
		//						this.minecraft.gameDirectory,
		//						this.minecraft.getMainRenderTarget(),
		//						component -> this.minecraft.execute(() -> this.minecraft.gui.getChat().addMessage(component))
		//					);
		Screenshot.takeScreenshot();
	}
}
