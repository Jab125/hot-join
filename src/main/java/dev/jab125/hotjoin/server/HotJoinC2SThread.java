package dev.jab125.hotjoin.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static dev.jab125.hotjoin.server.HotJoinClient.handlers;

public class HotJoinC2SThread extends Thread {

	@Override
	public void run() {
		try {
			run0();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private HotJoinClient client;
	@SuppressWarnings("rawtypes")
	private void run0() throws Throwable {
		client = new HotJoinClient();
		System.out.println("Started");
		client.startConnection("127.0.0.1", 4447);
		new Thread(() -> {
			while (true) {
				for (Consumer<HotJoinC2SThread> runnable : runnables) {
					runnable.accept(this);
					runnables.remove(runnable);
				}
			}
		}).start();
		int i = 0;
		FriendlyByteBuf bup = null;
		try {
			while (true) {
				byte read = (byte) client.in.read();
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
						((Consumer) handlers.get(typeStreamCodecEntry.getKey())).accept(decode);
						bup = null;
						i = 0;
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		System.out.println("Connection has halted.");
		client.stopConnection();
	}

	CopyOnWriteArrayList<Consumer<HotJoinC2SThread>> runnables = new CopyOnWriteArrayList<>();
	public void runTask(Runnable runnable) {
		runnables.add(f -> runnable.run());
	}

	public void runTask(Consumer<HotJoinC2SThread> runnable) {
		System.out.println("Task added");
		runnables.add(runnable);
	}

	public <T extends CustomPacketPayload> void send(T value) {
		try {
			HotJoinCommon.send((CustomPacketPayload.Type<? super T>) value.type(), value, HotJoinCommon.rethrow(client.out::write));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
