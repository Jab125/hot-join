package dev.jab125.hotjoin.compat.authme;

import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.api.HotJoinAccess;
import dev.jab125.hotjoin.util.AuthCallback;
import me.axieum.mcmod.authme.api.util.SessionUtils;
import me.axieum.mcmod.authme.impl.gui.MicrosoftAuthScreen;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.util.function.Supplier;

public class AuthMeCompat implements IAuthMeModCompat {
	@Override
	public int hotJoinAuthMeMicrosoft(CommandContext<FabricClientCommandSource> a) {
		a.getSource().getClient().tell(() -> {
			Supplier<Supplier<Runnable>> sp = () -> () -> () -> {
				MicrosoftAuthScreen microsoftAuthScreen = new MicrosoftAuthScreen(Minecraft.getInstance().screen, null, true);
				((AuthCallback) microsoftAuthScreen).hotjoin$authResponse(AuthMeCompat::launchAuthMeClient);
				Minecraft.getInstance().setScreen(microsoftAuthScreen);
			};
			sp.get().get().run();
		});
		return 0;
	}

	private static void launchAuthMeClient(String magic) {
		HotJoinAccess.launchMinecraftClient("authme", magic);
	}

	@Override
	public void setSession(User user) {
		SessionUtils.setSession(user);
	}
}
