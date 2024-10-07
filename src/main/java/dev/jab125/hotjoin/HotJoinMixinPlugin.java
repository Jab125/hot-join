package dev.jab125.hotjoin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

public class HotJoinMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {

	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		// stop annoying realms crash
		if (FabricLoader.getInstance().isModLoaded("legacy") && FabricLoader.getInstance().isDevelopmentEnvironment()) {
			if (targetClassName.contains("RealmsAvailability")) {
				targetClass.fields.stream().filter(a -> a.name.equals("future")).forEach(fieldNode -> {
					if (Modifier.isPrivate(fieldNode.access)) {
						fieldNode.access -= Opcodes.ACC_PRIVATE;
						fieldNode.access += Opcodes.ACC_PUBLIC;
					}
				});
			}
		}
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}
}
