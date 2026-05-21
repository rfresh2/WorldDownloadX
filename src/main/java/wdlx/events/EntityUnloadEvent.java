package wdlx.events;

import net.minecraft.world.entity.Entity;

public record EntityUnloadEvent(Entity entity, Entity.RemovalReason reason) { }
