package dev.jab125.hotjoin.packet;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record AlohaPayload(UUID uuid) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hotjoin", "aloha");
	public static final Type<AlohaPayload> TYPE = new Type<>(AlohaPayload.ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, AlohaPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, AlohaPayload::uuid, AlohaPayload::new);
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
