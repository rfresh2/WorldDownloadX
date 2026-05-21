package wdlx;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldDownloadX implements ModInitializer {
	public static final String MOD_ID = "world-download-x";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("WorldDownloadX");
	public static final WorldDownloadManager WDL_MANAGER = new WorldDownloadManager();

	@Override
	public void onInitialize() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {

		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {

		});
		ClientCommandRegistrationCallback.EVENT.register(CommandManager::registerCommands);
	}
}
