package dev.jab125.hotjoin.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

public record Legacy4JSdlNativesPayload(Path path) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hotjoin", "sdlnatives");
	public static final Type<Legacy4JSdlNativesPayload> TYPE = new Type<>(Legacy4JSdlNativesPayload.ID);
	public static final StreamCodec<FriendlyByteBuf, Legacy4JSdlNativesPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8.map(Path::of, a -> a.toAbsolutePath().toString()), Legacy4JSdlNativesPayload::path, Legacy4JSdlNativesPayload::new);
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
