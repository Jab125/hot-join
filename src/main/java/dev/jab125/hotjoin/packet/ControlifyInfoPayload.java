package dev.jab125.hotjoin.packet;

import dev.jab125.hotjoin.compat.controlify.ControlifyData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ControlifyInfoPayload(ControlifyData data) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hotjoin", "controlify");
	public static final Type<ControlifyInfoPayload> TYPE = new Type<>(ControlifyInfoPayload.ID);
	public static final StreamCodec<FriendlyByteBuf, ControlifyInfoPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8.map(ControlifyData::new, ControlifyData::uid), ControlifyInfoPayload::data, ControlifyInfoPayload::new);
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
