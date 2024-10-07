package dev.jab125.hotjoin.compat.legacy4j;

import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.api.HotJoinAccess;
import dev.jab125.hotjoin.util.AuthCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.network.chat.Component;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.screen.ChooseUserScreen;
import wily.legacy.util.MCAccount;

import java.util.function.Consumer;

public class Legacy4JModCompat implements ILegacy4JModCompat {
	// used by MCAccountMixin due to the lack of mutable static fields in interfaces
	public static Consumer<String> authConsumer;
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
			launchLegacy4jClient(s);
		});
		((AuthCallback) chooseUserScreen).hotjoin$legacy4jData(data);
		Minecraft.getInstance().setScreen(chooseUserScreen);
	}

	private static void launchLegacy4jClient(String magic) {
		HotJoinAccess.launchMinecraftClient("legacy4j", magic);
	}

	@Override
	public void setSession(User user) {
		MCAccount.setUser(user);
	}
}
