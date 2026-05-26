package wdlx;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wdlx.api.impl.WdlxApiInstance;
import wdlx.util.XaeroPlusIntegration;

public class WorldDownloadX implements ClientModInitializer {
	public static final String MOD_ID = "wdlx";
	public static final Logger LOGGER = LoggerFactory.getLogger("WorldDownloadX");
	public static final WorldDownloadManager WDL_MANAGER = new WorldDownloadManager();
	public static final LambdaManager EVENT_BUS = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator());
	public static final WdlxApiInstance API = new WdlxApiInstance();

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register(CommandManager::registerCommands);
		XaeroPlusIntegration.init();
	}
}
