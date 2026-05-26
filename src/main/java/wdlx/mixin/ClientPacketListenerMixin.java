package wdlx.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wdlx.WorldDownloadX;
import wdlx.events.ChunkLoadEvent;
import wdlx.events.ServerJoinEvent;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Shadow
    private ClientLevel level;

    @Inject(method = "handleLevelChunkWithLight", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;updateLevelChunk(IILnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;)V"
    )) // on main thread before chunk data buf is read
    public void handleLevelChunkWithLightPre(
        final ClientboundLevelChunkWithLightPacket packet,
        final CallbackInfo ci,
        @Share("seenChunk") LocalBooleanRef seenChunkRef
    ) {
        seenChunkRef.set(level.getChunk(packet.getX(), packet.getZ(), ChunkStatus.FULL, false) != null);
    }


    @Inject(method = "handleLevelChunkWithLight", at = @At("RETURN"))
    public void handleLevelChunkWithLightPost(
        ClientboundLevelChunkWithLightPacket packet,
        CallbackInfo ci,
        @Share("seenChunk") LocalBooleanRef seenChunkRef
    ) {
        WorldDownloadX.EVENT_BUS.call(new ChunkLoadEvent(level.getChunk(packet.getX(), packet.getZ()), seenChunkRef.get()));
    }

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void handleLoginPost(ClientboundLoginPacket packet, CallbackInfo ci) {
        WorldDownloadX.EVENT_BUS.call(ServerJoinEvent.INSTANCE);
    }
}
