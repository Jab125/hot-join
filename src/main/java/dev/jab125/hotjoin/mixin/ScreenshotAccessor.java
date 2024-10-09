package dev.jab125.hotjoin.mixin;

import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.File;

@Mixin(Screenshot.class)
public interface ScreenshotAccessor {
	@Invoker("getFile")
	public static File getFile(File file) {
		throw new RuntimeException();
	}
}
