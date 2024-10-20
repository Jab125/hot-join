package dev.jab125.hotjoin.compat.controlify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.ControllerInfo;

public record ControlifyData(String uid) {
	public static final Codec<ControlifyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("uid").forGetter(ControlifyData::uid)
	).apply(instance, ControlifyData::new));

	public ControlifyData(ControllerEntity entity) {
		this(entity.info().uid());
	}
}
