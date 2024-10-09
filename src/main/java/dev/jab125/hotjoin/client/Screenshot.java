package dev.jab125.hotjoin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.jab125.hotjoin.HotJoin;
import dev.jab125.hotjoin.HotJoinClientInit;
import dev.jab125.hotjoin.mixin.NativeImageAccessor;
import dev.jab125.hotjoin.mixin.ScreenshotAccessor;
import dev.jab125.hotjoin.packet.ScreenshotC2SPayload;
import dev.jab125.hotjoin.packet.ScreenshotRequestPayload;
import dev.jab125.hotjoin.server.HotJoinS2CThread;
import net.deechael.concentration.Concentration;
import net.deechael.concentration.fabric.config.ConcentrationConfigFabric;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Screenshot {

	public static <T extends Throwable> Path ts() throws T {
		try {
			return _ts();
		} catch (Throwable t) {
			throw (T) t;
		}
	}

	public static <T extends Throwable> void takeScreenshot() throws T {
		try {
			takeScreenshot0();
		} catch (Throwable t) {
			throw (T) t;
		}
	}

	public static Path _ts() throws IOException {
		RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
		NativeImage nativeImage = net.minecraft.client.Screenshot.takeScreenshot(mainRenderTarget);

		Path tempFile = Files.createTempFile("hotjoin.", ".png");
		nativeImage.writeToFile(tempFile);
		tempFile.toFile().deleteOnExit();
		nativeImage.close();
		return tempFile;
	}
	private static void takeScreenshot0() throws IOException {
		if (handled != null && handled.get() != 0) return; // don't allow spamming screenshots
		handled = new AtomicInteger();
		if (!HotJoin.hotjoinClient) {
			Path bytes = _ts();
			handle(null, new ScreenshotC2SPayload(bytes, ConcentrationConfigFabric.getInstance().x, ConcentrationConfigFabric.getInstance().y, ConcentrationConfigFabric.getInstance().width, ConcentrationConfigFabric.getInstance().height), null);
			Collection<HotJoinS2CThread> values = HotJoin.uuidPlayerMap.values();
			int size = 1;
			for (HotJoinS2CThread value : values) {
				if (value.isWindowReady) {
					size++;
					value.runTask(t -> {
						t.send(new ScreenshotRequestPayload());
					});
				}
			}
			while (handled.get() < size); // block the game until the screenshot finished
			RenderSystem.recordRenderCall(() -> {
				BufferedImage nativeImage = null;
				try {
					nativeImage = stitchScreenshots();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				after(nativeImage);
			});
		} else {
			HotJoinClientInit.hotJoinC2SThread.runTask(t -> t.send(new ScreenshotRequestPayload()));
		}
	}

	private static void after(BufferedImage nativeImage) {
		String string = null;
		File file = Minecraft.getInstance().gameDirectory;
		Consumer<Component> consumer = c -> {
			Minecraft.getInstance().execute(() -> {
				Minecraft.getInstance().gui.getChat().addMessage(c);
			});
		};
		File file2 = new File(file, "screenshots");
		file2.mkdir();
		File file3;
		if (string == null) {
			file3 = ScreenshotAccessor.getFile(file2);
		} else {
			file3 = new File(file2, string);
		}

		Util.ioPool()
				.execute(
						() -> {
							try {
								ImageIO.write(nativeImage, "png", file3);
								Component component = Component.literal(file3.getName())
										.withStyle(ChatFormatting.UNDERLINE)
										.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file3.getAbsolutePath())));
								consumer.accept(Component.translatable("screenshot.success", component));
							} catch (Exception var7) {
								HotJoin.LOGGER.warn("Couldn't save screenshot", (Throwable)var7);
								consumer.accept(Component.translatable("screenshot.failure", var7.getMessage()));
							} finally {
								//nativeImage.close();
							}
						}
				);
	}

	private static volatile AtomicInteger handled = new AtomicInteger();
	private static CopyOnWriteArrayList<ScreenshotC2SPayload> payloads = new CopyOnWriteArrayList<>();
	public static void handle(@Nullable HotJoinS2CThread thread, ScreenshotC2SPayload payload, @Nullable UUID uuid) {
		handled.incrementAndGet();
		payloads.add(payload);
	}


	public static BufferedImage stitchScreenshots() throws IOException {
		ArrayList<BufferedImage> nativeImages = new ArrayList<>();
		Map<BufferedImage, ScreenshotC2SPayload> map = new HashMap<>();
		int width = 0;
		int height = 0;
		for (ScreenshotC2SPayload payload : payloads) {
			BufferedImage read = ImageIO.read(new ByteArrayInputStream(Files.readAllBytes(payload.path())));
			width = Math.max(width, payload.x() + read.getWidth());
			height = Math.max(height, payload.y() + read.getHeight());
			nativeImages.add(read);
			map.put(read, payload);
		}
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); //new NativeImage(NativeImage.Format.RGBA, width, height, true);
		for (BufferedImage nativeImage : nativeImages) {
			ScreenshotC2SPayload screenshotC2SPayload = map.get(nativeImage);
			int x1 = screenshotC2SPayload.x();
			int y1 = screenshotC2SPayload.y();
			for (int x = 0; x < nativeImage.getWidth(); x++) {
				for (int y = 0; y < nativeImage.getHeight(); y++) {
					int argb = nativeImage.getRGB(x, y);
					image.setRGB(x1 + x, y1 + y, argb);
				}
			}
		}
//		for (BufferedImage nativeImage : nativeImages) {
//			nativeImage.close();
//		}
		nativeImages = null;
		map = null;
		handled = new AtomicInteger();
		payloads.clear();
		return image;
	}
}
