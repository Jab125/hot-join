package dev.jab125.hotjoin;

import dev.jab125.hotjoin.packet.AlohaPayload;
import dev.jab125.hotjoin.packet.ClosingPayload;
import dev.jab125.hotjoin.packet.WindowOpenedPayload;
import dev.jab125.hotjoin.server.HotJoinClient;
import dev.jab125.hotjoin.server.HotJoinServer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static dev.jab125.hotjoin.HotJoin.*;

public class HotJoinServerInit {
	public static void init() {
		new Thread(() -> {
			try {
				HotJoinServer.main(null);
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}).start();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			var command = ClientCommandManager.literal("hotjoin");
			if (FabricLoader.getInstance().isModLoaded("authme")) {
				command.then(ClientCommandManager.literal("authme").then(ClientCommandManager.literal("microsoft").executes(authMeCompat::hotJoinAuthMeMicrosoft)));
			}

			if (FabricLoader.getInstance().isModLoaded("legacy")) {
				command.then(ClientCommandManager.literal("legacy4j").executes(legacy4JModCompat::hotJoinLegacy4J));
			}

			if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
				command.executes(HotJoin::hotJoin);
			}

			dispatcher.register(command);
		});

		// TODO
//		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
//			Optional<Map.Entry<UUID, ServerPlayer>> first = uuidPlayerMap.entrySet().stream().filter(a -> a.getValue() == handler.player).findFirst();
//			if (first.isPresent()) {
//				UUID key = first.get().getKey();
//				INSTANCES.remove(key);
//				uuidPlayerMap.remove(key);
//				if (legacy4JModCompat != null) legacy4JModCompat.leftWorld(key);
//				arrangeWindows();
//			}
//		});

		HotJoinServer.registerPacketHandler(AlohaPayload.TYPE, (thread, payload, uuid) -> {
			// UUID is null at the moment, we set it here
			System.out.println("ALOHA!");
			thread.uuid = payload.uuid();
			uuidPlayerMap.put(thread.uuid, thread);
			// Malicious connection?
			if (!INSTANCES.contains(thread.uuid)) {
				thread.disconnect();
				System.out.println("Disconnected.");
			}
		});

		HotJoinServer.registerPacketHandler(WindowOpenedPayload.TYPE, (thread, payload, uuid) -> {
			System.out.println("Windows opened.");
			arrangeWindows();
		});

		HotJoinServer.registerPacketHandler(ClosingPayload.TYPE, (thread, payload, uuid) -> {
			INSTANCES.remove(uuid);
			arrangeWindows();
		});

		// TODO
//		HotJoinServer.registerPacketHandler(AlohaPayload.TYPE, payload -> {
//			if (INSTANCES.contains(payload.uuid())) {
//				uuidPlayerMap.put(payload.uuid(), context.player());
//				arrangeWindows();
//			}
//		});
	}
}
