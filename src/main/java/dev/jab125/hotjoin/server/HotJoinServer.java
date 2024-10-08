package dev.jab125.hotjoin.server;

import dev.jab125.hotjoin.HotJoin;
import dev.jab125.hotjoin.packet.AlohaPayload;
import dev.jab125.hotjoin.packet.SteamPayload;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class HotJoinServer {
	private ServerSocket serverSocket;
	//private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;

	public void start(int port) throws IOException, InterruptedException {
		serverSocket = new ServerSocket(port);
		//serverSocket.accept()
		while (true) {
			Socket accept = serverSocket.accept();
			HotJoinServerClientThread hotJoinServerClientThread = new HotJoinServerClientThread(accept);
			hotJoinServerClientThread.start();
			System.out.println("A client has connected");
			hotJoinServerClientThread.runTask(t -> {
				try {
					t.send(SteamPayload.TYPE, new SteamPayload(15, 15));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
		//out = new PrintWriter(clientSocket.getOutputStream(), true);
		//in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	}

	public <T extends CustomPacketPayload> void send(CustomPacketPayload.Type<T> r, T value) throws IOException {
//		FriendlyByteBuf friendlyByteBuf = PacketByteBufs.create();
//		friendlyByteBuf.writeBytes(Constants.MAGIC_START);
//		codec.encode(friendlyByteBuf, value);
//		friendlyByteBuf.writeBytes(Constants.MAGIC_END);
//		clientSocket.getOutputStream().write(friendlyByteBuf.array());
		//HotJoinCommon.send(r, value, HotJoinCommon.rethrow(clientSocket.getOutputStream()::write));
	}

	public void stop() throws IOException {
		in.close();
		out.close();
		//clientSocket.close();
		serverSocket.close();
	}
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Started");
		HotJoinServer server = new HotJoinServer();
		server.start(4444);
	}
}
