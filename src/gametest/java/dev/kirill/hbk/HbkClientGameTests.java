package dev.kirill.hbk;

import dev.kirill.hbk.world.BusClientGameTest;
import dev.kirill.hbk.client.SquirrelClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Select a scenario with ./gradlew runClientGameTest -PhbkClientTest=time_furnace|kitchen|blood|bus|squirrel|carpet|rofl|universe|all. */
public final class HbkClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		switch (System.getProperty("hbk.client.gametest", "all")) {
			case "worlds" -> new UniverseMinesClientGameTest().runTest(context);
			case "alloys" -> new UniverseAlloyClientGameTest().runTest(context);
			case "universe" -> new UniverseClientGameTest().runTest(context);
			case "time_furnace" -> new TimeFurnaceClientGameTest().runTest(context);
			case "anomaly" -> new AnomalyClientGameTest().runTest(context);
			case "kitchen" -> new KitchenClientGameTest().runTest(context);
			case "bus" -> new BusClientGameTest().runTest(context);
			case "blood" -> new LiberalBloodClientGameTest().runTest(context);
			case "squirrel" -> new SquirrelClientGameTest().runTest(context);
			case "carpet" -> new FlyingCarpetClientGameTest().runTest(context);
			case "rofl" -> new RoflClientGameTest().runTest(context);
			case "all" -> {
				new UniverseClientGameTest().runTest(context);
				new UniverseAlloyClientGameTest().runTest(context);
				new UniverseMinesClientGameTest().runTest(context);
				new TimeFurnaceClientGameTest().runTest(context);
				new AnomalyClientGameTest().runTest(context);
				new KitchenClientGameTest().runTest(context);
				new LiberalBloodClientGameTest().runTest(context);
				new SquirrelClientGameTest().runTest(context);
				new BusClientGameTest().runTest(context);
				new FlyingCarpetClientGameTest().runTest(context);
				new RoflClientGameTest().runTest(context);
			}
			default -> throw new IllegalArgumentException("hbkClientTest must be time_furnace, kitchen, blood, bus, squirrel, carpet, rofl, anomaly, universe, alloys, worlds or all");
		}
	}
}
