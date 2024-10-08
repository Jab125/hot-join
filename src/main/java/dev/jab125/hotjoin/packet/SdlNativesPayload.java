package dev.jab125.hotjoin.packet;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.UUID;

public record SdlNativesPayload(Path path) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hotjoin", "sdlnatives");
	public static final Type<SdlNativesPayload> TYPE = new Type<>(SdlNativesPayload.ID);
	public static final StreamCodec<FriendlyByteBuf, SdlNativesPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8.map(Path::of, a -> a.toAbsolutePath().toString()), SdlNativesPayload::path, SdlNativesPayload::new);
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
