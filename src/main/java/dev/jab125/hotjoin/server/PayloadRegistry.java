package dev.jab125.hotjoin.server;

import dev.jab125.hotjoin.packet.AlohaPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;

public class PayloadRegistry {
	static {
		HotJoinCommon.init();
	}
	static HashMap<CustomPacketPayload.Type<?>, StreamCodec<FriendlyByteBuf, ?>> REGS;

	public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec) {
		if (REGS == null) REGS = new HashMap<>();
		REGS.put(type, codec);
	}
}
