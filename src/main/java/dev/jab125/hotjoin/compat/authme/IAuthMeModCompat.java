package dev.jab125.hotjoin.compat.authme;

import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.compat.IModCompat;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.User;

public interface IAuthMeModCompat extends IModCompat {
	// HotJoin Microsoft
	int hotJoinAuthMeMicrosoft(CommandContext<FabricClientCommandSource> a);

	@Override
	void setSession(User user);
}
