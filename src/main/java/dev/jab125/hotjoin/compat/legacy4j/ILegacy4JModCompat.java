package dev.jab125.hotjoin.compat.legacy4j;

import dev.jab125.hotjoin.compat.IModCompat;
import net.minecraft.client.User;

public interface ILegacy4JModCompat extends IModCompat {
	@Override
	void setSession(User user);
}
