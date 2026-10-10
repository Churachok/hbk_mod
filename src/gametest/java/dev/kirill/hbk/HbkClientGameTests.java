package dev.kirill.hbk;

import dev.kirill.hbk.world.BusClientGameTest;
import dev.kirill.hbk.client.SquirrelClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Select a scenario with ./gradlew runClientGameTest -PhbkClientTest=kitchen|blood|bus|squirrel|all. */
public final class HbkClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		switch (System.getProperty("hbk.client.gametest", "all")) {
			case "kitchen" -> new KitchenClientGameTest().runTest(context);
			case "bus" -> new BusClientGameTest().runTest(context);
			case "blood" -> new LiberalBloodClientGameTest().runTest(context);
			case "squirrel" -> new SquirrelClientGameTest().runTest(context);
			case "all" -> {
				new KitchenClientGameTest().runTest(context);
				new LiberalBloodClientGameTest().runTest(context);
				new SquirrelClientGameTest().runTest(context);
				new BusClientGameTest().runTest(context);
			}
			default -> throw new IllegalArgumentException("hbkClientTest must be kitchen, blood, bus, squirrel or all");
		}
	}
}
