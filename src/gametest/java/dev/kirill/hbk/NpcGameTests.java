package dev.kirill.hbk;

import dev.kirill.hbk.effect.GoshasRageEffect;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import dev.kirill.hbk.world.ModWorldData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class NpcGameTests {
	@GameTest
	public void hbkCreativeTabContainsEveryModItem(GameTestHelper test) {
		var tab = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB
				.getValue(ModItems.HBK_CREATIVE_TAB_KEY);
		test.assertTrue(tab != null, "HBK creative tab must be registered");
		test.assertTrue(HbkMod.id("hbk_logo").equals(tab.getIconItem().get(
				net.minecraft.core.component.DataComponents.ITEM_MODEL)),
				"HBK creative tab must use the custom HBK logo model");
		tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(
				test.getLevel().enabledFeatures(), true, test.getLevel().registryAccess()));
		var displayedItems = tab.getDisplayItems().stream()
				.map(net.minecraft.world.item.ItemStack::getItem)
				.collect(java.util.stream.Collectors.toSet());
		var registeredModItems = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
				.filter(item -> HbkMod.MOD_ID.equals(
						net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace()))
				.collect(java.util.stream.Collectors.toSet());
		test.assertTrue(displayedItems.equals(registeredModItems),
				"HBK creative tab must contain every registered mod item exactly once");
		test.succeed();
	}

	@GameTest
	public void memberWeaponsHaveNoRecipes(GameTestHelper test) {
		var level = test.getLevel();
		var displayContext = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(level);
		for (var recipe : level.getServer().getRecipeManager().getRecipes()) {
			for (var display : recipe.value().display()) {
				for (var result : display.result().resolveForStacks(displayContext)) {
					test.assertFalse(ModItems.isMemberWeapon(result), "Member weapons must be loot-only, found recipe " + recipe.id());
				}
			}
		}
		for (var item : java.util.List.of(ModItems.URANIUM_HELMET, ModItems.URANIUM_CHESTPLATE,
				ModItems.URANIUM_LEGGINGS, ModItems.URANIUM_BOOTS)) {
			var equippable = item.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
			test.assertTrue(equippable != null && equippable.assetId().orElseThrow().identifier().equals(HbkMod.id("uranium")),
					"All uranium armour must reference the hbk equipment asset");
		}
		test.succeed();
	}

	@GameTest
	public void deathDropsUseLootTableOnly(GameTestHelper test) {
		checkDeathDrops(test, 0.0f);
		test.succeed();
	}

	@GameTest
	public void savedGuaranteedEquipmentDoesNotBypassDropChance(GameTestHelper test) {
		// Old entities may retain a guaranteed equipment drop in their saved data.
		checkDeathDrops(test, 2.0f);
		test.succeed();
	}

	private static void checkDeathDrops(GameTestHelper test, float equipmentChance) {
		var types = java.util.List.of(ModEntityTypes.KIRILL, ModEntityTypes.LIZA, ModEntityTypes.SASHA,
				ModEntityTypes.GRISHA, ModEntityTypes.GOSHA, ModEntityTypes.VLAD, ModEntityTypes.LESHA);
		var items = java.util.List.of(ModItems.ATTACKING_MEMBER, ModItems.FEMALE_VAGINA, ModItems.ARMORED_MEMBER,
				ModItems.HAMMER_FIGHTER_MEMBER, ModItems.BEASTLIKE_MEMBER, ModItems.CARRIER_MEMBER, ModItems.COLOSSAL_MEMBER);
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var level = test.getLevel();
		for (int i = 0; i < types.size(); i++) {
			int drops = 0;
			int sickles = 0;
			for (int trial = 0; trial < 256; trial++) {
				var npc = (net.minecraft.world.entity.Mob) test.spawn(types.get(i), 2, 2, 2);
				npc.setItemSlot(EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(items.get(i)));
				npc.setDropChance(EquipmentSlot.MAINHAND, equipmentChance);
				npc.hurtServer(level, level.damageSources().playerAttack(player), 1000);
				test.assertFalse(npc.isAlive(), "Death-drop trial must kill the NPC");
				for (var entity : test.getEntities(EntityTypes.ITEM)) {
					if (entity.getItem().is(items.get(i))) {
						drops += entity.getItem().getCount();
					} else if (entity.getItem().is(ModItems.SICKLE_AND_HAMMER)) {
						sickles += entity.getItem().getCount();
					}
					entity.discard();
				}
				npc.discard();
			}
			test.assertTrue(drops >= 20 && drops <= 85,
					"Expected approximately 20% drops for " + types.get(i) + ", got " + drops + "/256; equipmentChance=" + equipmentChance);
			if (types.get(i) == ModEntityTypes.GRISHA) {
				test.assertTrue(sickles >= 20 && sickles <= 85, "Grisha's second item must also have 20% chance, got " + sickles);
			}
		}
		for (int trial = 0; trial < 32; trial++) {
			var anton = test.spawn(ModEntityTypes.ANTON, 2, 2, 2);
			anton.hurtServer(level, level.damageSources().generic(), 1000);
			int buckwheat = 0;
			for (var entity : test.getEntities(EntityTypes.ITEM)) {
				if (entity.getItem().is(ModItems.BUCKWHEAT)) {
					buckwheat += entity.getItem().getCount();
				}
				entity.discard();
			}
			test.assertTrue(buckwheat == 1, "Anton must always drop exactly one buckwheat, even without a player kill");
			anton.discard();
		}
	}

	@GameTest
	public void goshasRagePotionDropsAtRequestedRates(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		int goshaPotions = 0;
		int pillagerPotions = 0;
		for (int trial = 0; trial < 256; trial++) {
			var gosha = test.spawn(ModEntityTypes.GOSHA, 2, 2, 2);
			gosha.hurtServer(level, level.damageSources().playerAttack(player), 1000);
			goshaPotions += collectRagePotions(test);
			gosha.discard();

			var pillager = test.spawn(EntityTypes.PILLAGER, 2, 2, 2);
			pillager.hurtServer(level, level.damageSources().playerAttack(player), 1000);
			pillagerPotions += collectRagePotions(test);
			pillager.discard();
		}
		test.assertTrue(goshaPotions >= 90 && goshaPotions <= 170,
				"Gosha must drop the potion approximately 50% of the time, got " + goshaPotions + "/256");
		test.assertTrue(pillagerPotions >= 10 && pillagerPotions <= 45,
				"Pillagers must drop the potion approximately 10% of the time, got " + pillagerPotions + "/256");
		var drinker = test.spawn(EntityTypes.COW, 3, 2, 2);
		ModItems.GOSHAS_RAGE_BOTTLE.finishUsingItem(
				new net.minecraft.world.item.ItemStack(ModItems.GOSHAS_RAGE_BOTTLE), level, drinker);
		var rage = drinker.getEffect(ModEffects.GOSHAS_RAGE);
		test.assertTrue(rage != null && rage.getDuration() == GoshasRageEffect.DURATION_TICKS,
				"The registered bottle item must apply two minutes of Gosha's Rage when drunk");
		drinker.discard();
		test.succeed();
	}

	private static int collectRagePotions(GameTestHelper test) {
		int count = 0;
		for (var entity : test.getEntities(EntityTypes.ITEM)) {
			var stack = entity.getItem();
			if (stack.is(ModItems.GOSHAS_RAGE_BOTTLE)) {
				count += stack.getCount();
			}
			entity.discard();
		}
		return count;
	}

	@GameTest
	public void sashaCadenceSurvivesSaveAndReload(GameTestHelper test) {
		ModWorldData data = new ModWorldData();
		test.assertFalse(data.isSashaSpawnRollDue(), "No roll before exploring chunks");
		for (long key = 1; key <= 100; key++) {
			test.assertFalse(data.isNpcChunkHandled(key), "New chunks must not already be handled");
			data.markNpcChunkHandled(key);
			test.assertTrue(data.isSashaSpawnRollDue() == (key % 20 == 0), "Roll only every 20 unique chunks");
			data.markNpcChunkHandled(key);
			test.assertTrue(data.isSashaSpawnRollDue() == (key % 20 == 0), "Reloading a chunk must not advance cadence");
			var json = ModWorldData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, data).getOrThrow();
			data = ModWorldData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
			test.assertTrue(data.isNpcChunkHandled(key), "Handled chunks must persist");
			test.assertTrue(data.isSashaSpawnRollDue() == (key % 20 == 0), "Cadence must persist");
		}
		test.succeed();
	}

	@GameTest
	public void denisHouseResourcesLoad(GameTestHelper test) {
		var level = test.getLevel();
		var id = HbkMod.id("denis_house");
		level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
				.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, id));
		level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL)
				.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.TEMPLATE_POOL, id));
		level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET)
				.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE_SET, HbkMod.id("denis_houses")));
		var template = level.getStructureManager().get(id).orElseThrow();
		test.assertTrue(template.getSize().getX() > 0 && template.getSize().getY() > 0 && template.getSize().getZ() > 0,
				"Imported Denis house must be a nonempty structure template");
		test.succeed();
	}

	@GameTest
	public void healthAndPermanentWeapons(GameTestHelper test) {
		var anton = test.spawn(ModEntityTypes.ANTON, 1, 2, 1);
		var lesha = test.spawn(ModEntityTypes.LESHA, 2, 2, 1);
		test.assertTrue(anton.getMaxHealth() == 1 && lesha.getMaxHealth() == 1, "Anton and Lesha must have 1 HP");
		for (var type : java.util.List.of(ModEntityTypes.DENIS, ModEntityTypes.GOSHA,
				ModEntityTypes.GRISHA, ModEntityTypes.SASHA, ModEntityTypes.VLAD)) {
			var npc = test.spawn(type, 3, 2, 1);
			test.assertTrue(npc.getMaxHealth() == 20, "Other humanoids must have 20 HP");
			npc.discard();
		}
		var sasha = test.spawn(ModEntityTypes.SASHA, 3, 2, 1);
		var vlad = test.spawn(ModEntityTypes.VLAD, 4, 2, 1);
		test.assertTrue(sasha.getMainHandItem().is(ModItems.ARMORED_MEMBER), "Sasha always carries armored member");
		test.assertTrue(vlad.getMainHandItem().is(ModItems.CARRIER_MEMBER), "Vlad always carries carrier member");
		test.assertTrue(sasha.getDropChances().byEquipment(EquipmentSlot.MAINHAND) == 0, "Sasha's equipment must not add another drop roll");
		test.assertFalse(sasha.canAttack(lesha), "Sasha must never attack Lesha");
		test.succeed();
	}

	@GameTest
	public void retaliationEquipsWeapons(GameTestHelper test) {
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var types = java.util.List.of(ModEntityTypes.DENIS, ModEntityTypes.GOSHA, ModEntityTypes.GRISHA);
		var weapons = java.util.List.of(ModItems.JAW_MEMBER, ModItems.BEASTLIKE_MEMBER, ModItems.HAMMER_FIGHTER_MEMBER);
		for (int i = 0; i < types.size(); i++) {
			var npc = test.spawn(types.get(i), 1 + i, 2, 1);
			test.assertTrue(npc.getTarget() == null && npc.getMainHandItem().isEmpty(), "Neutral mobs initially have no target or weapon");
			boolean hurt = npc.hurtServer(test.getLevel(), test.getLevel().damageSources().playerAttack(player), 1);
			test.assertTrue(hurt, "NPC must receive the hit: " + types.get(i));
			test.assertTrue(npc.getMainHandItem().is(weapons.get(i)), "NPC must draw its weapon: " + types.get(i) + "; canAttack=" + npc.canAttack(player));
			test.assertTrue(npc.getTarget() == player, "NPC must target attacker: " + types.get(i));
			test.assertTrue(npc.getDropChances().byEquipment(EquipmentSlot.MAINHAND) == 0, "Neutral NPC equipment must not add another drop roll");
		}
		var vlad = test.spawn(ModEntityTypes.VLAD, 4, 2, 1);
		vlad.hurtServer(test.getLevel(), test.getLevel().damageSources().playerAttack(player), 1);
		test.assertTrue(vlad.getTarget() == null && !vlad.canAttack(player), "Vlad stays friendly after being hit");
		test.succeed();
	}

	@GameTest(maxTicks = 230)
	public void pinkFurryWolfFleesAfterBeingHit(GameTestHelper test) {
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var wolf = test.spawn(ModEntityTypes.PINK_FURRY_WOLF, 2, 2, 2);
		boolean hurt = wolf.hurtServer(test.getLevel(), test.getLevel().damageSources().playerAttack(player), 1);
		test.assertTrue(hurt, "Pink furry wolf must receive ordinary damage");
		test.assertTrue(wolf.getMaxHealth() == 30, "Pink furry wolf must have 30 HP");
		test.assertTrue(wolf.getTarget() == null, "Pink furry wolf must never target its attacker");
		test.assertFalse(wolf.canAttack(player), "Pink furry wolf must never acquire an attack target");
		test.assertTrue(wolf.getMainHandItem().isEmpty(), "Pink furry wolf must not carry a weapon");
		test.assertTrue(wolf.isFleeingFrom(player), "Pink furry wolf must flee from the entity that hit it");
		test.assertTrue(wolf.isFleeing(), "Pink furry wolf must enter its synchronized four-legged fleeing state");
		test.runAfterDelay(199, () -> test.assertTrue(wolf.isFleeing(), "Fleeing must last the full ten seconds"));
		test.runAfterDelay(202, () -> {
			test.assertFalse(wolf.isFleeing(), "Pink furry wolf must stop fleeing after ten seconds");
			test.assertTrue(wolf.getTarget() == null, "Pink furry wolf must remain peaceful after fleeing");
			test.succeed();
		});
	}

	@GameTest
	public void pinkFurryWolfRarelyRetaliatesWithGoshasRage(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		int retaliations = 0;
		for (int trial = 0; trial < 256; trial++) {
			player.removeEffect(ModEffects.GOSHAS_RAGE);
			player.setHealth(10.0f);
			player.invulnerableTime = 0;
			var wolf = test.spawn(ModEntityTypes.PINK_FURRY_WOLF, 2, 2, 2);
			boolean hurt = wolf.hurtServer(level, level.damageSources().playerAttack(player), 1.0f);
			test.assertTrue(hurt && wolf.isFleeingFrom(player),
					"Furry wolf must receive the hit and flee regardless of retaliation");
			var rage = player.getEffect(ModEffects.GOSHAS_RAGE);
			if (rage != null) {
				retaliations++;
				test.assertTrue(rage.getDuration() == GoshasRageEffect.DURATION_TICKS,
						"A retaliating furry wolf must apply two minutes of Gosha's Rage");
				test.assertTrue(Math.abs(player.getHealth() - 4.0f) < 0.001f,
						"The retaliation must deal exactly three hearts before applying the effect");
				test.assertTrue(player.getLastDamageSource() != null
						&& player.getLastDamageSource().getEntity() == wolf,
						"The furry wolf must be the source of retaliation damage");
			} else {
				test.assertTrue(player.getHealth() == 10.0f,
						"A normal flee reaction must not damage the player");
			}
			test.assertTrue(wolf.getTarget() == null && !wolf.canAttack(player),
					"Retaliation must not turn the furry wolf into an aggressive mob");
			wolf.discard();
		}
		player.removeEffect(ModEffects.GOSHAS_RAGE);
		player.discard();
		test.assertTrue(retaliations >= 10 && retaliations <= 50,
				"Expected approximately 10% furry retaliation, got " + retaliations + "/256");
		test.succeed();
	}

	@GameTest
	public void pinkFurryWolfDropsPinkWool(GameTestHelper test) {
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var level = test.getLevel();
		int successfulDrops = 0;
		for (int trial = 0; trial < 256; trial++) {
			var wolf = test.spawn(ModEntityTypes.PINK_FURRY_WOLF, 2, 2, 2);
			var color = net.minecraft.world.item.DyeColor.VALUES.get(trial % net.minecraft.world.item.DyeColor.VALUES.size());
			wolf.setFurColor(color);
			wolf.hurtServer(level, level.damageSources().playerAttack(player), 1000);
			test.assertFalse(wolf.isAlive(), "Pink furry wolf drop trial must kill the wolf");
			for (var entity : test.getEntities(EntityTypes.ITEM)) {
				if (entity.getItem().is(net.minecraft.world.item.Items.WOOL.pick(color))) {
					int count = entity.getItem().getCount();
					test.assertTrue(count >= 1 && count <= 3, "Matching wool drop must contain 1-3 blocks, got " + count);
					successfulDrops++;
				} else {
					test.fail("Furry wolf must not drop wool of a different colour: expected " + color.getName());
				}
				entity.discard();
			}
			wolf.discard();
		}
		test.assertTrue(successfulDrops >= 25 && successfulDrops <= 80,
				"Expected approximately 20% matching wool drops, got " + successfulDrops + "/256");
		test.succeed();
	}

	@GameTest
	public void pinkFurryWolfSupportsEverySheepColor(GameTestHelper test) {
		var wolf = test.spawn(ModEntityTypes.PINK_FURRY_WOLF, 2, 2, 2);
		test.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
				.getKey(ModEntityTypes.PINK_FURRY_WOLF).equals(HbkMod.id("furry_wolf")),
				"Furry wolf summon identifier must not contain the old pink prefix");
		for (var color : net.minecraft.world.item.DyeColor.VALUES) {
			wolf.setFurColor(color);
			test.assertTrue(wolf.getFurColor() == color, "Pink furry wolf must retain colour " + color.getName());
		}
		test.assertTrue(ModItems.PINK_FURRY_WOLF_SPAWN_EGG instanceof net.minecraft.world.item.SpawnEggItem,
				"All colour variants must continue to use the single furry wolf spawn egg");
		test.succeed();
	}

	@GameTest
	public void catgirlIsPeacefulAndUsesHerOwnEgg(GameTestHelper test) {
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var catgirl = test.spawn(ModEntityTypes.CATGIRL, 2, 2, 2);
		test.assertTrue(catgirl.getMaxHealth() == 20, "Catgirl must have 20 HP");
		test.assertTrue(catgirl.getTarget() == null && !catgirl.canAttack(player), "Catgirl must be completely peaceful");
		test.assertTrue(ModItems.CATGIRL_SPAWN_EGG instanceof net.minecraft.world.item.SpawnEggItem,
				"Catgirl must have her own spawn egg");
		test.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
				.getKey(ModEntityTypes.CATGIRL).equals(HbkMod.id("catgirl")),
				"Catgirl summon identifier must be hbk:catgirl");
		test.succeed();
	}

	@GameTest
	public void goshasRageHalvesHealthAndTriplesPlayerDamage(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var gosha = test.spawn(ModEntityTypes.GOSHA, 2, 2, 2);
		var victim = test.spawn(EntityTypes.COW, 3, 2, 2);
		var waterPos = test.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
		try {
			player.invulnerableTime = 0;
			player.setPos(test.absoluteVec(new Vec3(2.5, 2, 2.5)));
			player.setHealth(player.getMaxHealth());
			test.assertTrue(gosha.doHurtTarget(level, player), "Gosha's successful hit must hurt the player");
			var rage = player.getEffect(ModEffects.GOSHAS_RAGE);
			test.assertTrue(rage != null && rage.getDuration() == GoshasRageEffect.DURATION_TICKS,
					"Gosha's hit must apply two minutes of rage");
			test.assertTrue(player.getMaxHealth() == 10.0f && player.getHealth() <= 10.0f,
					"Gosha's Rage must halve maximum health and clamp current health");

			victim.setHealth(victim.getMaxHealth());
			victim.hurtServer(level, level.damageSources().playerAttack(player), 2.0f);
			test.assertTrue(victim.getHealth() == victim.getMaxHealth() - 6.0f,
					"All player-caused damage must be tripled by Gosha's Rage");

			level.setBlockAndUpdate(waterPos, Blocks.WATER.defaultBlockState());
			victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.GOSHAS_RAGE,
					GoshasRageEffect.DURATION_TICKS));
			victim.setPos(Vec3.atCenterOf(waterPos));
			victim.setHealth(victim.getMaxHealth());
			victim.tick();
			test.assertTrue(victim.isInWaterOrRain(), "The water-damage fixture must put its victim in water");
			victim.tickCount = 20;
			victim.invulnerableTime = 0;
			float healthBeforeWater = victim.getHealth();
			ModEffects.GOSHAS_RAGE.value().applyEffectTick(level, victim, 0);
			test.assertTrue(victim.getHealth() < healthBeforeWater,
					"Water contact must damage an entity with Gosha's Rage");

			player.removeEffect(ModEffects.GOSHAS_RAGE);
			test.assertTrue(player.getMaxHealth() == 20.0f,
					"Maximum health must return to normal when Gosha's Rage ends");
		} finally {
			player.discard();
			level.removeBlock(waterPos, false);
			gosha.discard();
			victim.discard();
		}
		test.succeed();
	}

	@GameTest
	public void lexSurvivesDamageAndKill(GameTestHelper test) {
		var lex = test.spawn(ModEntityTypes.LEX, 1, 2, 1);
		var level = test.getLevel();
		float health = lex.getHealth();
		lex.hurtServer(level, level.damageSources().generic(), 1000);
		lex.hurtServer(level, level.damageSources().fellOutOfWorld(), 1000);
		lex.kill(level);
		lex.die(level.damageSources().generic());
		test.assertTrue(lex.isAlive() && lex.getHealth() == health && health == 20, "Lex must survive all damage and /kill with 20 HP");
		test.succeed();
	}

	@GameTest(maxTicks = 100)
	public void zombieHuntsAntonAndBuckwheatDrops(GameTestHelper test) {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				test.setBlock(x, 1, z, Blocks.STONE);
				test.setBlock(x, 4, z, Blocks.STONE);
			}
		}
		var anton = test.spawn(ModEntityTypes.ANTON, 2, 2, 2);
		anton.setNoAi(true);
		var zombie = test.spawn(EntityTypes.ZOMBIE, 3, 2, 2);
		zombie.setPersistenceRequired();
		test.succeedWhen(() -> {
			test.assertFalse(anton.isAlive(), "A hostile mob must kill Anton without provocation");
			test.assertItemEntityPresent(ModItems.BUCKWHEAT);
		});
	}

	@GameTest(maxTicks = 160)
	public void sashaHuntsAntonAndSparesLesha(GameTestHelper test) {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				test.setBlock(x, 1, z, Blocks.STONE);
			}
		}
		var sasha = test.spawn(ModEntityTypes.SASHA, 2, 2, 2);
		sasha.setPersistenceRequired();
		var lesha = test.spawn(ModEntityTypes.LESHA, 3, 2, 2);
		lesha.setNoAi(true);
		var anton = test.spawn(ModEntityTypes.ANTON, 1, 2, 2);
		anton.setNoAi(true);
		test.succeedWhen(() -> {
			test.assertFalse(anton.isAlive(), "The new hostile NPC must also hunt Anton");
			test.assertTrue(lesha.isAlive() && !sasha.canAttack(lesha), "Sasha must spare Lesha");
		});
	}

	@GameTest(maxTicks = 160)
	public void goshaHuntsSheepUnarmed(GameTestHelper test) {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				test.setBlock(x, 1, z, Blocks.STONE);
			}
		}
		var gosha = test.spawn(ModEntityTypes.GOSHA, 2, 2, 2);
		var sheep = test.spawn(EntityTypes.SHEEP, 3, 2, 2);
		sheep.setNoAi(true);
		test.succeedWhen(() -> {
			test.assertFalse(sheep.isAlive(), "Gosha must hunt sheep on sight");
			test.assertTrue(gosha.getMainHandItem().isEmpty(), "Gosha draws his weapon only after being attacked");
		});
	}
}
