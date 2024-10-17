package dev.jab125.hotjoin.compat.legacy4j;

import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.compat.IModCompat;
import dev.jab125.hotjoin.packet.AlohaPayload;
import dev.jab125.hotjoin.packet.SdlNativesPayload;
import dev.jab125.hotjoin.server.HotJoinS2CThread;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.UUID;

public interface ILegacy4JModCompat extends IModCompat {
	int hotJoinLegacy4J(CommandContext<FabricClientCommandSource> context);
	@Override
	void setSession(User user);
	void sendLegacy4jData(String data);
	void joinedWorld();
	void leftWorld(UUID uuid);
	void connectionEstablished(HotJoinS2CThread thread, AlohaPayload payload, UUID uuid);
	void receivedSdlNatives(SdlNativesPayload payload);
	void renderUsername(GuiGraphics graphics);
	void onBeginScreenSet(Screen previousScreen, Screen newScreen);
}
