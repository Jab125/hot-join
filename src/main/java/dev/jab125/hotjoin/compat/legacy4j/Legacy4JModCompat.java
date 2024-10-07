package dev.jab125.hotjoin.compat.legacy4j;

import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.api.HotJoinAccess;
import dev.jab125.hotjoin.util.AuthCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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
	@Override
	public int hotJoinLegacy4J(CommandContext<FabricClientCommandSource> context) {
		context.getSource().getClient().tell(() -> {
			ChooseUserScreen chooseUserScreen = new ChooseUserScreen(null);
			((AuthCallback)chooseUserScreen).hotjoin$authResponse(s -> {
				Minecraft.getInstance().getToasts().addToast(new LegacyTip(Component.literal("Success, joining world...")));
				launchLegacy4jClient(s);
			});
			Minecraft.getInstance().setScreen(chooseUserScreen);
		});
		return 0;
	}

	private static void launchLegacy4jClient(String magic) {
		HotJoinAccess.launchMinecraftClient("legacy4j", magic);
	}

	@Override
	public void setSession(User user) {
		MCAccount.setUser(user);
	}
}
