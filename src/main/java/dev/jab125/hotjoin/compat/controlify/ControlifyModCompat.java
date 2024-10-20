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
import dev.jab125.hotjoin.compat.legacy4j.Legacy4JData;
import dev.jab125.hotjoin.packet.AlohaPayload;
import dev.jab125.hotjoin.packet.ControlifyInfoPayload;
import dev.jab125.hotjoin.server.HotJoinS2CThread;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ControlifyModCompat implements IControlifyModCompat {
	public static final HashMap<UUID, ControlifyData> uuidControlifyMap = new HashMap<>();
	// only set on hot-join client
	public static ControlifyInfoPayload payload;

	@Override
	public void init() {
		//ControlifyApi.get().
		ControlifyEvents.CONTROLLER_STATE_UPDATE.register(event -> {
			ControllerEntity controller = event.controller();
			if (uuidControlifyMap.values().stream().map(ControlifyData::uid).anyMatch(controller.info().uid()::equals)) return;
			Optional<ControllerEntity> currentController = ControlifyApi.get().getCurrentController();
			if (currentController.isPresent() && controller == currentController.get()) return; // Don't allow menu to be opened when it is empty
			Optional<InputComponent> input = controller.input();
			if (input.isEmpty()) return; // There's literally no point if it's empty
			InputBinding binding = input.get().getBinding(ResourceLocation.fromNamespaceAndPath("controlify", "pause"));
			if (binding != null && binding.justPressed()) {
				System.out.println("Pressed!");
				Minecraft.getInstance().setScreen(new PlayerSelectionScreen(controller));
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

	@Override
	public void leftWorld(UUID o) {
		uuidControlifyMap.remove(o);
	}

	@Override
	public void connectionEstablished(HotJoinS2CThread thread, AlohaPayload payload, UUID uuid) {
		thread.runTask(t -> t.send(new ControlifyInfoPayload(uuidControlifyMap.get(payload.uuid()))));
	}

	@Override
	public void receivedControlifyPayload(ControlifyInfoPayload payload) {
		ControlifyModCompat.payload = payload;
	}
}
