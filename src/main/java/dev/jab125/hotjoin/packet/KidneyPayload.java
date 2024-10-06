package dev.jab125.hotjoin.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KidneyPayload(byte[] b) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hotjoin", "kidney");
	public static final CustomPacketPayload.Type<KidneyPayload> TYPE = new CustomPacketPayload.Type<>(KidneyPayload.ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, KidneyPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BYTE_ARRAY, KidneyPayload::b, KidneyPayload::new);
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
