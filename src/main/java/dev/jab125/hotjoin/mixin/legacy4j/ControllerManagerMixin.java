package dev.jab125.hotjoin.mixin.legacy4j;

import dev.jab125.hotjoin.HotJoin;
import dev.jab125.hotjoin.compat.legacy4j.Legacy4JData;
import dev.jab125.hotjoin.compat.legacy4j.Legacy4JModCompat;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.*;
import wily.legacy.util.ScreenUtil;

@Mixin(targets = "wily/legacy/client/controller/ControllerManager$1")
public class ControllerManagerMixin {
	@Shadow @Final private ControllerManager this$0;
	@Inject(method = "run", at = @At("HEAD"), remap = false)
	void interceptRun(CallbackInfo ci) {
		if (!HotJoin.canLaunchAnotherClient()) return;
		if (!(Minecraft.getInstance().level != null || (Minecraft.getInstance().getSingleplayerServer() != null || Minecraft.getInstance().getCurrentServer() != null) || Minecraft.getInstance().screen != null)) return;
		Controller.Handler handler = ControllerManager.getHandler();
		// this goes from 0 to 15
		//handler.getController()
		for (int i = 0; i < 16; i++) {
			if (i == ScreenUtil.getLegacyOptions().selectedController().get()) continue;
			if (Minecraft.getInstance().isRunning() && ControllerManager.getHandler().update()) {
				int finalI1 = i;
				if (Legacy4JModCompat.uuidLegacy4JMap.values().stream().map(Legacy4JData::controllerIndex).anyMatch(j -> j.equals(finalI1))) continue;
				if (handler.isValidController(i)) {
					// TODO: how expensive is this
					Controller controller = handler.getController(i);
					//System.out.println(i + " is a valid controller, " + controller.getName());
					//controller.connect(Legacy4JClient.controllerManager);

					if (handler instanceof GLFWControllerHandler handler2) {
						GLFWGamepadState gamepadState = GLFWGamepadState.calloc();
						// this _should_ be safe, right?
						if (GLFW.glfwGetGamepadState(i, gamepadState)) {
							gamepadState.free();
							//manager.updateBindings();
							if (gamepadState.buttons(ControllerManager.getHandler().getBindingIndex(ControllerBinding.START)) == 1) {
								if (Minecraft.getInstance().level != null && Minecraft.getInstance().getSingleplayerServer() != null && Minecraft.getInstance().screen == null) {
									// we are in a world, and we own it, and there is no screen open.
									int finalI = i;
									Minecraft.getInstance().tell(() -> {
										if (Minecraft.getInstance().screen != null) return;
										Legacy4JModCompat.openLegacy4JUserPicker(new Legacy4JData(controller.getName(), ScreenUtil.getLegacyOptions().selectedController().get(), finalI, ScreenUtil.getLegacyOptions().selectedControllerHandler().get()));
									});
								}
								//System.out.println(controller.getName() + " is holding down the guide button!");
							}
						} else {
							gamepadState.free();
						}
					} else if (handler instanceof SDLControllerHandler handler1) {
						if (controller.buttonPressed(ControllerManager.getHandler().getBindingIndex(ControllerBinding.START))) {
							if (Minecraft.getInstance().level != null && (Minecraft.getInstance().getSingleplayerServer() != null || Minecraft.getInstance().getCurrentServer() != null) && Minecraft.getInstance().screen == null) {
								// we are in a world, and we own it, and there is no screen open.
								int finalI = i;
								Minecraft.getInstance().tell(() -> {
									if (Minecraft.getInstance().screen != null) return;
									Legacy4JModCompat.openLegacy4JUserPicker(new Legacy4JData(controller.getName(), ScreenUtil.getLegacyOptions().selectedController().get(), finalI, ScreenUtil.getLegacyOptions().selectedControllerHandler().get()));
								});
							}
						}
					}
				}
			}
		}
	}
}
