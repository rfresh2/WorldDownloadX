package wdlx;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldDownloadX implements ModInitializer {
	public static final String MOD_ID = "wdlx";
	public static final Logger LOGGER = LoggerFactory.getLogger("WorldDownloadX");
	public static final WorldDownloadManager WDL_MANAGER = new WorldDownloadManager();
	public static final LambdaManager EVENT_BUS = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator());

	@Override
	public void onInitialize() {
		ClientCommandRegistrationCallback.EVENT.register(CommandManager::registerCommands);
	}
}
