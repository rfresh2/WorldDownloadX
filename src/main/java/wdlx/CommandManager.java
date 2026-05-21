package wdlx;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandManager {
    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
            literal("wdlStart").executes(c -> {
                WorldDownloadX.WDL_MANAGER.start("wdl-test");
                return 0;
            })
        );
        dispatcher.register(
            literal("wdlStop").executes(c -> {
                WorldDownloadX.WDL_MANAGER.stop();
                return 0;
            })
        );
    }
}
