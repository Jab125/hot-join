package dev.jab125.hotjoin.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ScreenshotRequestPayload() implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hotjoin", "screenshot-request");
	public static final Type<ScreenshotRequestPayload> TYPE = new Type<>(ScreenshotRequestPayload.ID);
	public static final StreamCodec<FriendlyByteBuf, ScreenshotRequestPayload> STREAM_CODEC = StreamCodec.unit(new ScreenshotRequestPayload());
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
