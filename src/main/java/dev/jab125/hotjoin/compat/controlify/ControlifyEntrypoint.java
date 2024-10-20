package dev.jab125.hotjoin.compat.controlify;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.controller.ControllerEntity;

import java.util.Objects;
import java.util.Optional;

public class ControlifyEntrypoint implements dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint {
	@Override
	public void onControllersDiscovered(ControlifyApi controlify) {
		ControlifyData data = ControlifyModCompat.payload.data();
		Optional<ControllerEntity> first = Controlify.instance().getControllerManager().orElseThrow().getConnectedControllers().stream().filter(a -> Objects.equals(data.uid(), a.info().uid())).findFirst();
		if (first.isPresent()) {
			Controlify.instance().setCurrentController(first.get(), true);
			Controlify.instance().config().globalSettings().outOfFocusInput = true;
			Controlify.instance().config().save();
		}
	}
}
