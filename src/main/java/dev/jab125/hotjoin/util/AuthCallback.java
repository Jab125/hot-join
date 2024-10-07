package dev.jab125.hotjoin.util;

import java.util.function.Consumer;

public interface AuthCallback {
	void hotjoin$authResponse(Consumer<String> authConsumer);

	Consumer<String> hotjoin$authResponse();

	@SuppressWarnings("UnnecessaryReturnStatement")
	default void hotjoin$legacy4jData(Object object) {
		return;
	}

	default Object hotjoin$legacy4jData() {
		return null;
	}
}
