package dev.jab125.hotjoin.compat.legacy4j;

import net.minecraft.client.User;
import wily.legacy.util.MCAccount;

public class Legacy4JModCompat implements ILegacy4JModCompat {
	@Override
	public void setSession(User user) {
		MCAccount.setUser(user);
	}
}
