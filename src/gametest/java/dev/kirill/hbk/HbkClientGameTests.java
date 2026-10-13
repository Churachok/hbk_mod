package dev.kirill.hbk;

import dev.kirill.hbk.client.BeerBottleClientGameTest;
import dev.kirill.hbk.client.BeerCauldronClientGameTest;
import dev.kirill.hbk.client.SquirrelClientGameTest;
import dev.kirill.hbk.world.BusClientGameTest;
import dev.kirill.hbk.world.KonataHouseClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Select a scenario with ./gradlew runClientGameTest -PhbkClientTest=kitchen|blood|bus|squirrel|carpet|beer|konata|all. */
public final class HbkClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		switch (System.getProperty("hbk.client.gametest", "all")) {
			case "kitchen" -> new KitchenClientGameTest().runTest(context);
			case "bus" -> new BusClientGameTest().runTest(context);
			case "blood" -> new LiberalBloodClientGameTest().runTest(context);
			case "squirrel" -> new SquirrelClientGameTest().runTest(context);
			case "carpet" -> new FlyingCarpetClientGameTest().runTest(context);
			case "beer" -> runBeerTests(context);
			case "konata" -> new KonataHouseClientGameTest().runTest(context);
			case "all" -> {
				new KitchenClientGameTest().runTest(context);
				new LiberalBloodClientGameTest().runTest(context);
				new SquirrelClientGameTest().runTest(context);
				runBeerTests(context);
				new KonataHouseClientGameTest().runTest(context);
				new BusClientGameTest().runTest(context);
				new FlyingCarpetClientGameTest().runTest(context);
			}
			default -> throw new IllegalArgumentException("hbkClientTest must be kitchen, blood, bus, squirrel, carpet, beer, konata or all");
		}
	}

	private static void runBeerTests(ClientGameTestContext context) {
		new BeerBottleClientGameTest().runTest(context);
		new BeerCauldronClientGameTest().runTest(context);
	}
}
