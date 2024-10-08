package dev.jab125.hotjoin.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class HotJoinServerClientThread extends Thread {

	private final InputStream inputStream;
	protected Socket socket;
	CopyOnWriteArrayList<Consumer<HotJoinServerClientThread>> runnables = new CopyOnWriteArrayList<>();

	public HotJoinServerClientThread(Socket socket) {
		this.socket = socket;
		try {
			inputStream = socket.getInputStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void run() {
		int i = 0;
		FriendlyByteBuf bup = null;
		while (true) {
			for (Consumer<HotJoinServerClientThread> runnable : runnables) {
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
					System.out.println(decode);
					bup = null;
					i = 0;
				}
			}

		}
	}
	public void runTask(Runnable runnable) {
		runnables.add(f -> runnable.run());
	}

	public void runTask(Consumer<HotJoinServerClientThread> runnable) {
		runnables.add(runnable);
	}

	public <T extends CustomPacketPayload> void send(CustomPacketPayload.Type<T> r, T value) throws IOException {
		HotJoinCommon.send(r, value, HotJoinCommon.rethrow(socket.getOutputStream()::write));
	}
}
