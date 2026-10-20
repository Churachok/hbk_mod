package dev.kirill.hbk.mixin;

import dev.kirill.hbk.universe.UniverseEnvironment;
import dev.kirill.hbk.universe.UniverseProfile;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(Entity.class)
public abstract class UniversePhysicsMixin {
	@ModifyReturnValue(method = "getGravity", at = @At("RETURN"))
	private double hbk$universeGravity(double original) {
		Entity entity = (Entity) (Object) this;
		var profile = UniverseProfile.of(entity.level());
		if (profile != null) {
			double gravity = UniverseEnvironment.gravity(profile, entity.position());
			if (entity instanceof net.minecraft.world.entity.LivingEntity living && dev.kirill.hbk.universe.AlloyActions.equipped(living, net.minecraft.world.entity.EquipmentSlot.CHEST, "gravity_chestplate")) gravity = 1 + (gravity - 1) * .2;
			return original * gravity;
		}
		return original;
	}
}
