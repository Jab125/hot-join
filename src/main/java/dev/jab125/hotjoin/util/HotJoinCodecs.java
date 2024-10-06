package dev.jab125.hotjoin.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jab125.hotjoin.mixin.UserAccessor;
import net.minecraft.client.User;
import net.minecraft.core.UUIDUtil;

public class HotJoinCodecs {
	public static final Codec<User> USER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("name").forGetter(User::getName),
			UUIDUtil.CODEC.fieldOf("uuid").forGetter(a -> ((UserAccessor) a).getUUID()),
			Codec.STRING.fieldOf("accessToken").forGetter(User::getAccessToken),
			Codec.STRING.optionalFieldOf("xuid").forGetter(User::getXuid),
			Codec.STRING.optionalFieldOf("clientId").forGetter(User::getClientId),
			Codec.STRING.xmap(User.Type::byName, User.Type::getName).fieldOf("type").forGetter(User::getType)
	).apply(instance, User::new));
}
