package dev.jab125.hotjoin.client.screen;

import com.google.common.io.ByteStreams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
/////// TODO TODO TODO TODO TODO TODO
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.controller.ControllerEntity;
/////// TODO TODO TODO TODO TODO TODO
import dev.jab125.hotjoin.HotJoin;
import dev.jab125.hotjoin.compat.authme.AuthMeCompat;
import dev.jab125.hotjoin.compat.controlify.ControlifyData;
import dev.jab125.hotjoin.compat.controlify.ControlifyModCompat;
import dev.jab125.hotjoin.mixin.UserAccessor;
import dev.jab125.hotjoin.util.AuthCallback;
import dev.jab125.hotjoin.util.HotJoinCodecs;
import me.axieum.mcmod.authme.impl.gui.MicrosoftAuthScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static dev.jab125.hotjoin.HotJoin.LOGGER;
import static dev.jab125.hotjoin.HotJoin.crashgoByeBye;

// This class will probably be most of the MC version porting work
public class PlayerSelectionScreen extends Screen {

	private final @Nullable ControllerEntity controller;
	private final ControllerEntity previousController;
	private PlayerSelectionList playerSelectionList;
	private Button join;
	private Button addUser;
	private List<String> players = new ArrayList<>();

	public PlayerSelectionScreen(@Nullable ControllerEntity controller) {
		super(Component.empty());
		this.controller = controller;
		previousController = ControlifyApi.get().getCurrentController().orElse(null);
		if (controller != null) Controlify.instance().setCurrentController(controller, true);
	}

	@Override
	public void onClose() {
		super.onClose();
		if (controller != null) Controlify.instance().setCurrentController(previousController, true);
	}

	@Override
	protected void init() {
		super.init();
		try {
			players = loadPlayers();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		playerSelectionList = new PlayerSelectionList(minecraft, this.width, this.height - 33 - 33, 33, 18);
		for (String player : players) {
			playerSelectionList.children().add(new PlayerSelectionList.Entry(player));
		}
		addRenderableWidget(playerSelectionList);
		addUser = Button.builder(Component.literal("Add User"), b -> {
			MicrosoftAuthScreen microsoftAuthScreen = new MicrosoftAuthScreen(Minecraft.getInstance().screen, this, true);
			((AuthCallback)microsoftAuthScreen).hotjoin$authResponse(this::response);
			Minecraft.getInstance().setScreen(microsoftAuthScreen);
		}).pos(this.width / 2 - 50 - 105, this.height - 25).size(100, 20).build();
		addRenderableWidget(addUser);
		join = Button.builder(Component.literal("Join"), b -> {
			if (playerSelectionList.getSelected() == null) return;
			PlayerSelectionList.Entry selected = playerSelectionList.getSelected();
			String magic = selected.magic;
			String uuid = selected.uuid;
			UUID uuid1 = HotJoin.authMeCompat.launchAuthMeClient(uuid, magic);
			ControlifyModCompat.uuidControlifyMap.put(uuid1, new ControlifyData(controller == null ? "" : controller.info().uid()));
			this.onClose();
		}).pos(this.width / 2 - 50, this.height - 25).size(100, 20).build();
		addRenderableWidget(join);
	}

	private void response(String uuid, String magic) {
		ArrayList<String> strings = null;
		try {
			strings = loadPlayers();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		strings.add(magic);
		try {
			savePlayers(strings);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static ArrayList<String> loadPlayers() throws IOException {
		Path resolve = FabricLoader.getInstance().getGameDir().resolve(Path.of(".hotjoin-account-data.dat"));
		if (!resolve.toFile().exists()) return new ArrayList<>();
		CompoundTag read = NbtIo.read(resolve);
		List<String> first = P.CODEC.decode(NbtOps.INSTANCE, read).resultOrPartial(LOGGER::error).orElseThrow().getFirst().accounts();
		return new ArrayList<>(first);
	}

	private record P(ArrayList<String> accounts) {
		private static final Codec<P> CODEC = RecordCodecBuilder.create(instance ->
				instance.group(Codec.STRING.listOf().fieldOf("accounts").xmap(ArrayList::new, a -> a).forGetter(P::accounts)).apply(instance, P::new));
	}

	private static void savePlayers(ArrayList<String> players) throws IOException {
		Path resolve = FabricLoader.getInstance().getGameDir().resolve(Path.of(".hotjoin-account-data.dat"));
		CompoundTag tag = (CompoundTag) P.CODEC.encodeStart(NbtOps.INSTANCE, new P(players)).resultOrPartial(LOGGER::error).orElseThrow();
		NbtIo.write(tag, resolve);
	}

	@Override
	public void tick() {
		super.tick();
		if (playerSelectionList == null) join.active = false;
		else join.active = playerSelectionList.getSelected() != null;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int i, int j, float f) {
		super.render(guiGraphics, i, j, f);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.literal("Basic Player Selection Screen"), width / 2, 6, 0xffffffff);
	}

	static class PlayerSelectionList extends ObjectSelectionList<PlayerSelectionList.Entry> {

		public PlayerSelectionList(Minecraft minecraft, int i, int j, int k, int l) {
			super(minecraft, i, j, k, l);
		}

		static class Entry extends ObjectSelectionList.Entry<Entry> {
			private final String magic;
			private final String username;
			private final String uuid;
			public Entry(String magic) {
				this.magic = magic;
				this.username = HotJoinCodecs.USER_CODEC.decode(NbtOps.INSTANCE, crashgoByeBye(() -> NbtIo.read(ByteStreams.newDataInput(Base64.getDecoder().decode(magic.replace("$", "=")))))).resultOrPartial(LOGGER::error).orElseThrow().getFirst().getName();
				this.uuid = ((UserAccessor) HotJoinCodecs.USER_CODEC.decode(NbtOps.INSTANCE, crashgoByeBye(() -> NbtIo.read(ByteStreams.newDataInput(Base64.getDecoder().decode(magic.replace("$", "=")))))).resultOrPartial(LOGGER::error).orElseThrow().getFirst()).getUUID().toString();
			}
			@Override
			public Component getNarration() {
				return Component.literal(username);
			}

			@Override
			public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
				guiGraphics.drawCenteredString(Minecraft.getInstance().font, username, x + entryWidth / 2/* - Minecraft.getInstance().font.width(username) / 2*/, y, hovered ? 0xffffff00 : 0xffffffff);
			}
		}
	}
}
