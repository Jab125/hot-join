package dev.jab125.hotjoin.compat.legacy4j;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.HotJoin;
import dev.jab125.hotjoin.api.HotJoinAccess;
import dev.jab125.hotjoin.util.AuthCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.screen.ChooseUserScreen;
import wily.legacy.util.MCAccount;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

import static dev.jab125.hotjoin.HotJoin.crashgoByeBye;

public class Legacy4JModCompat implements ILegacy4JModCompat {
	// used by MCAccountMixin due to the lack of mutable static fields in interfaces
	public static Consumer<String> authConsumer;
	public static final HashMap<UUID, Legacy4JData> uuidLegacy4JMap = new HashMap<>();
	public Legacy4JModCompat() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			ScreenEvents.afterRender(screen).register((screen1, graphics, mouseX, mouseY, tickDelta) -> {
				if (screen instanceof ChooseUserScreen sc && sc instanceof AuthCallback c) {
					if (c.hotjoin$legacy4jData() instanceof Legacy4JData data) {
						graphics.drawCenteredString(client.font, "Joining with " + data.controllerName(), screen.width / 2, 15, 0xffffffff);
					}
				}
			});
		});
	}
	@Override
	public int hotJoinLegacy4J(CommandContext<FabricClientCommandSource> context) {
		HotJoin.canLaunchOtherwiseThrow();
		context.getSource().getClient().tell(Legacy4JModCompat::openLegacy4JUserPicker);
		return 0;
	}

	public static void openLegacy4JUserPicker() {
		openLegacy4JUserPicker(null);
	}

	public static void openLegacy4JUserPicker(Legacy4JData data) {
		ChooseUserScreen chooseUserScreen = new ChooseUserScreen(null);
		((AuthCallback)chooseUserScreen).hotjoin$authResponse(s -> {
			Minecraft.getInstance().getToasts().addToast(new LegacyTip(Component.literal("Success, joining world...")));
			launchLegacy4jClient(s, data);
		});
		((AuthCallback) chooseUserScreen).hotjoin$legacy4jData(data);
		Minecraft.getInstance().setScreen(chooseUserScreen);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void launchLegacy4jClient(String magic, Legacy4JData legacy4JData) throws T {
		String data;
		if (legacy4JData != null) {
			Tag tag = Legacy4JData.CODEC.encodeStart(NbtOps.INSTANCE, legacy4JData).resultOrPartial(HotJoin.LOGGER::error).orElseThrow();
			ByteArrayDataOutput byteArrayDataOutput = ByteStreams.newDataOutput();
			try {
				NbtIo.write((CompoundTag) tag, byteArrayDataOutput);
			} catch (IOException e) {
				throw (T) e;
			}
			data = Base64.getEncoder().encodeToString(byteArrayDataOutput.toByteArray());
		} else {
			data = null;
		}
		UUID uuid = HotJoinAccess.launchMinecraftClient("legacy4j", magic, data);
		if (legacy4JData != null) uuidLegacy4JMap.put(uuid, legacy4JData);
	}

	@Override
	public void setSession(User user) {
		MCAccount.setUser(user);
	}

	// this should be null for the main client!
	public static Legacy4JData legacy4JData;
	@Override
	public void sendLegacy4jData(String data) {
		legacy4JData = Legacy4JData.CODEC.decode(NbtOps.INSTANCE, crashgoByeBye(() -> NbtIo.read(ByteStreams.newDataInput(Base64.getDecoder().decode(data.replace("$", "=")))))).resultOrPartial(HotJoin.LOGGER::error).orElseThrow().getFirst();
	}

	@Override
	public void joinedWorld() {
		LegacyOptions legacyOptions = ScreenUtil.getLegacyOptions();
		if (legacy4JData != null) {
			legacyOptions.selectedController().set(legacy4JData.controllerIndex());
			legacyOptions.selectedControllerHandler().set(legacy4JData.selectedControllerHandler());
		}
		legacyOptions.unfocusedInputs().set(true);
		Minecraft.getInstance().options.pauseOnLostFocus = false;
		Minecraft.getInstance().options.save();
	}

	@Override
	public void leftWorld(UUID uuid) {
		uuidLegacy4JMap.remove(uuid);
	}
}
