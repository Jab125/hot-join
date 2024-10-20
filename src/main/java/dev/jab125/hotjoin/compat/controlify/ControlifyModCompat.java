package dev.jab125.hotjoin.compat.controlify;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.compatibility.ControlifyCompat;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.InputComponent;
import dev.isxander.controlify.gui.screen.ControllerCarouselScreen;
import dev.isxander.controlify.platform.EventHandler;
import dev.isxander.yacl3.api.Binding;
import dev.jab125.hotjoin.client.screen.PlayerSelectionScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class ControlifyModCompat implements IControlifyModCompat {
	@Override
	public void init() {
		//ControlifyApi.get().
		ControlifyEvents.CONTROLLER_STATE_UPDATE.register(event -> {
			ControllerEntity controller = event.controller();
			Optional<ControllerEntity> currentController = ControlifyApi.get().getCurrentController();
			if (currentController.isPresent() && controller == currentController.get()) return; // Don't allow menu to be opened when it is empty
			Optional<InputComponent> input = controller.input();
			if (input.isEmpty()) return; // There's literally no point if it's empty
			InputBinding binding = input.get().getBinding(ResourceLocation.fromNamespaceAndPath("controlify", "pause"));
			if (binding != null && binding.justPressed()) {
				System.out.println("Pressed!");
				Minecraft.getInstance().setScreen(new PlayerSelectionScreen());
			}
		});
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof ControllerCarouselScreen carouselScreen) {
				Button build = Button.builder(Component.literal("Hot-Join: Disable controller"), b -> {
					Controlify.instance().setCurrentController(null, true);
				}).width(150).pos(screen.width / 2 - 75, 0).build();
				Screens.getButtons(carouselScreen).add(build);
			}
		});
	}
}
