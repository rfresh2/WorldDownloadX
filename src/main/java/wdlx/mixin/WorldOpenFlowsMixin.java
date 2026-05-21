package wdlx.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Files;

@Mixin(WorldOpenFlows.class)
public class WorldOpenFlowsMixin {
    @WrapOperation(method = "openWorldCheckWorldStemCompatibility", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;askForBackup(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;ZLjava/lang/Runnable;Ljava/lang/Runnable;)V"
    ))
    public void disableExperimentalWorldScreen(final WorldOpenFlows instance, LevelStorageSource.LevelStorageAccess levelStorage, boolean customized, Runnable loadLevel, Runnable onCancel, final Operation<Void> original) {
        var wdlxPropertyFilePath = levelStorage.getLevelDirectory().path().resolve("wdlx.properties");
        if (Files.exists(wdlxPropertyFilePath)) {
            loadLevel.run();
        } else {
            original.call(instance, levelStorage, customized, loadLevel, onCancel);
        }
    }
}
