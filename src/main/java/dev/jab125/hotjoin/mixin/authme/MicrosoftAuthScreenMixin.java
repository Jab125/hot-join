package dev.jab125.hotjoin.mixin.authme;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.jab125.hotjoin.HotJoin;
import dev.jab125.hotjoin.util.AuthCallback;
import dev.jab125.hotjoin.util.HotJoinCodecs;
import me.axieum.mcmod.authme.api.gui.AuthScreen;
import me.axieum.mcmod.authme.impl.gui.MicrosoftAuthScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.User;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Base64;
import java.util.function.Consumer;

@Mixin(MicrosoftAuthScreen.class)
public abstract class MicrosoftAuthScreenMixin extends AuthScreen implements AuthCallback {
	public MicrosoftAuthScreenMixin() {
		super(null, null, null);
	}
	private @Unique @Nullable Consumer<String> authResponse;

	@Inject(method = "lambda$init$7", at = @At("HEAD"), cancellable = true)
	void d(User session, CallbackInfo ci) {
		try {
			if (authResponse == null) return;
			Tag tag = HotJoinCodecs.USER_CODEC.encodeStart(NbtOps.INSTANCE, session).resultOrPartial(HotJoin.LOGGER::error).orElseThrow();
			ByteArrayDataOutput byteArrayDataOutput = ByteStreams.newDataOutput();
			NbtIo.write((CompoundTag) tag, byteArrayDataOutput);
			authResponse.accept(Base64.getEncoder().encodeToString(byteArrayDataOutput.toByteArray()));
			this.success = true;
			ci.cancel();
			return;
		} catch (Throwable t) {
			HotJoin.LOGGER.info("Error authenticating via {}!", FabricLoader.getInstance().getModContainer("authme").orElseThrow().getMetadata().getName());
		}
		ci.cancel();
	}

	@Override
	public void hotjoin$authResponse(Consumer<String> authConsumer) {
		authResponse = authConsumer;
	}

	@Override
	public Consumer<String> hotjoin$authResponse() {
		return authResponse;
	}
}
