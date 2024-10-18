package dev.jab125.hotjoin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.jab125.hotjoin.client.Screenshot;
import dev.jab125.hotjoin.packet.*;
import dev.jab125.hotjoin.server.HotJoinClient;
import dev.jab125.hotjoin.server.HotJoinS2CThread;
import dev.jab125.hotjoin.server.HotJoinServer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

			{
				command.then(ClientCommandManager.literal("instances").then(ClientCommandManager.literal("abort").then(ClientCommandManager.argument("uuid", StringArgumentType.word()).suggests((context, builder) -> {
					for (UUID instance : INSTANCES) {
						builder = builder.suggest(instance.toString(), Component.literal("Connected: " + uuidPlayerMap.containsKey(instance)));
					}
					return builder.buildFuture();
				}).executes(context -> {
					String string = StringArgumentType.getString(context, "uuid");
					UUID o = UUID.fromString(string);
					boolean d = INSTANCES.remove(o);
					HotJoinS2CThread remove = uuidPlayerMap.remove(o);
					boolean v = remove != null;
					if (v) remove.disconnect();
					if (legacy4JModCompat != null) legacy4JModCompat.leftWorld(o);
					arrangeWindows();
					return (d ? 1 : 0) + (v ? 1 : 0);
				}))));
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
			System.out.println("New client joined with UUID " + uuid);
			thread.uuid = payload.uuid();
			uuidPlayerMap.put(thread.uuid, thread);
			// Malicious connection?
			if (!INSTANCES.contains(thread.uuid)) {
				thread.disconnect();
				System.out.println("Disconnected.");
			} else {
				if (legacy4JModCompat != null) legacy4JModCompat.connectionEstablished(thread, payload, uuid);
			}
		});

		HotJoinServer.registerPacketHandler(WindowOpenedPayload.TYPE, (thread, payload, uuid) -> {
			System.out.println("Windows opened.");
			thread.isWindowReady = true;
			arrangeWindows();
		});

		HotJoinServer.registerPacketHandler(ClosingPayload.TYPE, (thread, payload, uuid) -> {
			INSTANCES.remove(uuid);
			uuidPlayerMap.remove(uuid);
			if (legacy4JModCompat != null) legacy4JModCompat.leftWorld(uuid);
			arrangeWindows();
		});

		HotJoinServer.registerPacketHandler(ScreenshotRequestPayload.TYPE, (thread, payload, uuid) -> {
			RenderSystem.recordRenderCall(Screenshot::takeScreenshot);
		});

		HotJoinServer.registerPacketHandler(ScreenshotC2SPayload.TYPE, Screenshot::handle);

		// TODO
//		HotJoinServer.registerPacketHandler(AlohaPayload.TYPE, payload -> {
//			if (INSTANCES.contains(payload.uuid())) {
//				uuidPlayerMap.put(payload.uuid(), context.player());
//				arrangeWindows();
//			}
//		});

		ClientLifecycleEvents.CLIENT_STARTED.register(a -> {
			HotJoin.arrangeWindows();
		});
	}
}
