package dev.jab125.hotjoin.mixin.legacy4j;

import dev.jab125.hotjoin.util.AuthCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.ChooseUserScreen;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.client.screen.RenderableVListScreen;

import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(ChooseUserScreen.class)
public abstract class ChooseUserScreenMixin extends PanelVListScreen implements AuthCallback {
	private @Unique @Nullable Consumer<String> authResponse;

	@SuppressWarnings("DataFlowIssue")
	public ChooseUserScreenMixin() {
		super(null, null);
	}

	@Override
	public void hotjoin$authResponse(Consumer<String> authConsumer) {
		this.authResponse = authConsumer;
	}

	@Override
	public Consumer<String> hotjoin$authResponse() {
		return this.authResponse;
	}

//	@Inject(method = "<init>", at = @At("RETURN"))
//	void interceptRender(Screen parent, CallbackInfo ci) {
//		this.addRenderableOnly((guiGraphics, mouseX, mouseY, delta) -> {
//			guiGraphics.drawCenteredString(minecraft.font, "Joining with [CONTROLLER NAME]", this.width / 2, 15, 0xffffffff);
//		});
//	}
}
