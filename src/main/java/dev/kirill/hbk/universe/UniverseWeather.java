package dev.kirill.hbk.universe;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class UniverseWeather {
	private UniverseWeather() {}
	public static float rain(UniverseProfile p, long time) {
		int frequency = p.get(HiddenParameter.PRECIPITATION_FREQUENCY);
		if (frequency == 0) return 0;
		double roll = UniverseNoise.unit(p.stream(30) ^ Math.floorDiv(time, 6000));
		return frequency == 9 || roll < frequency / 10.0 ? .08f + p.get(HiddenParameter.PRECIPITATION_INTENSITY) * .1f : 0;
	}
	public static void tick(ServerLevel level, UniverseProfile p) {
		float previous = level.getRainLevel(1), thunder = level.getThunderLevel(1);
		float rain = rain(p, level.getGameTime());
		float storm = rain > 0 && UniverseNoise.unit(p.stream(31) ^ Math.floorDiv(level.getGameTime(), 3000)) < p.get(HiddenParameter.THUNDER) / 9.0 ? 1 : 0;
		level.getWeatherData().setRaining(rain > 0);
		level.getWeatherData().setThundering(storm > 0);
		level.setRainLevel(rain); level.setThunderLevel(storm);
		if (previous != rain || thunder != storm) for (var player : level.players()) {
			player.connection.send(new ClientboundGameEventPacket(rain > 0 ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0));
			player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rain));
			player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, storm));
		}
	}
	public static double windAngle(UniverseProfile p, long time) {
		int direction = p.get(HiddenParameter.WIND_DIRECTION);
		return direction < 8 ? direction * Math.PI / 4 : direction == 8 ? time / 6000.0 : UniverseNoise.noise(p.stream(32), time / 900.0, 0, 0) * Math.PI * 2;
	}
}
