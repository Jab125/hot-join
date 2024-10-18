package dev.jab125.hotjoin.mixin.legacy4j;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.jab125.hotjoin.HotJoin;
import dev.jab125.hotjoin.compat.legacy4j.Legacy4JModCompat;
import dev.jab125.hotjoin.mixin.UserAccessor;
import dev.jab125.hotjoin.util.AuthCallback;
import dev.jab125.hotjoin.util.HotJoinCodecs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.ChooseUserScreen;
import wily.legacy.util.MCAccount;

import java.io.IOException;
import java.util.Base64;

@Mixin(value = MCAccount.class, remap = false)
public interface MCAccountMixin {

	@Inject(method = "login(Lwily/legacy/client/screen/ChooseUserScreen;Ljava/lang/String;)V", at = @At("HEAD"))
	default void interceptSimpleLogin(ChooseUserScreen screen, String password, CallbackInfo ci) {
		if (((AuthCallback)screen).hotjoin$authResponse() != null) Legacy4JModCompat.authConsumer = ((AuthCallback)screen).hotjoin$authResponse();
		else Legacy4JModCompat.authConsumer = null;
	}

	@Inject(method = "lambda$login$0", at = @At(value = "INVOKE", target = "Lwily/legacy/client/screen/ChooseUserScreen;reloadAccountButtons()V", shift = At.Shift.AFTER), cancellable = true)
	private static void interceptLambda(ChooseUserScreen screen, CallbackInfo ci) {
		if (((AuthCallback)screen).hotjoin$authResponse() != null) ci.cancel();
		Minecraft.getInstance().setScreen(null);
	}

	@Inject(method = "setUser", at = @At("HEAD"), cancellable = true)
	private static void interceptSetUser(User user, CallbackInfo ci) throws IOException {
		if (Legacy4JModCompat.authConsumer != null) {
			Tag tag = HotJoinCodecs.USER_CODEC.encodeStart(NbtOps.INSTANCE, user).resultOrPartial(HotJoin.LOGGER::error).orElseThrow();
			ByteArrayDataOutput byteArrayDataOutput = ByteStreams.newDataOutput();
			NbtIo.write((CompoundTag) tag, byteArrayDataOutput);
			Legacy4JModCompat.authConsumer.accept(((UserAccessor)user).getUUID().toString(), Base64.getEncoder().encodeToString(byteArrayDataOutput.toByteArray()));
			Legacy4JModCompat.authConsumer = null;
			ci.cancel();
		}
	}
}
