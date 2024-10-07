package dev.jab125.hotjoin;

import com.google.common.io.ByteStreams;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.compat.IModCompat;
import dev.jab125.hotjoin.compat.authme.IAuthMeModCompat;
import dev.jab125.hotjoin.compat.authme.AuthMeCompat;
import dev.jab125.hotjoin.compat.legacy4j.ILegacy4JModCompat;
import dev.jab125.hotjoin.compat.legacy4j.Legacy4JModCompat;
import dev.jab125.hotjoin.packet.AlohaPayload;
import dev.jab125.hotjoin.packet.SteamPayload;
import dev.jab125.hotjoin.util.HotJoinCodecs;
import net.deechael.concentration.Concentration;
import net.deechael.concentration.FullscreenMode;
import net.deechael.concentration.fabric.config.ConcentrationConfigFabric;
import net.deechael.concentration.mixin.accessor.WindowAccessor;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

import static java.util.function.Predicate.not;

public class HotJoin {
	public static final String MOD_ID = "hotjoin";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ArrayList<UUID> INSTANCES = new ArrayList<>();
	public static final HashMap<UUID, ServerPlayer> uuidPlayerMap = new HashMap<>();
	public static IAuthMeModCompat authMeCompat;
	public static ILegacy4JModCompat legacy4JModCompat;

	private Wrapped wrapped = null;
	byte[] bytes;
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		if (FabricLoader.getInstance().isModLoaded("authme")) {
			authMeCompat = new AuthMeCompat();
		} else {
			authMeCompat = null;
		}
		if (FabricLoader.getInstance().isModLoaded("legacy")) {
			legacy4JModCompat = new Legacy4JModCompat();
		} else {
			legacy4JModCompat = null;
		}
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			var command = ClientCommandManager.literal("hotjoin");
			if (FabricLoader.getInstance().isModLoaded("authme")) {
				command.then(ClientCommandManager.literal("authme").then(ClientCommandManager.literal("microsoft").executes(authMeCompat::hotJoinAuthMeMicrosoft)));
			}

			if (FabricLoader.getInstance().isModLoaded("legacy")) {
				command.then(ClientCommandManager.literal("legacy4j").executes(legacy4JModCompat::hotJoinLegacy4J));
			}

			if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
				command.executes(this::hotJoin);
			}

			dispatcher.register(command);
		});
		//noinspection removal
		PayloadTypeRegistry.playC2S().register(dev.jab125.hotjoin.packet.KidneyPayload.TYPE, dev.jab125.hotjoin.packet.KidneyPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(AlohaPayload.TYPE, AlohaPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SteamPayload.TYPE, SteamPayload.STREAM_CODEC);

		ClientPlayNetworking.registerGlobalReceiver(SteamPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				if (wrapped == null) wrapped = wrap(Minecraft.getInstance());
				int val = payload.val();
				switch (payload.in()) {
					case Instructions.WIDTH -> wrapped.width(val);
					case Instructions.HEIGHT -> wrapped.height(val);
					case Instructions.X -> wrapped.x(val);
					case Instructions.Y -> wrapped.y(val);
					case Instructions.APPLY -> {
						wrapped.apply();
						wrapped = null;
					}
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(AlohaPayload.TYPE, (payload, context) -> {
			if (INSTANCES.contains(payload.uuid())) {
				uuidPlayerMap.put(payload.uuid(), context.player());
				arrangeWindows();
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Optional<Map.Entry<UUID, ServerPlayer>> first = uuidPlayerMap.entrySet().stream().filter(a -> a.getValue() == handler.player).findFirst();
			if (first.isPresent()) {
				UUID key = first.get().getKey();
				INSTANCES.remove(key);
				uuidPlayerMap.remove(key);
			}
			arrangeWindows();
		});


		boolean hotjoinClient = System.getProperty("hotjoin.client", "false").equals("true");
		String hotjoinServer = System.getProperty("hotjoin.server", "");
		String t = System.getProperty("hotjoin.uuid", "");
		UUID hotjoinUUID = t.isEmpty() ? null : UUID.fromString(t);
		boolean[] firstTime = new boolean[]{true, true};
		String magic = System.getProperty("hotjoin.magic", "");
		String compatString = System.getProperty("hotjoin.compat", "authme");
		IModCompat compat = "authme".equals(compatString) ? authMeCompat : "legacy4j".equals(compatString) ? legacy4JModCompat : null;

		if (hotjoinClient) {
			ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
				if (screen instanceof TitleScreen || screen instanceof AccessibilityOnboardingScreen) {
					if (firstTime[0]) firstTime[0] = false;
					else {
						if (firstTime[1]) {
							firstTime[1] = false;
							if (!magic.isEmpty()) compat.setSession(HotJoinCodecs.USER_CODEC.decode(NbtOps.INSTANCE, crashgoByeBye(() ->NbtIo.read(ByteStreams.newDataInput(Base64.getDecoder().decode(magic.replace("$", "=")))))).resultOrPartial(LOGGER::error).orElseThrow().getFirst());
						}
						this.join(new ServerData("A Minecraftc nk∆∆i¶•†¥", hotjoinServer, ServerData.Type.LAN));
					}
				}
			});

			ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
				sender.sendPacket(new AlohaPayload(hotjoinUUID));
			});
			ClientPlayConnectionEvents.DISCONNECT.register((clientPacketListener,c) -> {
				c.stop();
			});
		}
	}

	@SuppressWarnings("SequencedCollectionMethodCanBeUsed")
	public static void arrangeWindows() {
		Wrapped wrap = wrap(Minecraft.getInstance());
		ArrayList<Wrapped> wrappeds = new ArrayList<>();
		wrappeds.add(wrap);
		for (UUID instance : uuidPlayerMap.keySet()) {
			wrappeds.add(wrap(instance));
		}
		// get monitor width and height
		Monitor bestMonitor = ((WindowAccessor) (Object) Minecraft.getInstance().getWindow()).getScreenManager().findBestMonitor(Minecraft.getInstance().getWindow());
		assert bestMonitor != null;
		int width = bestMonitor.getMode(0).getWidth();
		int height = bestMonitor.getMode(0).getHeight();

		if (wrappeds.size() == 1) {
			Wrapped wrapped = wrappeds.get(0);
			wrapped.x(0);
			wrapped.y(0);
			wrapped.width(width);
			wrapped.height(height);
			wrapped.apply();
		} else if (wrappeds.size() == 2) {
			Wrapped wrapped = wrappeds.get(0);
			wrapped.x(0);
			wrapped.y(0);
			wrapped.width(width);
			wrapped.height(height / 2);
			wrapped.apply();

			wrapped = wrappeds.get(1);
			wrapped.x(0);
			wrapped.y(height / 2);
			wrapped.width(width);
			wrapped.height(height / 2);
			wrapped.apply();
		}
	}

	interface Wrapped {
		int getId(); // support up to 4
		void width(int width);
		void height(int height);
		void x(int x);
		void y(int y);
		void apply();
	}
	private static Wrapped wrap(Minecraft minecraft) {
		return new Wrapped() {
			@Override
			public int getId() {
				return 0;
			}

			private int width = Integer.MIN_VALUE;
			private int height = Integer.MIN_VALUE;
			private int x = Integer.MIN_VALUE;
			private int y = Integer.MIN_VALUE;
			@Override
			public void width(int width) {
				this.width = width;
			}

			@Override
			public void height(int height) {
				this.height = height;
			}

			@Override
			public void x(int x) {
				this.x = x;
			}

			@Override
			public void y(int y) {
				this.y = y;
			}

			@Override
			public void apply() {
				ConcentrationConfigFabric instance = ConcentrationConfigFabric.getInstance();
				instance.fullscreen = FullscreenMode.BORDERLESS;
				instance.customized = true;
				if (x != Integer.MIN_VALUE) {
					instance.x = x;
					x = Integer.MIN_VALUE;
				}
				if (y != Integer.MIN_VALUE) {
					instance.y = y;
					y = Integer.MIN_VALUE;
				}
				if (width != Integer.MIN_VALUE) {
					instance.width = width;
					width = Integer.MIN_VALUE;
				}
				if (height != Integer.MIN_VALUE) {
					instance.height = height;
					height = Integer.MIN_VALUE;
				}
				instance.save();
				RenderSystem.recordRenderCall(() -> Concentration.toggleFullScreenMode(Minecraft.getInstance().options, true));
			}
		};
	}

	private static class Instructions {
		public static final int WIDTH = 0;
		public static final int HEIGHT = 1;
		public static final int X = 2;
		public static final int Y = 3;
		private static final int APPLY = 4;
	}
	private static Wrapped wrap(UUID player) {
		ServerPlayer serverPlayer = uuidPlayerMap.get(player);
		return new Wrapped() {
			@Override
			public int getId() {
				return INSTANCES.indexOf(player);
			}
			private void send(int a, int b) {
				ServerPlayNetworking.send(serverPlayer, new SteamPayload(a, b));
			}

			@Override
			public void width(int width) {
				send(Instructions.WIDTH, width);
			}

			@Override
			public void height(int height) {
				send(Instructions.HEIGHT, height);
			}

			@Override
			public void x(int x) {
				send(Instructions.X, x);
			}

			@Override
			public void y(int y) {
				send(Instructions.Y, y);
			}

			@Override
			public void apply() {
				send(Instructions.APPLY, 0);
			}
		};
	}

	@FunctionalInterface
	interface SupplierButBetter<T, R extends Throwable> {
		T get() throws R;
	}

	@SuppressWarnings({"RedundantCast", "unchecked"})
	private <T, R extends Throwable> T crashgoByeBye(SupplierButBetter<T, R> t) {
		return ((SupplierButBetter<T, RuntimeException>) t).get();
	}


	private void join(ServerData serverData) {
		ConnectScreen.startConnecting(new TitleScreen(), Minecraft.getInstance(), ServerAddress.parseString(serverData.ip), serverData, false, null);
	}




	private int hotJoin(CommandContext<FabricClientCommandSource> a) {
		try {
			launchMinecraftClient(null, null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return 0;
	}

	// The goal is to launch Minecraft a second time, under a different directory.
	public static void launchMinecraftClient(String compat, String magic) throws IOException {
		if (magic != null) magic = magic.replace("=", "$");
		UUID uuid = UUID.randomUUID();
		INSTANCES.add(uuid);
		IntegratedServer singleplayerServer = Minecraft.getInstance().getSingleplayerServer();
		singleplayerServer.publishServer(singleplayerServer.getDefaultGameType(), singleplayerServer.getPlayerList().isAllowCommandsForAllPlayers(), 3600);
		String[] launchArguments = FabricLoader.getInstance().getLaunchArguments(false);
		int i = 0;
		for (String launchArgument : launchArguments) {
			if (launchArgument.equals("--gameDir")) {
				break;
			}
			i++;
		}
		Path path = Path.of("second");
		path.toFile().mkdirs();
		launchArguments[i + 1] = path.toAbsolutePath().toString();
		String cp = System.getProperty("java.class.path");
		List<String> splitClassPath = Arrays.stream(cp.split(File.pathSeparator)).toList();
		String addMods = "-Dfabric.addMods=";
		ArrayList<String> fjio = new ArrayList<>();
		for (ModContainer allMod : FabricLoader.getInstance().getAllMods()) {
			if (allMod.getOrigin().getKind() == ModOrigin.Kind.PATH) {
				String id = allMod.getMetadata().getId();
				if (id.equals("minecraft") || id.equals("java") || id.equals("fabricloader") || id.equals("mixinextras"))
					continue;
				String[] array = allMod.getOrigin().getPaths().stream().map(Path::toAbsolutePath).map(Path::toString).filter(not(splitClassPath::contains)).toArray(String[]::new);
				fjio.addAll(Arrays.asList(array));
			}
		}
		addMods += String.join(File.pathSeparator, fjio);
		var l = new String[0];
		l = ArrayUtils.addAll(l, "java");
		if (Minecraft.ON_OSX) l = ArrayUtils.addAll(l, "-XstartOnFirstThread");
		if (!System.getProperty("fabric.remapClasspathFile", "").isEmpty()) l = ArrayUtils.addAll(l, "-Dfabric.remapClasspathFile=" + System.getProperty("fabric.remapClasspathFile"));
		l = ArrayUtils.addAll(l, "-Dhotjoin.client=true", "-Dhotjoin.server=localhost:" + singleplayerServer.getPort(), addMods);
		l = ArrayUtils.addAll(l,
				"-Dhotjoin.uuid=" + uuid
		);
		if (compat != null) l = ArrayUtils.addAll(l, "-Dhotjoin.compat=" + compat);
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) l = ArrayUtils.addAll(l, "-Dfabric.development=true");
		if (magic != null) l = ArrayUtils.addAll(l, "-Dhotjoin.magic=" + magic);
		l = ArrayUtils.addAll(l, "-cp", cp, "net.fabricmc.loader.impl.launch.knot.KnotClient");
		l = ArrayUtils.addAll(l, launchArguments);
		ProcessBuilder exec = new ProcessBuilder().directory(path.toFile()).command(l).redirectOutput(ProcessBuilder.Redirect.INHERIT)
				.redirectError(ProcessBuilder.Redirect.INHERIT);
		exec.start();
	}
}