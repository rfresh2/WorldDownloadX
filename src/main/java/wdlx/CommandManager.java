package wdlx;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandManager {
    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
            literal("wdlx:start")
                .executes(c -> {
                    WorldDownloadX.WDL_MANAGER.start();
                    return 0;
                }).then(argument("name", string()).executes(c -> {
                    WorldDownloadX.WDL_MANAGER.start(getString(c, "name"));
                    return 0;
                })
        ));
        dispatcher.register(
            literal("wdlx:stop").executes(c -> {
                WorldDownloadX.WDL_MANAGER.stop();
                return 0;
            })
        );
    }
}
