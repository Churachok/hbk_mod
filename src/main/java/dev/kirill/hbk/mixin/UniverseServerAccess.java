package dev.kirill.hbk.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public interface UniverseServerAccess {
	@Accessor("levels") Map<ResourceKey<Level>, ServerLevel> hbk$levels();
	@Accessor("storageSource") LevelStorageSource.LevelStorageAccess hbk$storage();
	@Accessor("executor") Executor hbk$executor();
}
