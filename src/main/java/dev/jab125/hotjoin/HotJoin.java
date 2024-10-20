package dev.jab125.hotjoin;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.client.render.SharedRendering;
import dev.jab125.hotjoin.compat.authme.IAuthMeModCompat;
import dev.jab125.hotjoin.compat.authme.AuthMeCompat;
import dev.jab125.hotjoin.compat.controlify.ControlifyModCompat;
import dev.jab125.hotjoin.compat.controlify.IControlifyModCompat;
import dev.jab125.hotjoin.compat.legacy4j.ILegacy4JModCompat;
import dev.jab125.hotjoin.compat.legacy4j.Legacy4JModCompat;
import dev.jab125.hotjoin.packet.SteamPayload;
import dev.jab125.hotjoin.server.HotJoinS2CThread;
import net.deechael.concentration.Concentration;
import net.deechael.concentration.FullscreenMode;
import net.deechael.concentration.fabric.config.ConcentrationConfigFabric;
import net.deechael.concentration.mixin.accessor.WindowAccessor;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static java.util.function.Predicate.not;

public class HotJoin {
	public static final String MOD_ID = "hotjoin";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ArrayList<UUID> INSTANCES = new ArrayList<>();
	public static final HashMap<UUID, HotJoinS2CThread> uuidPlayerMap = new HashMap<>();
	public static IAuthMeModCompat authMeCompat;
	public static ILegacy4JModCompat legacy4JModCompat;
	public static IControlifyModCompat controlifyModCompat;

	private Wrapped wrapped = null;
	public static boolean hotjoinClient;

	public static final int LIMIT = 4;
	public static boolean canLaunchAnotherClient() {
		int size = INSTANCES.size();
		size++; // account for the original client
		return size < LIMIT;
	}

	public static void canLaunchOtherwiseThrow() {
		if (!HotJoin.canLaunchAnotherClient()) throw new HotJoinPlayerLimitException("You have reached the max limit of " + HotJoin.LIMIT + " splitscreen instances!");
	}

	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		hotjoinClient = System.getProperty("hotjoin.client", "false").equals("true");

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
		if (FabricLoader.getInstance().isModLoaded("controlify")) {
			controlifyModCompat = new ControlifyModCompat();
			controlifyModCompat.init();
		} else {
			controlifyModCompat = null;
		}

		if (hotjoinClient) {
			HotJoinClientInit.init();
		} else {
			HotJoinServerInit.init();
		}
	}

	@SuppressWarnings("SequencedCollectionMethodCanBeUsed")
	public static void arrangeWindows() {
		Wrapped wrap = wrap(Minecraft.getInstance());
		ArrayList<Wrapped> wrappeds = new ArrayList<>();
		wrappeds.add(wrap);
		for (UUID instance : INSTANCES) {
			if (uuidPlayerMap.containsKey(instance)) {
				if (uuidPlayerMap.get(instance).isWindowReady) {
					wrappeds.add(wrap(instance));
				}
			} else {
				System.out.println("We don't have " + instance + "!");
			}
		}
		System.out.println("There are " + wrappeds.size() + " instances");
		// get monitor width and height
		Monitor bestMonitor = ((WindowAccessor) (Object) Minecraft.getInstance().getWindow()).getScreenManager().findBestMonitor(Minecraft.getInstance().getWindow());
		assert bestMonitor != null;
		int width = bestMonitor.getCurrentMode().getWidth();
		int height = bestMonitor.getCurrentMode().getHeight();
		int maxFps;
		if (wrappeds.size() >= 4) {
			maxFps = 30;
		} else if (wrappeds.size() >= 2) {
			maxFps = 60;
		} else {
			maxFps = 60;
		}
		for (Wrapped wrapped : wrappeds) {
			wrapped.capFPS(maxFps);
		}
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
		} else if (wrappeds.size() == 3) {
			Wrapped wrapped = wrappeds.get(0);
			wrapped.x(0);
			wrapped.y(0);
			wrapped.width(width / 2);
			wrapped.height(height / 2);
			wrapped.apply();

			wrapped = wrappeds.get(1);
			wrapped.x(width / 2);
			wrapped.y(0);
			wrapped.width(width / 2);
			wrapped.height(height / 2);
			wrapped.apply();

			wrapped = wrappeds.get(2);
			wrapped.x(0);
			wrapped.y(height / 2);
			wrapped.width(width);
			wrapped.height(height / 2);
			wrapped.apply();
		} else if (wrappeds.size() == 4) {
			Wrapped wrapped = wrappeds.get(0);
			wrapped.x(0);
			wrapped.y(0);
			wrapped.width(width / 2);
			wrapped.height(height / 2);
			wrapped.apply();

			wrapped = wrappeds.get(1);
			wrapped.x(width / 2);
			wrapped.y(0);
			wrapped.width(width / 2);
			wrapped.height(height / 2);
			wrapped.apply();

			wrapped = wrappeds.get(2);
			wrapped.x(0);
			wrapped.y(height / 2);
			wrapped.width(width / 2);
			wrapped.height(height / 2);
			wrapped.apply();

			wrapped = wrappeds.get(3);
			wrapped.x(width / 2);
			wrapped.y(height / 2);
			wrapped.width(width / 2);
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
		void capFPS(int maxFps);
		void apply();
	}
	public static Wrapped wrap(Minecraft minecraft) {
		return new Wrapped() {
			@Override
			public int getId() {
				return 0;
			}

			private int width = Integer.MIN_VALUE;
			private int height = Integer.MIN_VALUE;
			private int x = Integer.MIN_VALUE;
			private int y = Integer.MIN_VALUE;
			private int maxFps = Integer.MIN_VALUE;
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
			public void capFPS(int maxFps) {
				// TODO config
				// this.maxFps = maxFps;
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
				if (maxFps != Integer.MIN_VALUE) {
					Minecraft.getInstance().options.framerateLimit().set(maxFps);
					maxFps = Integer.MIN_VALUE;
				}
				instance.save();
				RenderSystem.recordRenderCall(() -> {
					Concentration.toggleFullScreenMode(Minecraft.getInstance().options, true);
					SharedRendering.scale = -1;
					SharedRendering.bestMonitor = null;
				});
			}
		};
	}

	static class Instructions {
		public static final int WIDTH = 0;
		public static final int HEIGHT = 1;
		public static final int X = 2;
		public static final int Y = 3;
		public static final int APPLY = 4;
		public static final int FPS = 5;
	}
	public static Wrapped wrap(UUID player) {
		HotJoinS2CThread serverPlayer = uuidPlayerMap.get(player);
		return new Wrapped() {
			@Override
			public int getId() {
				return INSTANCES.indexOf(player) + 1;
			}
			private void send(int a, int b) {
				serverPlayer.runTask(t -> t.send(new SteamPayload(a, b)));
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
			public void capFPS(int maxFps) {
				send(Instructions.FPS, maxFps);
			}

			@Override
			public void apply() {
				send(Instructions.APPLY, 0);
			}
		};
	}

	@FunctionalInterface
	public interface SupplierButBetter<T, R extends Throwable> {
		T get() throws R;
	}

	@SuppressWarnings({"RedundantCast", "unchecked"})
	public static  <T, R extends Throwable> T crashgoByeBye(SupplierButBetter<T, R> t) {
		return ((SupplierButBetter<T, RuntimeException>) t).get();
	}


	static int hotJoin(CommandContext<FabricClientCommandSource> a) {
		HotJoin.canLaunchOtherwiseThrow();
		try {
			launchMinecraftClient(null, null, null, null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return 0;
	}

	// The goal is to launch Minecraft a second time, under a different directory.
	public static UUID launchMinecraftClient(String compat, String magic, String legacy4jData, @Nullable String folderName) throws IOException {
		HotJoin.canLaunchOtherwiseThrow();
		if (folderName == null || folderName.isBlank()) folderName = "second";
		if (magic != null) magic = magic.replace("=", "$");
		if (legacy4jData != null) legacy4jData = legacy4jData.replace("=", "$");
		UUID uuid = UUID.randomUUID();
		INSTANCES.add(uuid);
		String server;
		IntegratedServer singleplayerServer = Minecraft.getInstance().getSingleplayerServer();
		if (singleplayerServer != null) {
			singleplayerServer.publishServer(singleplayerServer.getDefaultGameType(), singleplayerServer.getPlayerList().isAllowCommandsForAllPlayers(), 3600);
			server = "localhost:" + singleplayerServer.getPort();
		} else {
			ServerData currentServer = Minecraft.getInstance().getCurrentServer();
			assert currentServer != null;
			server = currentServer.ip;
		}

		String[] launchArguments = FabricLoader.getInstance().getLaunchArguments(false);
		int i = 0;
		for (String launchArgument : launchArguments) {
			if (launchArgument.equals("--gameDir")) {
				break;
			}
			i++;
		}
		Path path = Path.of(".hotjoin-instances/" + folderName);
		boolean directory = path.toFile().isDirectory();
		if (!directory) path.toFile().mkdirs();
		if (!directory) transfer(path);
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
		Path javaPath = null;
		{
			Path javaBinDir = LoaderUtil.normalizePath(Paths.get(System.getProperty("java.home"), "bin"));
			String[] executables = { "javaw.exe", "java.exe", "java" };

			for (String executable : executables) {
				Path jpath = javaBinDir.resolve(executable);

				if (Files.isRegularFile(jpath)) {
					javaPath = jpath;
					break;
				}
			}

			if (javaPath == null) throw new RuntimeException("Can't find java executable in " + javaBinDir + "!");
		}
		l = ArrayUtils.addAll(l, javaPath.toAbsolutePath().toString(), "-Xmx2G");
		if (Minecraft.ON_OSX) l = ArrayUtils.addAll(l, "-XstartOnFirstThread");
		if (!System.getProperty("fabric.classPathGroups", "").isEmpty()) l = ArrayUtils.addAll(l, "-Dfabric.classPathGroups=" + System.getProperty("fabric.classPathGroups"));
		if (!System.getProperty("fabric.remapClasspathFile", "").isEmpty()) l = ArrayUtils.addAll(l, "-Dfabric.remapClasspathFile=" + System.getProperty("fabric.remapClasspathFile"));
		l = ArrayUtils.addAll(l, "-Dhotjoin.client=true", "-Dhotjoin.server=" + server, addMods);
		l = ArrayUtils.addAll(l,
				"-Dhotjoin.uuid=" + uuid
		);
		if (compat != null) l = ArrayUtils.addAll(l, "-Dhotjoin.compat=" + compat);
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) l = ArrayUtils.addAll(l, "-Dfabric.development=true");
		if (magic != null) l = ArrayUtils.addAll(l, "-Dhotjoin.magic=" + magic);
		if (legacy4jData != null) l = ArrayUtils.addAll(l, "-Dhotjoin.legacy4jData=" + legacy4jData);
		l = ArrayUtils.addAll(l, "-cp", cp, "net.fabricmc.loader.impl.launch.knot.KnotClient");
		l = ArrayUtils.addAll(l, launchArguments);
		ProcessBuilder exec = new ProcessBuilder().directory(path.toFile()).command(l).redirectOutput(ProcessBuilder.Redirect.INHERIT)
				.redirectError(ProcessBuilder.Redirect.INHERIT);
		exec.start();
		return uuid;
	}

	// Transfer stuff into this folder
	private static void transfer(Path second) {
		PathTransfer pathTransfer = getPathTransfer(second);
		try {
			Minecraft.getInstance().options.save();
			if (legacy4JModCompat != null) legacy4JModCompat.saveAssorts();
			pathTransfer.copyFile(Path.of("resource_assorts.json"));
			pathTransfer.clearDirectoryRecursive(Path.of("resource_assorts"));
			pathTransfer.copyFolderRecursive(Path.of("resource_assorts"));
			pathTransfer.clearDirectoryRecursive(Path.of("resourcepacks"));
			pathTransfer.copyFolderRecursive(Path.of("resourcepacks"));

			pathTransfer.clearDirectoryRecursive(Path.of("config"));
			pathTransfer.copyFolderRecursive(Path.of("config"));

			pathTransfer.clearDirectoryRecursive(Path.of("defaultconfigs"));
			pathTransfer.copyFolderRecursive(Path.of("defaultconfigs"));

			pathTransfer.clearDirectoryRecursive(Path.of("shaderpacks"));
			pathTransfer.copyFolderRecursive(Path.of("shaderpacks"));

			pathTransfer.copyFile(Path.of("options.txt"));
		} catch (Throwable t) {

		}
	}

	private static @NotNull PathTransfer getPathTransfer(Path second) {
		Path gameDir = FabricLoader.getInstance().getGameDir();
		PathTransfer pathTransfer = new PathTransfer() {

			@Override
			public void clearDirectoryRecursive(Path path) throws IOException {
				Path resolve = second.resolve(path);
				PathUtils.deleteDirectory(resolve);
			}

			@Override
			public void copyFile(Path path) throws IOException {
				Path original = gameDir.resolve(path);
				if (!original.toFile().isFile()) return;
				Path resolve = second.resolve(path);
				Files.copy(original, resolve, StandardCopyOption.REPLACE_EXISTING);
			}

			@Override
			public void copyFolderRecursive(Path path) throws IOException {
				Path original = gameDir.resolve(path);
				if (!original.toFile().isDirectory()) return;
				Path resolve = second.resolve(path);
				PathUtils.copyDirectory(original, resolve);
			}
		};
		return pathTransfer;
	}

	interface PathTransfer {
		void clearDirectoryRecursive(Path path) throws Throwable;
		void copyFile(Path path) throws Throwable;
		void copyFolderRecursive(Path path) throws Throwable;
	}
}