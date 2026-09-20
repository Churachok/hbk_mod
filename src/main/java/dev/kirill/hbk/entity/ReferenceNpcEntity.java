package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Humanoid NPCs sharing a skin model but retaining their individual temperaments. */
public class ReferenceNpcEntity extends PathfinderMob {

	public ReferenceNpcEntity(EntityType<? extends ReferenceNpcEntity> type, Level level) {
		super(type, level);
		this.xpReward = 5;
		if (this.isNpc("sasha") || this.isNpc("vlad")) {
			this.arm();
		}
		if (this.isNpc("vlad")) {
			this.setPersistenceRequired();
		}
	}

	public boolean isNpc(String name) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath().equals(name);
	}

	public static AttributeSupplier.Builder createAttributes(double health) {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, health)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		if (this.isNpc("anton")) {
			this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
		} else if (!this.isNpc("vlad")) {
			this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
			this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		}
		if (this.isNpc("sasha")) {
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		} else if (this.isNpc("gosha")) {
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Sheep.class, true));
		}
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && this.isAlive() && !this.isNpc("anton") && !this.isNpc("vlad")
				&& source.getEntity() instanceof LivingEntity attacker && this.canAttack(attacker)) {
			this.arm();
			this.setTarget(attacker);
		}
		return hurt;
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		if (this.isNpc("vlad") || this.isNpc("anton")) {
			return false;
		}
		if (this.isNpc("sasha") && target instanceof ReferenceNpcEntity npc && npc.isNpc("lesha")) {
			return false;
		}
		return super.canAttack(target);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return this.isNpc("sasha");
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		Item weapon = this.getWeapon();
		if ((weapon != null && this.getMainHandItem().is(weapon))
				|| (this.isNpc("lesha") && this.getMainHandItem().is(ModItems.COLOSSAL_MEMBER))) {
			// Suppress legacy saved equipment chances; the loot table owns the only roll.
			this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
		}
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}

	private void arm() {
		Item weapon = this.getWeapon();
		if (weapon != null && !this.getMainHandItem().is(weapon)) {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
			this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
		}
	}

	private Item getWeapon() {
		return switch (BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath()) {
			case "denis" -> ModItems.JAW_MEMBER;
			case "gosha" -> ModItems.BEASTLIKE_MEMBER;
			case "grisha" -> ModItems.HAMMER_FIGHTER_MEMBER;
			case "sasha" -> ModItems.ARMORED_MEMBER;
			case "vlad" -> ModItems.CARRIER_MEMBER;
			default -> null;
		};
	}
}
