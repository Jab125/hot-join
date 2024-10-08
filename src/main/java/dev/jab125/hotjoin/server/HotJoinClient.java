package dev.jab125.hotjoin.server;

import dev.jab125.hotjoin.packet.AlohaPayload;
import dev.jab125.hotjoin.packet.SteamPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class HotJoinClient {
	private Socket clientSocket;
	private OutputStream out;
	private InputStream in;

	public void startConnection(String ip, int port) throws IOException {
		clientSocket = new Socket(ip, port);
		out = clientSocket.getOutputStream();
		in = clientSocket.getInputStream();
	}

	public String sendMessage(String msg) throws IOException {
		//out.println(msg);
		//String resp = in.readLine();
		//return resp;
		return null;
	}

	public void stopConnection() throws IOException {
		in.close();
		out.close();
		clientSocket.close();
	}

	public <T extends CustomPacketPayload> void send(CustomPacketPayload.Type<T> r, T value) throws IOException {
		HotJoinCommon.send(r, value, HotJoinCommon.rethrow(out::write));
	}

	public static void main(String[] args) throws IOException {
		HotJoinClient client = new HotJoinClient();
		System.out.println("Started");
		client.startConnection("127.0.0.1", 4444);
		int i = 0;
		FriendlyByteBuf bup = null;
		client.send(SteamPayload.TYPE, new SteamPayload(1337, 1337));
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
					System.out.println(decode);
					bup = null;
					i = 0;
				}
			}
		}
		client.stopConnection();
	}
}