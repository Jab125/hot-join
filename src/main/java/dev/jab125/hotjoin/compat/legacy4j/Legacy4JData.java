package dev.jab125.hotjoin.compat.legacy4j;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Legacy4JData(String controllerName, int oldControllerIndex, int controllerIndex, int selectedControllerHandler) {
	public static final Codec<Legacy4JData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("controllerName").forGetter(Legacy4JData::controllerName),
			Codec.INT.fieldOf("oldControllerIndex").forGetter(Legacy4JData::oldControllerIndex),
			Codec.INT.fieldOf("controllerIndex").forGetter(Legacy4JData::controllerIndex),
			Codec.INT.fieldOf("selectedControllerHandler").forGetter(Legacy4JData::selectedControllerHandler)
	).apply(instance, Legacy4JData::new));
}
