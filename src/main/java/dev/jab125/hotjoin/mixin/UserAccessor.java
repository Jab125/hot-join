package dev.jab125.hotjoin.mixin;

import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(User.class)
public interface UserAccessor {
	@Accessor("uuid")
	UUID getUUID();
}
