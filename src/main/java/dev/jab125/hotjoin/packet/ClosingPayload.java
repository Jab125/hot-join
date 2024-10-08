package dev.jab125.hotjoin.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClosingPayload() implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hotjoin", "closing");
	public static final CustomPacketPayload.Type<ClosingPayload> TYPE = new CustomPacketPayload.Type<>(ClosingPayload.ID);
	public static final StreamCodec<FriendlyByteBuf, ClosingPayload> STREAM_CODEC = StreamCodec.unit(new ClosingPayload());
	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
