package dev.jab125.hotjoin.util;

import java.util.function.BiConsumer;

public interface AuthCallback {
	void hotjoin$authResponse(BiConsumer<String /*uuid*/, String /*magic*/> authConsumer);

	BiConsumer<String, String> hotjoin$authResponse();

	@SuppressWarnings("UnnecessaryReturnStatement")
	default void hotjoin$legacy4jData(Object object) {
		return;
	}

	default Object hotjoin$legacy4jData() {
		return null;
	}
}
