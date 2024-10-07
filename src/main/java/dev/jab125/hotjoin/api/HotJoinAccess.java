package dev.jab125.hotjoin.api;

import dev.jab125.hotjoin.HotJoin;

public class HotJoinAccess {
	public static <T extends Throwable> void launchMinecraftClient(String compat, String magic) throws T {
		try {
			HotJoin.launchMinecraftClient(compat, magic);
		} catch (Throwable t) {
			throw (T) t;
		}
	}

}
