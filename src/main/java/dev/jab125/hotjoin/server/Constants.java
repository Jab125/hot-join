package dev.jab125.hotjoin.server;

public class Constants {
	/*
	 * For anyone taking "inspiration", change the numbers.
	 */
	public static final byte[] MAGIC_START = new byte[]{
			100,
			101,
			127,
			-13,
			14,
			17,
			127,
			127,
			127,
			14,
			15
	};
	public static final byte[] MAGIC_END = new byte[]{
			19,
			15,
			-127,
			-127,
			127,
			-128,
			17,
			19,
			81,
			-16
	};
}
