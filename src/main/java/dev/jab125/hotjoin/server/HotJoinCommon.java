package dev.jab125.hotjoin.server;

import dev.jab125.hotjoin.packet.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.io.IOException;
import java.util.function.Consumer;

public class HotJoinCommon {
	static {
		init();
	}
	private static boolean initted = false;
	public static void init() {
		if (initted) return;
		PayloadRegistry.register(SteamPayload.TYPE, SteamPayload.STREAM_CODEC);
		PayloadRegistry.register(AlohaPayload.TYPE, AlohaPayload.STREAM_CODEC);
		PayloadRegistry.register(ClosingPayload.TYPE, ClosingPayload.STREAM_CODEC);
		PayloadRegistry.register(WindowOpenedPayload.TYPE, WindowOpenedPayload.STREAM_CODEC);
		PayloadRegistry.register(ScreenshotRequestPayload.TYPE, ScreenshotRequestPayload.STREAM_CODEC);
		PayloadRegistry.register(SdlNativesPayload.TYPE, SdlNativesPayload.STREAM_CODEC);
		initted = true;
	}

	public static <T extends CustomPacketPayload> void send(CustomPacketPayload.Type<T> type, T value, Consumer<byte[]> consumer) throws IOException {
		@SuppressWarnings("unchecked") StreamCodec<FriendlyByteBuf, T> friendlyByteBufTStreamCodec = (StreamCodec<FriendlyByteBuf, T>) PayloadRegistry.REGS.get(type);
		FriendlyByteBuf friendlyByteBuf = PacketByteBufs.create();
		friendlyByteBuf.writeBytes(Constants.MAGIC_START);
		friendlyByteBuf.writeResourceLocation(type.id());
		friendlyByteBufTStreamCodec.encode(friendlyByteBuf, value);
		friendlyByteBuf.writeBytes(Constants.MAGIC_END);
		consumer.accept(friendlyByteBuf.array());
	}

	public static <T> Consumer<T> rethrow(ThrowableConsumer<T> throwableConsumer) {
		return t -> {
			try {
				throwableConsumer.apply(t);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		};
	}

	public interface ThrowableConsumer<T> {
		void apply(T val) throws Throwable;
	}
}
