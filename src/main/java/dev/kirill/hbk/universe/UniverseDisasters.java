package dev.kirill.hbk.universe;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

/** Address/time based events. No persistent entity flags, terrain damage or client countdown packets. */
public final class UniverseDisasters {
	public static final int WARNING = 200, GRAVITY_DURATION = 1200, CATACLYSM_DURATION = 1800;
	private static final int GRAVITY_PERIOD = 18000, CATACLYSM_PERIOD = 24000;
	public enum Kind { ACID, ASH, CRYSTAL, ELECTRIC }
	public record Event(boolean active, boolean warning, int remaining, int elapsed) {}
	private UniverseDisasters() {}
	public static Kind kind(UniverseProfile p) {
		return switch (p.geometry()) {
			case 0, 4, 5 -> Kind.ACID;
			case 3, 9 -> Kind.ASH;
			case 2, 7, 8 -> Kind.CRYSTAL;
			default -> Kind.ELECTRIC;
		};
	}
	private static Event event(UniverseProfile p, long time, boolean gravity) {
		int period = gravity ? GRAVITY_PERIOD : CATACLYSM_PERIOD;
		int duration = gravity ? GRAVITY_DURATION : CATACLYSM_DURATION;
		long start = 2400 + Long.remainderUnsigned(p.stream(gravity ? 81 : 82), period - duration - WARNING - 2400);
		long firstWarning = start - WARNING;
		if (time < firstWarning) return new Event(false, false, (int) Math.min(Integer.MAX_VALUE, firstWarning - time), 0);
		int phase = (int) Math.floorMod(time - firstWarning, period);
		if (phase < WARNING) return new Event(false, true, WARNING - phase, 0);
		if (phase < WARNING + duration) return new Event(true, false, WARNING + duration - phase, phase - WARNING);
		return new Event(false, false, period - phase, 0);
	}
	public static Event gravity(UniverseProfile p, long time) { return event(p, time, true); }
	public static Event cataclysm(UniverseProfile p, long time) { return event(p, time, false); }
	/** Nearby solid roofs work in open, stacked and ceiling worlds; never request another chunk. */
	public static boolean sheltered(Level level, Vec3 position) {
		BlockPos base = BlockPos.containing(position.x, position.y + 1.8, position.z);
		for (int y = 0; y <= 12; y++) {
			BlockPos pos = base.above(y);
			if (level.isOutsideBuildHeight(pos.getY())) break;
			if (!level.hasChunkAt(pos)) return true;
			var state = level.getBlockState(pos);
			if (state.isFaceSturdy(level, pos, net.minecraft.core.Direction.DOWN)) return true;
		}
		return false;
	}
	public static double gravityMultiplier(UniverseProfile p, long time) {
		var event = gravity(p, time);
		if (!event.active) return 1;
		double edge = Math.min(1, Math.min(event.elapsed / 60.0, event.remaining / 60.0));
		return 1 + Math.sin(event.elapsed * Math.PI / 120.0) * 1.65 * edge;
	}
	public static double gravityMultiplier(Entity entity) {
		var p = UniverseProfile.of(entity.level());
		if (p == null || !gravity(p, entity.level().getGameTime()).active || sheltered(entity.level(), entity.position())) return 1;
		return gravityMultiplier(p, entity.level().getGameTime());
	}
	public static boolean protectedFromAsh(Player player) {
		return UniverseHazards.protectedFromGas(player, UniverseHazards.Kind.SPORE_GAS);
	}
	/** Five-tick impulses compensate even the heavy phase without leaving a noGravity flag. */
	public static boolean liftItem(ItemEntity item, long time) {
		if (sheltered(item.level(), item.position())) return false;
		var motion = item.getDeltaMovement();
		double lift = .06 + .025 * Math.sin(time / 18.0 + item.getId()) + Math.max(0, item.getGravity()) * 3;
		item.setDeltaMovement(motion.x * .98, Math.max(motion.y, lift), motion.z * .98);
		item.hurtMarked = true;
		return true;
	}
	public static void affect(ServerLevel level, Player player, Kind kind, long time) {
		if (!player.isAlive() || player.isCreative() || player.isSpectator() || sheltered(level, player.position())) return;
		switch (kind) {
			case ACID -> {
				if (time % 40 == 0 && !AlloyActions.equipped(player, EquipmentSlot.HEAD, "hermetic_helmet"))
					player.hurtServer(level, player.damageSources().magic(), 1);
			}
			case ASH -> {
				if (!protectedFromAsh(player)) {
					player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 60, 0, true, true));
					player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, true));
				}
			}
			case CRYSTAL -> {
				if (time % 60 == 0) player.hurtServer(level, player.damageSources().magic(), player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() ? 2 : 1);
			}
			case ELECTRIC -> {
				if (time % 100 != 0) return;
				// The charge and strike share a visible position. Lightning is cosmetic: no fire or mob conversion.
				long hash = UniverseNoise.hash(UniverseProfile.of(level).stream(83), player.getId(), (int) (time / 100), 0);
				Vec3 target = player.position().add((hash & 7) - 3, 0, (hash >>> 4 & 7) - 3);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.x, target.y + 1, target.z, 24, .8, 1, .8, .03);
				if (time % 200 == 0) {
					var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
					if (bolt != null) { bolt.setPos(target); bolt.setVisualOnly(true); level.addFreshEntity(bolt); }
					if (target.distanceToSqr(player.position()) <= 6.25) player.hurtServer(level, player.damageSources().magic(), 3);
				}
			}
		}
	}
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			var lifted = new HashSet<ItemEntity>();
			for (var player : server.getPlayerList().getPlayers()) {
				var level = player.level(); var p = UniverseProfile.of(level);
				if (p == null || !player.isAlive() || player.isSpectator()) continue;
				long time = level.getGameTime(); var gravity = gravity(p, time); var weather = cataclysm(p, time);
				if (time % 20 == 0) {
					if (gravity.active || gravity.warning) player.sendOverlayMessage(Component.translatable("message.hbk.disaster.gravity_" + (gravity.active ? "active" : "warning"), (gravity.remaining + 19) / 20));
					else if (weather.active || weather.warning) player.sendOverlayMessage(Component.translatable("message.hbk.disaster.weather_" + (weather.active ? "active" : "warning"), Component.translatable("disaster.hbk." + kind(p).name().toLowerCase(java.util.Locale.ROOT)), (weather.remaining + 19) / 20));
					if (weather.active) affect(level, player, kind(p), time);
					if (gravity.active && !player.isCreative() && !sheltered(level, player.position())) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, true, false));
				}
				if (gravity.active && time % 5 == 0) {
					int budget = 64;
					for (var item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(24))) {
						if (budget-- <= 0) break;
						if (lifted.add(item)) liftItem(item, time);
					}
				}
			}
		});
	}
}
