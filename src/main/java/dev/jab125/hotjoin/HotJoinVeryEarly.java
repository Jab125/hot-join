package dev.jab125.hotjoin;

import net.fabricmc.loader.DependencyException;
import net.fabricmc.loader.api.*;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.util.version.VersionPredicateParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class HotJoinVeryEarly implements LanguageAdapter {
	@Override
	public <T> T create(ModContainer mod, String value, Class<T> type) {
		throw new Error();
	}

	private static boolean isModAvailable(String name, String... range) {
		if (name.equals("?development")) return FabricLoader.getInstance().isDevelopmentEnvironment();
		if (!FabricLoader.getInstance().isModLoaded(name)) return false;
		ModContainer modContainer = FabricLoader.getInstance().getModContainer(name).orElseThrow();
		Version version = modContainer.getMetadata().getVersion();
		Set<VersionPredicate> parse = null;
		try {
			parse = VersionPredicateParser.parse(Arrays.asList(range));
		} catch (VersionParsingException e) {
			throw new RuntimeException(e);
		}
		for (VersionPredicate versionPredicate : parse) {
			if (versionPredicate.test(version)) return true;
		}
		return false;
	}

	static {
		ModContainer modContainer = FabricLoader.getInstance().getModContainer("hotjoin").orElseThrow();
		CustomValue.CvObject asObject = modContainer.getMetadata().getCustomValue("hotjoin-data").getAsObject();
		l:
		{
			for (Map.Entry<String, CustomValue> entry : asObject.get("at-least-one-available").getAsObject()) {
				String id = entry.getKey();
				String[] range = entry.getValue().getType() == CustomValue.CvType.STRING ? new String[]{entry.getValue().getAsString()} : m(entry.getValue().getAsArray().getAsArray());
				if (isModAvailable(id, range)) break l;
			}
			throw new FormattedException("Couldn't find of Hot-Join's dependencies", "Hot-Join requires Legacy4J or Auth Me");
		}
	}

	private static String[] m(CustomValue.CvArray asArray) {
		ArrayList<String> r = new ArrayList<>();
		for (CustomValue customValue : asArray) {
			r.add(customValue.getAsString());
		}
		return r.toArray(String[]::new);
	}
}
