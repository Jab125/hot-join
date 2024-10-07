package dev.jab125.hotjoin;

public class HotJoinPlayerLimitException extends RuntimeException {
	public HotJoinPlayerLimitException() {
	}

	public HotJoinPlayerLimitException(String message) {
		super(message);
	}

	public HotJoinPlayerLimitException(String message, Throwable cause) {
		super(message, cause);
	}

	public HotJoinPlayerLimitException(Throwable cause) {
		super(cause);
	}

	public HotJoinPlayerLimitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
