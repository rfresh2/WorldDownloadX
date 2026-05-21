package wdlx.events;

import net.minecraft.client.multiplayer.ClientLevel;

public record LevelChangeEvent(ClientLevel previousLevel, ClientLevel newLevel) {
}
