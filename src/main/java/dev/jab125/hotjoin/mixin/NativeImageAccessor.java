package dev.jab125.hotjoin.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.channels.WritableByteChannel;

@Mixin(NativeImage.class)
public interface NativeImageAccessor {
	@Invoker("writeToChannel")
	boolean callwriteToChannel(WritableByteChannel channel);
}
