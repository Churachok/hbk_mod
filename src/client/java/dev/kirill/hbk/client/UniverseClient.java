package dev.kirill.hbk.client;

import dev.kirill.hbk.universe.HiddenParameter;
import dev.kirill.hbk.universe.UniverseProfile;
import dev.kirill.hbk.universe.UniverseWeather;
import dev.kirill.hbk.universe.UniverseHazards;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Items;

/** Bounded, local precipitation particles; no custom ambient audio. */
public final class UniverseClient {
	private UniverseClient() {}
	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null || client.player == null || client.isPaused()) return;
			var p = UniverseProfile.of(client.level);
			if (p == null) return;
			var random = client.level.getRandom();
			// Local fields are visible even in dry/closed worlds. Strictly bounded particle budget.
			for (int i = 0; i < 16; i++) {
				Vec3 pos = client.player.position().add(random.nextDouble() * 56 - 28, random.nextDouble() * 22 - 9, random.nextDouble() * 56 - 28);
				var zone = UniverseHazards.at(p, pos);
				if (zone == null || !client.level.getBlockState(BlockPos.containing(pos)).isAir()) continue;
				ParticleOptions particle = zone.kind().gas ? new DustParticleOptions(zone.kind().color, 1.8f)
						: switch (zone.kind()) { case UPDRAFT -> ParticleTypes.END_ROD; case HEAVY_FIELD -> ParticleTypes.FALLING_OBSIDIAN_TEAR; default -> ParticleTypes.REVERSE_PORTAL; };
				client.level.addParticle(particle, pos.x, pos.y, pos.z, .008, zone.kind() == UniverseHazards.Kind.HEAVY_FIELD ? -.06 : .025, -.008);
			}
			if (UniverseWeather.rain(p, client.level.getGameTime()) <= 0) return;
			int type = p.get(HiddenParameter.PRECIPITATION_TYPE);
			int amount = Math.min(18, 2 + p.get(HiddenParameter.PRECIPITATION_INTENSITY) * 2);
			double wind = p.get(HiddenParameter.WIND_STRENGTH) * .012;
			double angle = UniverseWeather.windAngle(p, client.level.getGameTime());
			for (int i = 0; i < amount; i++) {
				double x = client.player.getX() + random.nextDouble() * 28 - 14;
				double y = client.player.getY() + random.nextDouble() * 12 + 3;
				double z = client.player.getZ() + random.nextDouble() * 28 - 14;
				if (!client.level.canSeeSky(BlockPos.containing(x, y, z))) continue;
				int selected = type == 9 ? random.nextInt(9) : type;
				ParticleOptions particle = switch (selected) {
					case 1 -> ParticleTypes.SNOWFLAKE; case 2 -> new ItemParticleOption(ParticleTypes.ITEM, Items.SNOWBALL);
					case 5 -> ParticleTypes.WHITE_ASH; case 6 -> ParticleTypes.ASH;
					case 7 -> ParticleTypes.END_ROD; case 8 -> ParticleTypes.CRIT; default -> ParticleTypes.RAIN;
				};
				client.level.addParticle(particle, x, y, z, Math.sin(angle) * wind, selected == 2 || selected == 4 ? -.8 : -.15, Math.cos(angle) * wind);
			}
		});
	}
}
