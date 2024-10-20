package dev.jab125.hotjoin.compat.authme;

import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.HotJoin;
import dev.jab125.hotjoin.api.HotJoinAccess;
import dev.jab125.hotjoin.util.AuthCallback;
import me.axieum.mcmod.authme.api.util.SessionUtils;
import me.axieum.mcmod.authme.impl.gui.MicrosoftAuthScreen;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.util.UUID;

public class AuthMeCompat implements IAuthMeModCompat {
	@Override
	public int hotJoinAuthMeMicrosoft(CommandContext<FabricClientCommandSource> a) {
		HotJoin.canLaunchOtherwiseThrow();
		a.getSource().getClient().tell(() -> {
			MicrosoftAuthScreen microsoftAuthScreen = new MicrosoftAuthScreen(Minecraft.getInstance().screen, null, true);
			((AuthCallback) microsoftAuthScreen).hotjoin$authResponse(this::launchAuthMeClient);
			Minecraft.getInstance().setScreen(microsoftAuthScreen);
		});
		return 0;
	}

	@Override
	public UUID launchAuthMeClient(String uuid, String magic) {
		return HotJoinAccess.launchMinecraftClient("authme", magic, null, uuid);
	}

	@Override
	public void setSession(User user) {
		SessionUtils.setSession(user);
	}
}
