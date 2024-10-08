package dev.jab125.hotjoin.server;

import dev.jab125.hotjoin.HotJoin;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.function.TriConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static dev.jab125.hotjoin.server.HotJoinServer.handlers;

public class HotJoinS2CThread extends Thread {

	private final InputStream inputStream;
	protected Socket socket;
	// UUID of the connected player, set via Aloha packet
	public UUID uuid;
	CopyOnWriteArrayList<Consumer<HotJoinS2CThread>> runnables = new CopyOnWriteArrayList<>();
	private boolean shouldDisconnect = false;

	public HotJoinS2CThread(Socket socket) {
		this.socket = socket;
		try {
			inputStream = socket.getInputStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void run() {
		new Thread(() -> {
			while (true) {
				if (this.shouldDisconnect) return;
				for (Consumer<HotJoinS2CThread> runnable : runnables) {
					runnable.accept(this);
					runnables.remove(runnable);
				}
			}
		}).start();
		int i = 0;
		FriendlyByteBuf bup = null;
		while (true) {
			if (this.shouldDisconnect) {
				try {
					socket.close();
				} catch (IOException e) {
					HotJoin.LOGGER.error("Failed to stop thread!", e);
					//throw new RuntimeException(e);
				}
			}
			for (Consumer<HotJoinS2CThread> runnable : runnables) {
				//System.out.println("ran a task");
				runnable.accept(this);
				runnables.remove(runnable);
			}

			byte read;
			try {
				read = (byte) inputStream.read();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if (read == -1) break;
			if (bup == null) {
				if (read == Constants.MAGIC_START[i]) {
					i++;
				} else {
					i = 0;
				}
				if (i == Constants.MAGIC_START.length) {
					bup = PacketByteBufs.create();
					i = 0;
				}
			} else {
				bup = bup.writeByte(read);
				if (read == Constants.MAGIC_END[i]) {
					i++;
				} else {
					i = 0;
				}

				if (i == Constants.MAGIC_END.length) {
					ResourceLocation resourceLocation = bup.readResourceLocation();
					Map.Entry<CustomPacketPayload.Type<?>, StreamCodec<FriendlyByteBuf, ?>> typeStreamCodecEntry = PayloadRegistry.REGS.entrySet().stream().filter(a -> a.getKey().id().equals(resourceLocation)).findFirst().orElseThrow();
					StreamCodec<FriendlyByteBuf, ?> value = typeStreamCodecEntry.getValue();
					Object decode = value.decode(bup);
					// noinspection unchecked
					((TriConsumer) handlers.get(typeStreamCodecEntry.getKey())).accept(this, decode, uuid);
					bup = null;
					i = 0;
				}
			}

		}
	}
	public void runTask(Runnable runnable) {
		runnables.add(f -> runnable.run());
	}

	public void runTask(Consumer<HotJoinS2CThread> runnable) {
		runnables.add(runnable);
	}

	public <T extends CustomPacketPayload> void send(T value) {
		try {
			HotJoinCommon.send((CustomPacketPayload.Type<? super T>) value.type(), value, HotJoinCommon.rethrow(socket.getOutputStream()::write));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void disconnect() {
		this.shouldDisconnect = true;
	}
}
