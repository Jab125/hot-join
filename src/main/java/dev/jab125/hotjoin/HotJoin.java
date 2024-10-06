package dev.jab125.hotjoin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.packet.KidneyPayload;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.knot.Knot;
import net.fabricmc.loader.impl.launch.knot.KnotClient;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.server.LanServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.PublishCommand;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class HotJoin {
	public static final String MOD_ID = "hot-join";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ResourceLocation AVABVB = ResourceLocation.parse("ava:bvb");


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
			command.executes(this::hotJoin);
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

		ServerPlayNetworking.registerGlobalReceiver(KidneyPayload.TYPE, (payload, context) -> {
			//System.out.println("Received kidney, size " + payload.b().length);
//			if (bytes == null)
//			bytes = payload.b();
//			NativeImage nativeImage = crashgoByeBye(() -> NativeImage.read(payload.b()));
//			if (currentNativeImage[0] != null) currentNativeImage[0].close(); // don't want to leak 60 images a second
//			currentNativeImage[0] = nativeImage;
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
		boolean[] firstTime = new boolean[]{true};
		if (hotjoinClient) {
			ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
				if (screen instanceof TitleScreen || screen instanceof AccessibilityOnboardingScreen) {
					if (firstTime[0]) firstTime[0] = false;
					else {
						this.join(new ServerData("A Minecraftc nk∆∆i¶•†¥", hotjoinServer, ServerData.Type.LAN));
					}
				}
			});
			ClientTickEvents.END_CLIENT_TICK.register(client -> {
				if (client.level != null) {
					// if this leaks, well...
					byte[] mcWindowContents = crashgoByeBye(this::getMCWindowContents);
					ClientPlayNetworking.send(new KidneyPayload(mcWindowContents));
				}
			});
		}
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
			launchMinecraftClient();
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
	private void launchMinecraftClient() throws IOException {
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
		l = ArrayUtils.addAll(l, "java", "-XstartOnFirstThread");
		if (!System.getProperty("fabric.remapClasspathFile", "").isEmpty()) l = ArrayUtils.addAll(l, "-Dfabric.remapClasspathFile=" + System.getProperty("fabric.remapClasspathFile"));
		l = ArrayUtils.addAll(l, "-Dhotjoin.client=true", "-Dhotjoin.server=localhost:" + singleplayerServer.getPort(), "-Dfabric.development=true", addMods);
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