package dev.kirill.hbk;

import dev.kirill.hbk.client.BeerBottleClientGameTest;
import dev.kirill.hbk.client.BeerCauldronClientGameTest;
import dev.kirill.hbk.client.JagermeisterClientGameTest;
import dev.kirill.hbk.client.HuntersInstinctClientGameTest;
import dev.kirill.hbk.client.FurrySyringeClientGameTest;
import dev.kirill.hbk.client.FurryBreedingClientGameTest;
import dev.kirill.hbk.client.SquirrelClientGameTest;
import dev.kirill.hbk.world.BusClientGameTest;
import dev.kirill.hbk.world.KonataHouseClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Select a scenario with -PhbkClientTest; jagermeister_jei additionally requires -PhbkJei=true. */
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
			case "jagermeister" -> new JagermeisterClientGameTest().runTest(context);
			case "hunters_instinct" -> new HuntersInstinctClientGameTest().runTest(context);
			case "furry" -> new FurrySyringeClientGameTest().runTest(context);
			case "furry_breeding" -> new FurryBreedingClientGameTest().runTest(context);
			case "jagermeister_jei" -> new JeiJagermeisterClientGameTest().runTest(context);
			case "rofl" -> new RoflClientGameTest().runTest(context);
			case "all" -> {
				new KitchenClientGameTest().runTest(context);
				new LiberalBloodClientGameTest().runTest(context);
				new SquirrelClientGameTest().runTest(context);
				runBeerTests(context);
				new JagermeisterClientGameTest().runTest(context);
				new HuntersInstinctClientGameTest().runTest(context);
				new FurrySyringeClientGameTest().runTest(context);
				new FurryBreedingClientGameTest().runTest(context);
				new KonataHouseClientGameTest().runTest(context);
				new BusClientGameTest().runTest(context);
				new FlyingCarpetClientGameTest().runTest(context);
				new RoflClientGameTest().runTest(context);
			}
			default -> throw new IllegalArgumentException("hbkClientTest must be kitchen, blood, bus, squirrel, carpet, beer, konata, jagermeister, hunters_instinct, furry, furry_breeding, jagermeister_jei, rofl or all");
		}
	}

	private static void runBeerTests(ClientGameTestContext context) {
		new BeerBottleClientGameTest().runTest(context);
		new BeerCauldronClientGameTest().runTest(context);
	}
}
