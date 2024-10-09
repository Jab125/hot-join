package dev.jab125.hotjoin.client.render;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.deechael.concentration.Concentration;
import net.deechael.concentration.fabric.ConcentrationFabric;
import net.deechael.concentration.fabric.config.ConcentrationConfigFabric;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import wily.legacy.client.LegacyTip;

// Render something across multiple windows.
public class SharedRendering {

	public static Monitor bestMonitor;

	public static void render(GuiGraphics graphics, int width, int height) {
		//graphics.fill(width / 2 - 10, height / 2 - 10, width / 2 + 10, height / 2 + 10, 0xffeabc3a);
		//graphics.drawCenteredString(Minecraft.getInstance().font, "HELLO!", width / 2, height / 2 - Minecraft.getInstance().font.lineHeight / 2, 0xffffffff);
	}

	public static void globalToastManager() {
		//new LegacyTip(Component.literal("Welcome, " + player));
	}

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	public static int scale = -1;
	public static int getScale() {
		if (scale == -1) {
			Minecraft instance = Minecraft.getInstance();
			if (bestMonitor == null) bestMonitor = instance.getWindow().findBestMonitor();
			VideoMode currentMode = bestMonitor.getCurrentMode();
			// gui scale
			double guiScale = instance.getWindow().getGuiScale();

			int monitorWidth = currentMode.getWidth();
			int monitorHeight = currentMode.getHeight();
			return scale = calculateScale(0, false, monitorWidth, monitorHeight);
		}
		return scale;
	}

	public static int getWidth() {
		Minecraft instance = Minecraft.getInstance();
		if (bestMonitor == null) bestMonitor = instance.getWindow().findBestMonitor();
		return bestMonitor.getCurrentMode().getWidth();
	}

	public static int getHeight() {
		Minecraft instance = Minecraft.getInstance();
		if (bestMonitor == null) bestMonitor = instance.getWindow().findBestMonitor();
		return bestMonitor.getCurrentMode().getHeight();
	}

	public static int calculateScale(int scale, boolean forceUnicodeFont, int width, int height) {
		int j = 1;

		while (
				j != scale
				&& j < width
				&& j < height
				&& width / (j + 1) >= 320
				&& height / (j + 1) >= 240
		) {
			j++;
		}

		if (forceUnicodeFont && j % 2 != 0) {
			j++;
		}

		return j;
	}

	public static void render0(GuiGraphics graphics) {
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.scale(1 / (float) Minecraft.getInstance().getWindow().getGuiScale(), 1 / (float) Minecraft.getInstance().getWindow().getGuiScale(), 1);
		pose.scale(getScale(), getScale(), 1);
		pose.translate((float) -ConcentrationConfigFabric.getInstance().x / getScale(), (float) -ConcentrationConfigFabric.getInstance().y / getScale(), 1);
		render(graphics, SharedRendering.getWidth() / scale, SharedRendering.getHeight() / scale);
		pose.popPose();
	}
}
