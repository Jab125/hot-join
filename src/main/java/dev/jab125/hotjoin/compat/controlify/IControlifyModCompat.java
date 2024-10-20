package dev.jab125.hotjoin.compat.controlify;

import dev.jab125.hotjoin.compat.IModCompat;
import net.minecraft.client.User;

public sealed interface IControlifyModCompat extends IModCompat permits ControlifyModCompat {
	@Override
	default void setSession(User user) {
		throw new IllegalStateException("This shouldn't be called!");
	}

	void init();
}
