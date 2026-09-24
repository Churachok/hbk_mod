package dev.kirill.hbk.mechanic;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.FoundingPenisProjectileEntity;
import dev.kirill.hbk.item.MemberDestruction;
import dev.kirill.hbk.player.MechanicsPlayerData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Keeps the temporary giant form on the player, so their inventory and controls remain intact. */
public final class ProgenitorTransformation {
	public static final int DURATION_TICKS = 60 * 20;
	public static final float SCALE = 16.0f;
	public static final float WIDTH = 0.6f * SCALE;
	public static final float HEIGHT = 1.8f * SCALE;
	public static final float TRANSFORMATION_EXPLOSION_POWER = 30.0f;
	public static final float PROJECTILE_EXPLOSION_POWER = 12.0f;
	public static final float BLAST_EXPLOSION_POWER = 10.0f;
	public static final float FORM_MAX_HEALTH = 100.0f;
	public static final double FORM_JUMP_STRENGTH = 1.0;
	public static final int BLAST_COOLDOWN_TICKS = 60 * 20;
	public static final Identifier SCALE_MODIFIER_ID = HbkMod.id("progenitor_scale");
	public static final Identifier MAX_HEALTH_MODIFIER_ID = HbkMod.id("progenitor_max_health");
	public static final Identifier JUMP_STRENGTH_MODIFIER_ID = HbkMod.id("progenitor_jump_strength");
	private static final AttributeModifier SCALE_MODIFIER = new AttributeModifier(
			SCALE_MODIFIER_ID, SCALE - 1.0, AttributeModifier.Operation.ADD_VALUE
	);
	private static final AttributeModifier MAX_HEALTH_MODIFIER = new AttributeModifier(
			MAX_HEALTH_MODIFIER_ID, FORM_MAX_HEALTH - 20.0, AttributeModifier.Operation.ADD_VALUE
	);
	private static final AttributeModifier JUMP_STRENGTH_MODIFIER = new AttributeModifier(
			JUMP_STRENGTH_MODIFIER_ID, FORM_JUMP_STRENGTH - 0.42, AttributeModifier.Operation.ADD_VALUE
	);

	private ProgenitorTransformation() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tick(player);
			}
		});
	}

	public static boolean isActive(Player player) {
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		return scale != null && scale.hasModifier(SCALE_MODIFIER_ID);
	}

	public static boolean start(ServerPlayer player) {
		MechanicsPlayerData data = (MechanicsPlayerData) player;
		if (player.isSpectator() || !player.isAlive() || player.isPassenger()
				|| data.hbk$getProgenitorTicks() > 0 || isActive(player)) {
			return false;
		}
		data.hbk$setProgenitorTicks(DURATION_TICKS);
		setFormAttributes(player, true);
		player.setHealth(player.getMaxHealth());
		player.setIgnoreFallDamageFromCurrentImpulse(true, player.position());
		player.applyPostImpulseGraceTime(40);
		MemberDestruction.explode((ServerLevel) player.level(), player,
				player.position().add(0.0, HEIGHT / 2.0, 0.0), TRANSFORMATION_EXPLOSION_POWER);
		var advancement = player.level().getServer().getAdvancements().get(HbkMod.id("founding_penis"));
		if (advancement != null) {
			player.getAdvancements().award(advancement, "transform");
		}
		player.sendOverlayMessage(Component.translatable("message.hbk.founding_penis.started"));
		return true;
	}

	public static boolean fireProjectile(ServerPlayer player) {
		if (!isActive(player) || player.isSpectator() || !player.isAlive()) {
			return false;
		}
		ServerLevel level = (ServerLevel) player.level();
		Vec3 direction = player.getLookAngle();
		Vec3 origin = player.getEyePosition().add(direction.scale(5.5));
		FoundingPenisProjectileEntity projectile = new FoundingPenisProjectileEntity(level, player);
		projectile.setPos(origin.x, origin.y, origin.z);
		projectile.shoot(direction.x, direction.y, direction.z, 2.5f, 0.0f);
		if (!level.addFreshEntity(projectile)) {
			return false;
		}
		level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
				SoundSource.PLAYERS, 2.0f, 0.5f);
		return true;
	}

	public static boolean blast(ServerPlayer player) {
		if (!isActive(player) || player.isSpectator() || !player.isAlive()) {
			return false;
		}
		ServerLevel level = (ServerLevel) player.level();
		MechanicsPlayerData data = (MechanicsPlayerData) player;
		long now = level.getServer().overworld().getGameTime();
		long readyAt = data.hbk$getFoundingBlastReadyTick();
		if (now < readyAt) {
			long remainingSeconds = (readyAt - now + 19) / 20;
			player.sendOverlayMessage(Component.translatable("message.hbk.founding_penis.cooldown", remainingSeconds));
			return false;
		}
		Vec3 target = MemberDestruction.aimedPosition(level, player, 48.0);
		BlockPos targetBlock = BlockPos.containing(target);
		if (!level.isLoaded(targetBlock) || !level.getWorldBorder().isWithinBounds(targetBlock)) {
			return false;
		}
		MemberDestruction.explode(level, player, target, BLAST_EXPLOSION_POWER);
		data.hbk$setFoundingBlastReadyTick(now + BLAST_COOLDOWN_TICKS);
		return true;
	}

	public static void tick(ServerPlayer player) {
		MechanicsPlayerData data = (MechanicsPlayerData) player;
		int ticks = data.hbk$getProgenitorTicks();
		if (ticks <= 0 || !player.isAlive() || player.isSpectator()) {
			if (ticks > 0) {
				data.hbk$setProgenitorTicks(0);
			}
			setFormAttributes(player, false);
			return;
		}
		if (ticks == 1) {
			data.hbk$setProgenitorTicks(0);
			setFormAttributes(player, false);
			player.sendOverlayMessage(Component.translatable("message.hbk.founding_penis.ended"));
		} else {
			setFormAttributes(player, true);
			data.hbk$setProgenitorTicks(ticks - 1);
		}
	}

	private static void setFormAttributes(ServerPlayer player, boolean enabled) {
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		boolean dimensionsChanged = scale != null && scale.hasModifier(SCALE_MODIFIER_ID) != enabled;
		updateModifier(scale, SCALE_MODIFIER, enabled);
		updateModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_MODIFIER, enabled);
		updateModifier(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_STRENGTH_MODIFIER, enabled);
		if (dimensionsChanged) {
			player.refreshDimensions();
		}
		if (!enabled && player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	private static void updateModifier(AttributeInstance attribute, AttributeModifier modifier, boolean enabled) {
		if (attribute == null) {
			return;
		}
		if (enabled) {
			attribute.addOrUpdateTransientModifier(modifier);
		} else {
			attribute.removeModifier(modifier.id());
		}
	}
}
