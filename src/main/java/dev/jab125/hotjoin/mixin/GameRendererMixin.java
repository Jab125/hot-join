package dev.jab125.hotjoin.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.jab125.hotjoin.client.render.SharedRendering;
import net.deechael.concentration.fabric.config.ConcentrationConfigFabric;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Shadow @Final private Minecraft minecraft;

	@Shadow @Final private RenderBuffers renderBuffers;

	// Lnet/minecraft/client/gui/GuiGraphics;flush()V
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
	void re(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
//		Window window = minecraft.getWindow();
//		int x = ConcentrationConfigFabric.getInstance().x;
//		int y = ConcentrationConfigFabric.getInstance().y;
//		int scale = SharedRendering.getScale();
//		Matrix4f matrix4f = new Matrix4f()
//				.setOrtho(
//						-x, (float)((double)window.getWidth() / SharedRendering.getScale()), (float)((double)window.getHeight() / SharedRendering.getScale()), -y, 1000.0F, 21000.0F
//				);
//		RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
//		//Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
//		//matrix4fStack.popMatrix();
//		//matrix4fStack.pushMatrix();
//		//matrix4fStack.translation(0.0F, 0.0F, -11000.0F);
//		RenderSystem.applyModelViewMatrix();
		GuiGraphics graphics = new GuiGraphics(minecraft, this.renderBuffers.bufferSource());
		SharedRendering.render0(graphics);
		//matrix4fStack.popMatrix();
	}
}
