package dev.jab125.hotjoin.compat.controlify;

import dev.jab125.hotjoin.compat.IModCompat;
import dev.jab125.hotjoin.packet.AlohaPayload;
import dev.jab125.hotjoin.packet.ControlifyInfoPayload;
import dev.jab125.hotjoin.server.HotJoinS2CThread;
import net.minecraft.client.User;

import java.util.UUID;

public sealed interface IControlifyModCompat extends IModCompat permits ControlifyModCompat {
	@Override
	default void setSession(User user) {
		throw new IllegalStateException("This shouldn't be called!");
	}

	void init();

	void leftWorld(UUID o);

	void connectionEstablished(HotJoinS2CThread thread, AlohaPayload payload, UUID uuid);

	void receivedControlifyPayload(ControlifyInfoPayload payload);
}
