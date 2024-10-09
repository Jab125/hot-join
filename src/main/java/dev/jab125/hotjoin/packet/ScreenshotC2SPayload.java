package dev.jab125.hotjoin.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

public record ScreenshotC2SPayload(Path path, int x, int y, int width, int height) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hotjoin", "screenshot-c2s");
	public static final Type<ScreenshotC2SPayload> TYPE = new Type<>(ScreenshotC2SPayload.ID);
	public static final StreamCodec<FriendlyByteBuf, ScreenshotC2SPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8.map(Path::of, a -> a.toAbsolutePath().toString()), ScreenshotC2SPayload::path, ByteBufCodecs.INT, ScreenshotC2SPayload::x, ByteBufCodecs.INT, ScreenshotC2SPayload::y, ByteBufCodecs.INT, ScreenshotC2SPayload::width, ByteBufCodecs.INT, ScreenshotC2SPayload::height, ScreenshotC2SPayload::new);
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
