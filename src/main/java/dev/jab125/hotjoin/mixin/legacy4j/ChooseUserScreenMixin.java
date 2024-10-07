package dev.jab125.hotjoin.mixin.legacy4j;

import dev.jab125.hotjoin.util.AuthCallback;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.client.screen.ChooseUserScreen;

import java.util.function.Consumer;

@Mixin(ChooseUserScreen.class)
public class ChooseUserScreenMixin implements AuthCallback {
	private @Unique
	@Nullable Consumer<String> authResponse;
	@Override
	public void hotjoin$authResponse(Consumer<String> authConsumer) {
		this.authResponse = authConsumer;
	}

	@Override
	public Consumer<String> hotjoin$authResponse() {
		return this.authResponse;
	}
}
