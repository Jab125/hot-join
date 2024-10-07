package dev.jab125.hotjoin.compat.legacy4j;

import com.mojang.brigadier.context.CommandContext;
import dev.jab125.hotjoin.compat.IModCompat;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.User;

import java.util.UUID;

public interface ILegacy4JModCompat extends IModCompat {
	int hotJoinLegacy4J(CommandContext<FabricClientCommandSource> context);
	@Override
	void setSession(User user);
	void sendLegacy4jData(String data);
	void joinedWorld();
	void leftWorld(UUID uuid);
}
