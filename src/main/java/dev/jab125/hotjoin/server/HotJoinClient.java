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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class HotJoinClient {
	private Socket clientSocket;
	OutputStream out;
	InputStream in;

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

	static final HashMap<CustomPacketPayload.Type<?>, Consumer<?>> handlers = new HashMap<>();
	public static <T extends CustomPacketPayload> void registerPacketHandler(CustomPacketPayload.Type<T> t, Consumer<T> value) {
		handlers.put(t, value);
	}
}