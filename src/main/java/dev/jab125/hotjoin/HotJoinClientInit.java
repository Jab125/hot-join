package dev.jab125.hotjoin;

import com.google.common.io.ByteStreams;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.jab125.hotjoin.client.Screenshot;
import dev.jab125.hotjoin.compat.IModCompat;
import dev.jab125.hotjoin.packet.*;
import dev.jab125.hotjoin.server.HotJoinC2SThread;
import dev.jab125.hotjoin.server.HotJoinClient;
import dev.jab125.hotjoin.util.HotJoinCodecs;
import net.deechael.concentration.Concentration;
import net.deechael.concentration.fabric.config.ConcentrationConfigFabric;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.sounds.SoundSource;

import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import static dev.jab125.hotjoin.HotJoin.*;

public class HotJoinClientInit {
	private static HotJoin.Wrapped wrapped;
	public static HotJoinC2SThread hotJoinC2SThread;

	public static void init() {
		HotJoinClient.registerPacketHandler(SteamPayload.TYPE, payload -> {
			Minecraft.getInstance().execute(() -> {
				if (wrapped == null) wrapped = HotJoin.wrap(Minecraft.getInstance());
				int val = payload.val();
				switch (payload.in()) {
					case HotJoin.Instructions.WIDTH -> wrapped.width(val);
					case HotJoin.Instructions.HEIGHT -> wrapped.height(val);
					case HotJoin.Instructions.X -> wrapped.x(val);
					case HotJoin.Instructions.Y -> wrapped.y(val);
					case HotJoin.Instructions.APPLY -> {
						wrapped.apply();
						wrapped = null;
					}
				}
			});
		});

		HotJoinClient.registerPacketHandler(ScreenshotRequestPayload.TYPE, payload -> {
			RenderSystem.recordRenderCall(() -> {
				Path ts = Screenshot.ts();
				hotJoinC2SThread.runTask(t -> {
					t.send(
						new ScreenshotC2SPayload(
								ts,
								ConcentrationConfigFabric.getInstance().x,
								ConcentrationConfigFabric.getInstance().y,
								ConcentrationConfigFabric.getInstance().width,
								ConcentrationConfigFabric.getInstance().height
						)
					);

				}
				);
			});
		});

		HotJoinClient.registerPacketHandler(SdlNativesPayload.TYPE, payload -> {
			Minecraft.getInstance().execute(() -> {
				legacy4JModCompat.receivedSdlNatives(payload);
			});
		});

		HotJoinClientInit.hotJoinC2SThread = new HotJoinC2SThread();
		hotJoinC2SThread.start();

		boolean[] firstTime = new boolean[]{true, true, true};

		String hotjoinServer = System.getProperty("hotjoin.server", "");
		String t = System.getProperty("hotjoin.uuid", "");
		UUID hotjoinUUID = t.isEmpty() ? null : UUID.fromString(t);
		String magic = System.getProperty("hotjoin.magic", "");
		String compatString = System.getProperty("hotjoin.compat", "authme");
		IModCompat compat = "authme".equals(compatString) ? authMeCompat : "legacy4j".equals(compatString) ? legacy4JModCompat : null;
		String legacy4jData = System.getProperty("hotjoin.legacy4jData", "");
		if (!legacy4jData.isEmpty()) legacy4JModCompat.sendLegacy4jData(legacy4jData);


		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof TitleScreen || screen instanceof AccessibilityOnboardingScreen) {
				if (firstTime[0]) firstTime[0] = false;
				else {
					if (firstTime[1]) {
						firstTime[1] = false;
						legacy4JModCompat.joinedWorld();
						if (!magic.isEmpty()) {
							assert compat != null;
							compat.setSession(HotJoinCodecs.USER_CODEC.decode(NbtOps.INSTANCE, crashgoByeBye(() -> NbtIo.read(ByteStreams.newDataInput(Base64.getDecoder().decode(magic.replace("$", "=")))))).resultOrPartial(LOGGER::error).orElseThrow().getFirst());
						}
						client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0d);
					}
					if (firstTime[2]) {
						join(new ServerData("Splitscreen Host", hotjoinServer, ServerData.Type.LAN));
						firstTime[2] = false;
					} else {
						// we failed to join, we don't want to end up in a loop, so we end now
						client.stop();
					}
				}
			}
		});

		hotJoinC2SThread.runTask(v -> v.send(new AlohaPayload(hotjoinUUID)));

//		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
//
//		});
		ClientPlayConnectionEvents.DISCONNECT.register((clientPacketListener,c) -> {
			c.stop();
		});

		ClientLifecycleEvents.CLIENT_STARTED.register(a -> {
			new Thread(() -> {
				long l = System.currentTimeMillis();
				//noinspection StatementWithEmptyBody
				while (System.currentTimeMillis() - l < 500);
				hotJoinC2SThread.runTask(v -> v.send(new WindowOpenedPayload()));
			}).start();
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(a -> {
			hotJoinC2SThread.runTask(v -> v.send(new ClosingPayload()));
		});
	}

	private static void join(ServerData serverData) {
		ConnectScreen.startConnecting(new TitleScreen(), Minecraft.getInstance(), ServerAddress.parseString(serverData.ip), serverData, false, null);
	}
}
