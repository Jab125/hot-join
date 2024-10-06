package dev.jab125.hotjoin.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SteamPayload(int in, int val) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hotjoin", "steam");
	public static final Type<SteamPayload> TYPE = new Type<>(SteamPayload.ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, SteamPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, SteamPayload::in, ByteBufCodecs.INT, SteamPayload::val, SteamPayload::new);
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
