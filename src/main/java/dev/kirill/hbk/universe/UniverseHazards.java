package dev.kirill.hbk.universe;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.kirill.hbk.universe.UniverseNoise.*;

/** Local, non-destructive fields shared by server effects, client particles and atmosphere.
 * No chunk lookups, saved entities, network payloads or changes to the v1 density/profile. */
public final class UniverseHazards {
	public enum Kind {
		SPORE_GAS(0x99b96b, true), FROST_GAS(0xa2cbd5, true), CINDER_GAS(0xd7a176, true),
		UPDRAFT(0xb5e5dc, false), HEAVY_FIELD(0xaf97d2, false), RIFT(0xdea9d4, false);
		public final int color;
		public final boolean gas;
		Kind(int color, boolean gas) { this.color = color; this.gas = gas; }
		public String key() { return "hazard.hbk." + name().toLowerCase(java.util.Locale.ROOT); }
	}
	public record Zone(Kind kind, Vec3 center, double radius, double height) {
		public double strength(Vec3 pos) {
			double dx = (pos.x - center.x) / radius, dy = (pos.y - center.y) / height, dz = (pos.z - center.z) / radius;
			return clamp((1 - dx * dx - dy * dy - dz * dz) * 2, 0, 1);
		}
	}
	private record Exposure(UniverseCode code, Zone zone, long entered) {}
	private static final Map<UUID, Exposure> EXPOSURES = new HashMap<>();
	private UniverseHazards() {}
	public static Kind gasKind(UniverseProfile p) {
		return switch (p.geometry()) {
			case 0, 4, 5 -> Kind.SPORE_GAS;
			case 1, 2, 6, 8 -> Kind.FROST_GAS;
			default -> Kind.CINDER_GAS;
		};
	}
	public static Zone cell(UniverseProfile p, int x, int y, int z) {
		long h = hash(p.stream(60), x, y, z);
		if (Long.remainderUnsigned(h, 5) == 0) return null;
		long a = mix(h), b = mix(a), c = mix(b);
		int choice = (int) Long.remainderUnsigned(c, 6);
		Kind kind = choice < 3 ? gasKind(p) : Kind.values()[choice];
		return new Zone(kind, new Vec3(x * 96.0 + 32 + Long.remainderUnsigned(a, 32),
				y * 80.0 - 64 + 20 + Long.remainderUnsigned(b, 40), z * 96.0 + 32 + Long.remainderUnsigned(c, 32)),
				18 + Long.remainderUnsigned(mix(c), 11), 10 + Long.remainderUnsigned(mix(c + 1), 7));
	}
	public static Zone at(UniverseProfile p, Vec3 pos) {
		if (p == null || pos.y < -64 || pos.y >= 320) return null;
		Zone zone = cell(p, (int) Math.floor(pos.x / 96), (int) Math.floor((pos.y + 64) / 80), (int) Math.floor(pos.z / 96));
		return zone != null && zone.strength(pos) > 0 ? zone : null;
	}
	/** Nearby field centres only; this diagnostic never loads or generates chunks. */
	public static Zone nearest(UniverseProfile p, Vec3 pos, Kind kind) {
		Zone nearest = null; double distance = Double.MAX_VALUE;
		int cx = (int) Math.floor(pos.x / 96), cy = (int) Math.floor((pos.y + 64) / 80), cz = (int) Math.floor(pos.z / 96);
		for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) for (int y = Math.max(0, cy - 1); y <= Math.min(4, cy + 1); y++) {
			Zone zone = cell(p, x, y, z);
			if (zone == null || zone.kind != kind) continue;
			double d = zone.center.distanceToSqr(pos);
			if (d < distance) { nearest = zone; distance = d; }
		}
		return nearest;
	}
	public static double gravity(UniverseProfile p, Vec3 pos) {
		Zone zone = at(p, pos);
		if (zone == null) return 1;
		return lerp(1, zone.kind == Kind.UPDRAFT ? .3 : zone.kind == Kind.HEAVY_FIELD ? 1.7 : 1, zone.strength(pos));
	}
	public static boolean protectedFromGas(Player player, Kind kind) {
		return AlloyActions.equipped(player, net.minecraft.world.entity.EquipmentSlot.HEAD, "hermetic_helmet") || AlloyDeviceBlockEntity.filtered(player) || player.hasEffect(MobEffects.WATER_BREATHING) || kind == Kind.CINDER_GAS && player.hasEffect(MobEffects.FIRE_RESISTANCE);
	}
	/** Exposure begins in the core, after three seconds to let the player see and leave it. */
	public static void affect(Player player, Zone zone, long exposureTicks, long time) {
		if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || player.isCreative() || player.isSpectator() || exposureTicks < 60 || zone.strength(player.position()) < .35) return;
		if (zone.kind.gas && (protectedFromGas(player, zone.kind) || player.isEyeInFluid(FluidTags.WATER))) return;
		switch (zone.kind) {
			case SPORE_GAS -> player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, true, true));
			case FROST_GAS -> { player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, true, true)); player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, 0, true, true)); }
			case CINDER_GAS -> player.hurtServer(level, player.damageSources().inFire(), 1);
			case UPDRAFT -> { if (!AlloyActions.equipped(player, net.minecraft.world.entity.EquipmentSlot.FEET, "anchor_boots")) player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0, true, true)); player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, true)); }
			case HEAVY_FIELD -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, true, true));
			case RIFT -> { if (Math.floorMod(time, 120) < 20) { player.hurtServer(level, player.damageSources().magic(), 1); player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, true)); } }
		}
	}
	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> EXPOSURES.remove(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> EXPOSURES.clear());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				UniverseProfile profile = UniverseProfile.of(player.level());
				Zone zone = at(profile, player.position());
				if (zone == null || zone.strength(player.position()) < .35 || !player.isAlive() || player.isCreative() || player.isSpectator()) { EXPOSURES.remove(player.getUUID()); continue; }
				long time = player.level().getGameTime();
				Exposure exposure = EXPOSURES.get(player.getUUID());
				if (exposure == null || !exposure.code.equals(profile.code()) || !exposure.zone.equals(zone) || time < exposure.entered) {
					exposure = new Exposure(profile.code(), zone, time); EXPOSURES.put(player.getUUID(), exposure);
				}
				player.sendSystemMessage(Component.translatable(protectedFromGas(player, zone.kind) && zone.kind.gas
						? "message.hbk.universe.hazard_protected" : "message.hbk.universe.hazard_warning", Component.translatable(zone.kind.key())), true);
				affect(player, zone, time - exposure.entered, time);
			}
		});
	}
}
