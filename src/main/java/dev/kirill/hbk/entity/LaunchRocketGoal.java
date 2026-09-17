package dev.kirill.hbk.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class LaunchRocketGoal extends Goal {
	private final GiantBossEntity giant;
	private int cooldown;

	public LaunchRocketGoal(GiantBossEntity giant) {
		this.giant = giant;
		this.setFlags(EnumSet.of(Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.giant.getTarget();
		return this.giant.isAlive() && target != null && target.isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		return this.canUse();
	}

	@Override
	public void start() {
		this.cooldown = 20;
	}

	@Override
	public void tick() {
		if (--this.cooldown > 0) {
			return;
		}

		this.cooldown = 28 + this.giant.getRandom().nextInt(36);
		LivingEntity target = this.giant.getTarget();
		if (target != null && target.isAlive()) {
			this.giant.getLookControl().setLookAt(target, 40.0f, 40.0f);
			if (this.giant instanceof CjEntity cj && this.giant.getRandom().nextInt(4) == 0) {
				cj.launchFireworkVolley(target.getBoundingBox().getCenter());
				this.cooldown += 20;
				return;
			}
			this.giant.launchRocketAt(target.getBoundingBox().getCenter());
			if (this.giant.getRandom().nextInt(3) == 0) {
				Vec3 scatter = target.position().add(
						(this.giant.getRandom().nextDouble() - 0.5) * 10.0,
						0.0,
						(this.giant.getRandom().nextDouble() - 0.5) * 10.0
				);
				this.giant.launchRocketAt(scatter);
			}
		}
	}
}
