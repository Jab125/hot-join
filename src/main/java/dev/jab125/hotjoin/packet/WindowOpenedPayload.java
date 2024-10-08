package dev.jab125.hotjoin.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WindowOpenedPayload() implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hotjoin", "window-opened");
	public static final Type<WindowOpenedPayload> TYPE = new Type<>(WindowOpenedPayload.ID);
	public static final StreamCodec<FriendlyByteBuf, WindowOpenedPayload> STREAM_CODEC = StreamCodec.unit(new WindowOpenedPayload());
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
