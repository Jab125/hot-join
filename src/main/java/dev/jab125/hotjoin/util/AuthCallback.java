package dev.jab125.hotjoin.util;

import java.util.function.Consumer;

public interface AuthCallback {
	void hotjoin$authResponse(Consumer<String> authConsumer);
}
