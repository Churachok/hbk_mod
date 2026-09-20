package dev.kirill.hbk.mixin;

import dev.kirill.hbk.entity.ReferenceNpcEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class AntonPredatorMixin {
	@Shadow @Final protected GoalSelector targetSelector;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void hbk$huntAnton(CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;
		if (mob instanceof Enemy) {
			this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob,
					ReferenceNpcEntity.class, 10, true, false,
					(target, level) -> target instanceof ReferenceNpcEntity npc && npc.isNpc("anton")));
		}
	}
}
