package dev.kirill.hbk.universe;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Ordinary arrows become marked impulse bolts; the projectile retains the mark after save/load. */
public final class ImpulseCrossbow extends CrossbowItem {
	public ImpulseCrossbow(Properties properties) { super(properties); }
	@Override protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammunition, boolean critical) {
		Projectile projectile = super.createProjectile(level, shooter, weapon, ammunition, critical);
		projectile.addTag("hbk_impulse_bolt");
		return projectile;
	}
}
