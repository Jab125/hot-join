package dev.jab125.hotjoin.api;

import dev.jab125.hotjoin.HotJoin;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class HotJoinAccess {
	public static UUID launchMinecraftClient(String compat, String magic) {
		return launchMinecraftClient(compat, magic, null);
	}
	public static <T extends Throwable> UUID launchMinecraftClient(String compat, String magic, @Nullable String legacy4jData) throws T {
		return launchMinecraftClient(compat, magic, legacy4jData, null);
	}

	public static <T extends Throwable> UUID launchMinecraftClient(String compat, String magic, @Nullable String legacy4jData, @Nullable String folderName) throws T {
		try {
			return HotJoin.launchMinecraftClient(compat, magic, legacy4jData, folderName);
		} catch (Throwable t) {
			throw (T) t;
		}
	}

}
