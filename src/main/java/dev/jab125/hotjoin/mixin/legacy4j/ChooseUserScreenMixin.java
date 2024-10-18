package dev.jab125.hotjoin.mixin.legacy4j;

import dev.jab125.hotjoin.compat.legacy4j.Legacy4JData;
import dev.jab125.hotjoin.util.AuthCallback;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.client.screen.ChooseUserScreen;
import wily.legacy.client.screen.PanelVListScreen;

import java.util.function.BiConsumer;

@Mixin(ChooseUserScreen.class)
public abstract class ChooseUserScreenMixin extends PanelVListScreen implements AuthCallback {
	private @Unique @Nullable BiConsumer<String, String> authResponse;

	@SuppressWarnings("DataFlowIssue")
	public ChooseUserScreenMixin() {
		super(null, null);
	}

	@Override
	public void hotjoin$authResponse(BiConsumer<String, String> authConsumer) {
		this.authResponse = authConsumer;
	}

	@Override
	public BiConsumer<String, String> hotjoin$authResponse() {
		return this.authResponse;
	}

	@Unique private Legacy4JData data;
	@Override
	public Object hotjoin$legacy4jData() {
		return this.data;
	}

	@Override
	public void hotjoin$legacy4jData(Object object) {
		this.data = (Legacy4JData) object;
	}

	//	@Inject(method = "<init>", at = @At("RETURN"))
//	void interceptRender(Screen parent, CallbackInfo ci) {
//		this.addRenderableOnly((guiGraphics, mouseX, mouseY, delta) -> {
//			guiGraphics.drawCenteredString(minecraft.font, "Joining with [CONTROLLER NAME]", this.width / 2, 15, 0xffffffff);
//		});
//	}
}
