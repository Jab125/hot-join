package dev.jab125.hotjoin;

import com.google.common.io.ByteStreams;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.packet.AlohaPayload;
import dev.jab125.hotjoin.packet.SteamPayload;
import dev.jab125.hotjoin.packet.KidneyPayload;
import dev.jab125.hotjoin.util.AuthCallback;
import dev.jab125.hotjoin.util.HotJoinCodecs;
import me.axieum.mcmod.authme.api.util.SessionUtils;
import me.axieum.mcmod.authme.impl.gui.MicrosoftAuthScreen;
import net.deechael.concentration.Concentration;
import net.deechael.concentration.FullscreenMode;
import net.deechael.concentration.fabric.config.ConcentrationConfigFabric;
import net.deechael.concentration.mixin.accessor.WindowAccessor;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
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
	public static final String MOD_ID = "hot-join";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ResourceLocation AVABVB = ResourceLocation.parse("ava:bvb");
	public static final ArrayList<UUID> INSTANCES = new ArrayList<>();
	public static final HashMap<UUID, ServerPlayer> uuidPlayerMap = new HashMap<>();

	private Wrapped wrapped = null;
	byte[] bytes;
	public void onInitialize() throws IOException {
		System.out.println(System.getProperties());
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		//launchMinecraftClient();
		//QuickPlay quickPlay = new QuickPlay();
		LOGGER.info("Hello Fabric world!");
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			var command = ClientCommandManager.literal("hotjoin");
			if (FabricLoader.getInstance().isModLoaded("authme")) {
				Supplier<Supplier<Runnable>> f = () -> () -> () -> {
					command.then(ClientCommandManager.literal("authme").then(ClientCommandManager.literal("microsoft").executes(v -> hotJoin(true, v))));
				};
				f.get().get().run();
			}

			if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
				command.executes(v -> hotJoin(false, v));
			}
			var c = ClientCommandManager.literal("screenshot").executes(a -> {
				try {
					getMCWindowContents();
					return 0;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			dispatcher.register(c);
			dispatcher.register(command);
		});
//		ClientTickEvents.START_CLIENT_TICK.register(client -> {
//			hotJoin(null);
//		});
		PayloadTypeRegistry.playC2S().register(KidneyPayload.TYPE, KidneyPayload.STREAM_CODEC);
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

//		HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
//			if (bytes != null) {
//				DynamicTexture dynamicTexture = new DynamicTexture(crashgoByeBye(() -> NativeImage.read(bytes)));
//				Minecraft.getInstance().getTextureManager().register(AVABVB, dynamicTexture);
//				drawContext.blit(AVABVB, 0, 0, 100, 100, 0, 0);
//				dynamicTexture.close();
//			}
//		});

		boolean hotjoinClient = System.getProperty("hotjoin.client", "false").equals("true");
		String hotjoinServer = System.getProperty("hotjoin.server", "");
		long hotjoinWindow = Long.parseLong(System.getProperty("hotjoin.window", "0"));
		String t = System.getProperty("hotjoin.uuid", "");
		UUID hotjoinUUID = t.isEmpty() ? null : UUID.fromString(t);
		boolean[] firstTime = new boolean[]{true, true};
		String magic = System.getProperty("hotjoin.magic", "");
		if (hotjoinClient) {
			ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
				if (screen instanceof TitleScreen || screen instanceof AccessibilityOnboardingScreen) {
					if (firstTime[0]) firstTime[0] = false;
					else {
						if (firstTime[1]) {
							firstTime[1] = false;
							SessionUtils.setSession(HotJoinCodecs.USER_CODEC.decode(NbtOps.INSTANCE, crashgoByeBye(() ->NbtIo.read(ByteStreams.newDataInput(Base64.getDecoder().decode(magic.replace("$", "=")))))).resultOrPartial(LOGGER::error).orElseThrow().getFirst());
						}
						this.join(new ServerData("A Minecraftc nk∆∆i¶•†¥", hotjoinServer, ServerData.Type.LAN));
					}
				}
			});

//			ClientPlayConnectionEvents.INIT.register((handler, client) -> {
//				//ClientPlayNetworking.send(new AlohaPayload());
//
//			});
			ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
				sender.sendPacket(new AlohaPayload(hotjoinUUID));
			});


//			ClientTickEvents.END_CLIENT_TICK.register(client -> {
//				if (client.level != null) {
//					//Screenshot.grab();
//					// if this leaks, well...
//					byte[] mcWindowContents = crashgoByeBye(this::getMCWindowContents);
//					ClientPlayNetworking.send(new KidneyPayload(mcWindowContents));
//				}
//			});
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

	private int hotJoin(boolean michaelsoft, CommandContext<FabricClientCommandSource> a) {
		try {
			if (michaelsoft) {
				a.getSource().getClient().tell(() -> {
					MicrosoftAuthScreen microsoftAuthScreen = new MicrosoftAuthScreen(Minecraft.getInstance().screen, null, true);
					((AuthCallback) microsoftAuthScreen).hotjoin$authResponse(s -> {
						try {
							System.out.println("Got a response!: " + s);
							launchMinecraftClient(s);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
					Minecraft.getInstance().setScreen(microsoftAuthScreen);
				});
			} else {
				launchMinecraftClient(null);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return 0;
	}

	private byte[] getMCWindowContents() throws IOException {
		Window window = Minecraft.getInstance().getWindow();
		int height = window.getHeight();
		int width = window.getWidth();
		NativeImage nativeImage = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
		//nativeImage.writeToFile(Path.of("test.png"));
		return nativeImage.asByteArray();
	}

	// The goal is to launch Minecraft a second time, under a different directory.
	private void launchMinecraftClient(String magic) throws IOException {
		if (magic != null) magic = magic.replace("=", "$");
		UUID uuid = UUID.randomUUID();
		INSTANCES.add(uuid);
		IntegratedServer singleplayerServer = Minecraft.getInstance().getSingleplayerServer();
		singleplayerServer.publishServer(singleplayerServer.getDefaultGameType(), singleplayerServer.getPlayerList().isAllowCommandsForAllPlayers(), 3600);
		//String ip = Minecraft.getInstance().getConnection().getServerData().ip;
		//System.out.println(ip);
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
		//KnotClient
		String cp = System.getProperty("java.class.path");
		List<String> splitClassPath = Arrays.stream(cp.split(File.pathSeparator)).toList();
		String addMods = "-Dfabric.addMods=";
		ArrayList<String> fjio = new ArrayList<>();
		for (ModContainer allMod : FabricLoader.getInstance().getAllMods()) {
			//System.out.println(allMod.getMetadata().getId() + ": " + allMod.getOrigin().getKind() + ", " + allMod.getOrigin().getPaths() + ", " + allMod.getRootPaths());
			if (allMod.getOrigin().getKind() == ModOrigin.Kind.PATH) {
				String id = allMod.getMetadata().getId();
				if (id.equals("minecraft") || id.equals("java") || id.equals("fabricloader") || id.equals("mixinextras"))
					continue;
				String[] array = allMod.getOrigin().getPaths().stream().map(Path::toAbsolutePath).map(Path::toString).filter(not(splitClassPath::contains)).toArray(String[]::new);
				fjio.addAll(Arrays.asList(array));
//				System.out.println(Arrays.toString(array));
//				addMods += String.join(File.pathSeparator, array);
			}
		}
		addMods += String.join(File.pathSeparator, fjio);
		//FabricLoaderImpl
		//addMods = addMods.substring(0, addMods.length() - 1);
		System.out.println(addMods);
		var l = new String[0]; //new String[]{"java", "-XstartOnFirstThread", "-cp", cp, "net.fabricmc.loader.impl.launch.knot.KnotClient"};
		//LoaderUtil
		l = ArrayUtils.addAll(l, "java");
		if (Minecraft.ON_OSX) l = ArrayUtils.addAll(l, "-XstartOnFirstThread");
		if (!System.getProperty("fabric.remapClasspathFile", "").isEmpty()) l = ArrayUtils.addAll(l, "-Dfabric.remapClasspathFile=" + System.getProperty("fabric.remapClasspathFile"));
		l = ArrayUtils.addAll(l, "-Dhotjoin.client=true", "-Dhotjoin.server=localhost:" + singleplayerServer.getPort(), addMods);
		l = ArrayUtils.addAll(l,
				"-Dhotjoin.window=" + Minecraft.getInstance().getWindow().getWindow(),
				"-Dhotjoin.uuid=" + uuid
		);
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) ArrayUtils.addAll(l, "-Dfabric.development=true");
		if (magic != null) l = ArrayUtils.addAll(l, "-Dhotjoin.magic=" + magic);
		l = ArrayUtils.addAll(l, "-cp", cp, "net.fabricmc.loader.impl.launch.knot.KnotClient");
		l = ArrayUtils.addAll(l, launchArguments);
		//l = ArrayUtils.addAll(l, "--quickPlayMultiplayer", "hotjoin-lanlocalhost:" + singleplayerServer.getPort() /*"localhost:%s".formatted(singleplayerServer.getPort())*/);
		ProcessBuilder exec = new ProcessBuilder().directory(path.toFile()).command(l).redirectOutput(ProcessBuilder.Redirect.INHERIT)
				.redirectError(ProcessBuilder.Redirect.INHERIT);
		exec.start();
		//System.out.println("STARTED");
		System.out.println(Arrays.toString(launchArguments));
		//QuickPlay.connect();
	}
}