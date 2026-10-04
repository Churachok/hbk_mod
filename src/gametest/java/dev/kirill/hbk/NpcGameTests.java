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
			test.assertTrue(data.isSashaSpawnRollDue() == (key % 5 == 0), "Roll only every five unique chunks");
			data.markNpcChunkHandled(key);
			test.assertTrue(data.isSashaSpawnRollDue() == (key % 5 == 0), "Reloading a chunk must not advance cadence");
			var json = ModWorldData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, data).getOrThrow();
			data = ModWorldData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
			test.assertTrue(data.isNpcChunkHandled(key), "Handled chunks must persist");
			test.assertTrue(data.isSashaSpawnRollDue() == (key % 5 == 0), "Cadence must persist");
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
	public void konataHouseIsSmallAndRarerThanKirillHouse(GameTestHelper test) {
		var level = test.getLevel();
		var structureId = HbkMod.id("konata_house");
		level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
				.getOrThrow(net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.STRUCTURE, structureId));
		level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL)
				.getOrThrow(net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.TEMPLATE_POOL, structureId));
		var structureSets = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET);
		var konataSet = structureSets.getOrThrow(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.STRUCTURE_SET, HbkMod.id("konata_houses")));
		var kirillSet = structureSets.getOrThrow(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.STRUCTURE_SET, HbkMod.id("kirill_houses")));
		var konataPlacement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement)
				konataSet.value().placement();
		var kirillPlacement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement)
				kirillSet.value().placement();
		test.assertTrue(konataPlacement.spacing() > kirillPlacement.spacing(),
				"Konata's house must generate less often than Kirill's house");
		var template = level.getStructureManager().get(structureId).orElseThrow();
		test.assertTrue(template.getSize().getX() == 9 && template.getSize().getY() == 7
				&& template.getSize().getZ() == 9, "Konata's house must stay compact");
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
	public void konataHas20HealthAndMatchingSpawnEgg(GameTestHelper test) {
		var thirdVariant = test.spawn(ModEntityTypes.TEST3_KONATA, 8, 2, 2);
		test.assertTrue(thirdVariant.getType() != ModEntityTypes.TEST2_KONATA
				&& thirdVariant.getMaxHealth() == 20, "Test 3 must be a separate entity type with 20 HP");
		test.assertTrue(thirdVariant.getBbWidth() == 0.6f && thirdVariant.getBbHeight() == 1.8f,
				"Test 3 must retain Konata's original full-size dimensions after the model swap");
		var secondVariant = test.spawn(ModEntityTypes.TEST2_KONATA, 6, 2, 2);
		test.assertTrue(secondVariant.getType() != ModEntityTypes.KONATA
				&& secondVariant.getType() != ModEntityTypes.TEST_KONATA && secondVariant.getMaxHealth() == 20,
				"Test 2 must be a separate entity type with 20 HP");
		var variant = test.spawn(ModEntityTypes.TEST_KONATA, 4, 2, 2);
		test.assertTrue(variant.getType() != ModEntityTypes.KONATA && variant.getMaxHealth() == 20,
				"Test variant must be a separate entity type with 20 HP");
		var konata = test.spawn(ModEntityTypes.KONATA, 2, 2, 2);
		test.assertTrue(konata.getMaxHealth() == 20 && konata.getHealth() == 20,
				"Konata must spawn with 20 HP");
		test.assertTrue(konata.getBbWidth() == 0.55f && konata.getBbHeight() == 1.64f,
				"Konata must use Test 3's compact dimensions after the model swap");
		test.assertTrue(konata.getAmbientSoundInterval() == 240,
				"Konata's voice clips must use the longer roaming ambient interval");
		test.assertTrue(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
				.getKey(dev.kirill.hbk.registry.ModSounds.KONATA_AMBIENT)
				.equals(HbkMod.id("entity.konata.ambient")),
				"Konata's ambient voice event must be registered");
		for (var sound : java.util.List.of("shutdown", "startup", "info", "warning", "alert")) {
			test.assertTrue(NpcGameTests.class.getResource(
					"/assets/hbk/sounds/entity/konata/" + sound + ".ogg") != null,
					"Konata's clean voice clip must be packaged: " + sound);
		}
		for (var sound : java.util.List.of("pupue", "good")) {
			test.assertTrue(NpcGameTests.class.getResource(
					"/assets/hbk/sounds/entity/konata/" + sound + ".ogg") != null,
					"Konata's interaction voice clip must be packaged: " + sound);
		}
		test.assertTrue(ModItems.MUSIC_DISC_KONATA_THEME.components()
				.get(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE) != null,
				"Konata's theme disc must be jukebox-playable");
		test.assertTrue(NpcGameTests.class.getResource(
				"/assets/hbk/sounds/music_disc/konata_theme.ogg") != null,
				"Konata's theme must be packaged as a streaming music-disc sound");
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		test.assertFalse(konata.canAttack(player), "Konata must be peaceful");
		test.assertTrue(ModItems.KONATA_SPAWN_EGG.getDefaultInstance()
				.get(net.minecraft.core.component.DataComponents.ENTITY_DATA).type() == ModEntityTypes.KONATA,
				"Konata's egg must spawn Konata");
		int itemsBefore = test.getEntities(EntityTypes.ITEM).size();
		test.assertTrue(konata.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND, konata.position())
				== net.minecraft.world.InteractionResult.SUCCESS, "Konata must accept a right-click");
		var drops = test.getEntities(EntityTypes.ITEM);
		test.assertTrue(drops.size() == itemsBefore + 1
				&& drops.stream().anyMatch(entity -> entity.getItem().is(ModItems.SHPERMA)
						|| entity.getItem().is(ModItems.MUSIC_DISC_KONATA_THEME)),
				"Right-clicking Konata must drop exactly one of her two gifts");
		test.succeed();
	}

	@GameTest
	public void konataThemeDiscReplacesShpermaOnOneTenthOfOnePercentRolls(GameTestHelper test) {
		test.assertTrue(dev.kirill.hbk.entity.KonataEntity.createGiftForRoll(0.0f)
				.is(ModItems.MUSIC_DISC_KONATA_THEME),
				"A zero roll must select Konata's theme disc");
		test.assertTrue(dev.kirill.hbk.entity.KonataEntity.createGiftForRoll(0.000999f)
				.is(ModItems.MUSIC_DISC_KONATA_THEME),
				"Rolls below 0.001 must select Konata's theme disc");
		test.assertTrue(dev.kirill.hbk.entity.KonataEntity.createGiftForRoll(0.001f)
				.is(ModItems.SHPERMA),
				"The 0.1% boundary and higher rolls must select Shperma");
		test.succeed();
	}

	@GameTest
	public void konataLimitsEachPlayerToTenItemsPerDay(GameTestHelper test) {
		var level = test.getLevel();
		test.assertTrue(level.getGameRules().get(dev.kirill.hbk.registry.ModGameRules.KONATA_SHPERM_LIMIT) == 10,
				"Konata's configurable daily gift limit must default to ten");
		test.assertTrue(net.minecraft.core.registries.BuiltInRegistries.GAME_RULE
				.getValue(HbkMod.id("konata_shperm_limit"))
				== dev.kirill.hbk.registry.ModGameRules.KONATA_SHPERM_LIMIT,
				"Konata's daily gift limit must be a registered game rule");
		var dispatcher = level.getServer().getCommands().getDispatcher();
		var commandSource = level.getServer().createCommandSourceStack();
		for (String command : java.util.List.of(
				"konata shperm limit 12",
				"gamerule hbk:konata_shperm_limit 12")) {
			var parsed = dispatcher.parse(command, commandSource);
			test.assertFalse(parsed.getReader().canRead() || !parsed.getExceptions().isEmpty(),
					"Konata's limit command must parse completely: /" + command);
		}
		var konata = test.spawn(ModEntityTypes.KONATA, 2, 2, 2);
		var firstPlayer = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		for (int i = 0; i < 9; i++) {
			konata.interact(firstPlayer, net.minecraft.world.InteractionHand.MAIN_HAND, konata.position());
		}
		test.assertFalse(konata.isGoodGestureActive(),
				"Konata must reserve the wink and thumbs-up gesture for the tenth daily item");
		konata.interact(firstPlayer, net.minecraft.world.InteractionHand.MAIN_HAND, konata.position());
		test.assertTrue(test.getEntities(EntityTypes.ITEM).size() == 10,
				"Konata must give the first player exactly ten items");
		var firstClaim = firstPlayer.getAttachedOrCreate(
				dev.kirill.hbk.registry.ModAttachments.KONATA_DAILY_CLAIM);
		test.assertTrue(firstClaim.count() == 10,
				"Konata's daily item count must be stored on the player");
		test.assertTrue(konata.isGoodGestureActive(),
				"Konata must start her wink and thumbs-up gesture on the tenth daily item");

		konata.interact(firstPlayer, net.minecraft.world.InteractionHand.MAIN_HAND, konata.position());
		test.assertTrue(test.getEntities(EntityTypes.ITEM).size() == 10,
				"The eleventh click on the same day must not drop an item");

		var secondPlayer = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		konata.interact(secondPlayer, net.minecraft.world.InteractionHand.MAIN_HAND, konata.position());
		test.assertTrue(test.getEntities(EntityTypes.ITEM).size() == 11,
				"A second player must have an independent daily limit");

		long currentDay = level.getOverworldClockTime() / 24_000L;
		firstPlayer.setAttached(dev.kirill.hbk.registry.ModAttachments.KONATA_DAILY_CLAIM,
				new dev.kirill.hbk.player.KonataDailyClaim(currentDay - 1, 10));
		konata.interact(firstPlayer, net.minecraft.world.InteractionHand.MAIN_HAND, konata.position());
		test.assertTrue(test.getEntities(EntityTypes.ITEM).size() == 12,
				"The limit must reset at the start of a new Minecraft day");
		var resetClaim = firstPlayer.getAttachedOrCreate(
				dev.kirill.hbk.registry.ModAttachments.KONATA_DAILY_CLAIM);
		test.assertTrue(resetClaim.count() == 1 && resetClaim.day() == currentDay,
				"The new day's first item must start a new persistent counter");
		test.succeed();
	}

	@GameTest
	public void shpermaHealsOnTapAndFiresKirillBulletAfterCharging(GameTestHelper test) {
		var level = test.getLevel();
		var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var playerPos = test.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
		player.teleportTo(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
		player.setHealth(16.0f);
		player.getFoodData().setFoodLevel(16);
		var stack = new net.minecraft.world.item.ItemStack(ModItems.SHPERMA, 2);
		int duration = ModItems.SHPERMA.getUseDuration(stack, player);
		test.assertTrue(ModItems.SHPERMA.getUseAnimation(stack) == net.minecraft.world.item.ItemUseAnimation.BOW,
				"Shperma must use the bow charging animation");

		boolean tapped = ModItems.SHPERMA.releaseUsing(stack, level, player, duration);
		test.assertTrue(tapped, "A quick release must be handled");
		test.assertTrue(player.getHealth() == 18.0f && player.getFoodData().getFoodLevel() == 18,
				"A quick release must restore exactly one heart and one full hunger icon");
		test.assertTrue(stack.getCount() == 1, "Healing must consume one item");

		int bulletsBefore = test.getEntities(ModEntityTypes.ATTACKING_MEMBER_BULLET).size();
		boolean fired = ModItems.SHPERMA.releaseUsing(stack, level, player,
				duration - dev.kirill.hbk.item.ShpermaItem.CHARGE_TICKS);
		var bullets = test.getEntities(ModEntityTypes.ATTACKING_MEMBER_BULLET);
		test.assertTrue(fired && bullets.size() == bulletsBefore + 1,
				"A one-second charge must fire one of Kirill's bullets");
		var bullet = bullets.getLast();
		test.assertTrue(bullet.getOwner() == player && bullet.getDeltaMovement().lengthSqr() > 1.0,
				"The charged projectile must belong to the player and travel at bullet speed");
		test.assertTrue(stack.isEmpty(), "Firing must consume one item");
		test.succeed();
	}

	@GameTest
	public void konataGiftAchievementsUseDiscPickupAndShpermaEatingOnly(GameTestHelper test) {
		var server = test.getLevel().getServer();
		var musicLover = server.getAdvancements().get(HbkMod.id("music_lover"));
		var sweetTooth = server.getAdvancements().get(HbkMod.id("sweet_tooth"));
		test.assertTrue(musicLover != null && sweetTooth != null,
				"Both Konata gift advancements must load");
		var player = test.makeMockServerPlayerInLevel();
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

		var dirt = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT);
		player.getInventory().setItem(0, dirt);
		net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED
				.trigger(player, player.getInventory(), dirt);
		test.assertFalse(player.getAdvancements().getOrStartProgress(musicLover).isDone(),
				"Unrelated items must not grant Music Lover");
		var disc = new net.minecraft.world.item.ItemStack(ModItems.MUSIC_DISC_KONATA_THEME);
		player.getInventory().setItem(0, disc);
		net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED
				.trigger(player, player.getInventory(), disc);
		test.assertTrue(player.getAdvancements().getOrStartProgress(musicLover).isDone(),
				"Obtaining Konata's theme disc must grant Music Lover");

		var projectileStack = new net.minecraft.world.item.ItemStack(ModItems.SHPERMA);
		int duration = ModItems.SHPERMA.getUseDuration(projectileStack, player);
		ModItems.SHPERMA.releaseUsing(projectileStack, test.getLevel(), player,
				duration - dev.kirill.hbk.item.ShpermaItem.CHARGE_TICKS);
		test.assertFalse(player.getAdvancements().getOrStartProgress(sweetTooth).isDone(),
				"Firing Shperma must not grant Sweet Tooth");
		player.setHealth(16.0f);
		var foodStack = new net.minecraft.world.item.ItemStack(ModItems.SHPERMA);
		ModItems.SHPERMA.releaseUsing(foodStack, test.getLevel(), player,
				ModItems.SHPERMA.getUseDuration(foodStack, player));
		test.assertTrue(player.getAdvancements().getOrStartProgress(sweetTooth).isDone(),
				"Actually eating Shperma must grant Sweet Tooth");
		server.getPlayerList().remove(player);
		test.succeed();
	}

	@GameTest
	public void unknownIsCommandOnlyAndHas200Health(GameTestHelper test) {
		var unknown = test.spawn(ModEntityTypes.KIRILL_V2, 2, 2, 2);
		test.assertTrue(unknown.getMaxHealth() == 200.0f && unknown.getHealth() == 200.0f,
				"The Unknown must spawn with 200 HP");
		test.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
				.getKey(ModEntityTypes.KIRILL_V2).equals(HbkMod.id("kirill_v2")),
				"The Unknown summon identifier must remain hbk:kirill_v2");
		test.assertTrue(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
				.getKey(dev.kirill.hbk.registry.ModSounds.UNKNOWN_MUSIC).equals(HbkMod.id("music.unknown")),
				"The Unknown encounter music must be registered");
		boolean hasSpawnEgg = net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet().stream()
				.anyMatch(id -> id.equals(HbkMod.id("kirill_v2_spawn_egg")));
		test.assertFalse(hasSpawnEgg, "The Unknown must not have a spawn egg");
		test.assertTrue(unknown.shouldBeSaved(),
				"An ordinary command-summoned Unknown must remain persistent");
		var legacyOrphan = test.spawn(ModEntityTypes.KIRILL_V2, 3, 2, 2);
		legacyOrphan.setNoAi(true);
		legacyOrphan.setInvulnerable(true);
		legacyOrphan.setNoGravity(true);
		test.assertFalse(legacyOrphan.shouldBeSaved(),
				"A frozen legacy encounter Unknown must never be written back to the world save");
		legacyOrphan.tick();
		test.assertTrue(legacyOrphan.isRemoved(),
				"A frozen legacy encounter Unknown must remove itself after loading");
		test.succeed();
	}

	@GameTest(maxTicks = 610)
	public void unknownEncounterRunsCutsceneKillsAndGrantsSecretAdvancement(GameTestHelper test) {
		for (int x = 0; x <= 6; x++) {
			for (int z = 0; z <= 8; z++) {
				test.setBlock(x, 1, z, Blocks.STONE);
			}
		}
		var player = test.makeMockServerPlayerInLevel();
		Vec3 anchor = test.absoluteVec(new Vec3(3.5, 2.0, 1.5));
		player.teleportTo(anchor.x, anchor.y, anchor.z);
		player.setYRot(0.0f);
		player.setXRot(0.0f);

		var result = dev.kirill.hbk.world.UnknownEncounter.start(player);
		test.assertTrue(result == dev.kirill.hbk.world.UnknownEncounter.StartResult.STARTED,
				"The encounter must start when four blocks ahead are clear");
		test.assertTrue(test.getEntities(ModEntityTypes.KIRILL_V2).size() == 1,
				"The encounter must spawn exactly one Unknown");
		var unknown = test.getEntities(ModEntityTypes.KIRILL_V2).getFirst();
		test.assertFalse(unknown.shouldBeSaved(),
				"A temporary encounter Unknown must never be persisted in the world save");
		Vec3 portalPosition = unknown.position();
		test.assertTrue(unknown.isInvisible(),
				"The Unknown must remain hidden while the glitch portal opens");
		Vec3 directionToPlayer = player.getEyePosition().subtract(unknown.getEyePosition()).normalize();
		test.assertTrue(unknown.getLookAngle().dot(directionToPlayer) > 0.999
					&& Math.abs(net.minecraft.util.Mth.wrapDegrees(unknown.yBodyRot - unknown.getYRot())) < 0.01,
				"The Unknown's head and body must face the player immediately after spawning");
		Vec3 loweredCameraTarget = new Vec3(unknown.getX(), unknown.getEyeY() - 0.3, unknown.getZ());
		test.assertTrue(player.getLookAngle().dot(loweredCameraTarget.subtract(player.getEyePosition()).normalize()) > 0.999,
				"The encounter must lock the player's camera slightly below the Unknown's eyes");

		test.runAfterDelay(1, () -> player.teleportTo(anchor.x + 2.0, anchor.y, anchor.z));
		Vec3[] heldUnknownPosition = new Vec3[1];
		test.runAfterDelay(3, () -> test.assertTrue(player.position().distanceToSqr(anchor) < 1.0E-6,
				"The encounter must keep the player fixed at the starting position"));
		test.runAfterDelay(40, () -> test.assertTrue(unknown.isInvisible(),
				"The portal must be visible before the Unknown appears"));
		test.runAfterDelay(90, () -> {
			test.assertFalse(unknown.isInvisible(),
					"The Unknown must become visible only after the portal has opened");
			test.assertTrue(unknown.position().distanceToSqr(player.position())
						< portalPosition.distanceToSqr(player.position()),
					"The Unknown must step out of the portal toward the player");
		});
		test.runAfterDelay(300, () -> test.assertFalse(unknown.isGrabbing(),
				"The Unknown must pause after saying that everything falls into place"));
		test.runAfterDelay(340, () -> {
			var unknowns = test.getEntities(ModEntityTypes.KIRILL_V2);
			test.assertTrue(unknowns.size() == 1 && unknowns.getFirst().isGrabbing(),
					"The Unknown must approach the player and enter the grabbing animation");
			test.assertTrue(unknowns.getFirst().position().subtract(player.position()).horizontalDistanceSqr()
						<= 1.16 * 1.16,
					"The Unknown must grab only after approaching within one block");
			test.assertTrue(player.getY() >= anchor.y + 0.6,
					"The Unknown must lift the player slightly while holding their neck");
			test.assertTrue(player.isNoGravity(),
					"Gravity must be disabled while the player is held to prevent falling jitter");
			heldUnknownPosition[0] = unknowns.getFirst().position();
		});
		test.runAfterDelay(360, () -> {
			var heldUnknown = test.getEntities(ModEntityTypes.KIRILL_V2).getFirst();
			test.assertTrue(heldUnknownPosition[0] != null
						&& heldUnknown.position().distanceToSqr(heldUnknownPosition[0]) < 1.0E-8
						&& heldUnknown.getDeltaMovement().lengthSqr() < 1.0E-8,
					"The Unknown must remain completely still throughout the neck grab");
		});
		test.runAfterDelay(460, () -> {
			test.assertFalse(player.isAlive(), "The encounter must kill the player after the final typed message");
			test.assertTrue(player.getLastDamageSource() != null
						&& player.getLastDamageSource().is(dev.kirill.hbk.world.UnknownEncounter.LOST_IN_TIME_DAMAGE),
					"The encounter must use the lost-in-time death cause");
			var advancement = test.getLevel().getServer().getAdvancements().get(HbkMod.id("unknown"));
			test.assertTrue(advancement != null
					&& player.getAdvancements().getOrStartProgress(advancement).isDone(),
					"The encounter death must grant the secret Unknown advancement");
			var departingUnknowns = test.getEntities(ModEntityTypes.KIRILL_V2);
			test.assertTrue(departingUnknowns.size() == 1 && !departingUnknowns.getFirst().isGrabbing(),
					"The Unknown must lower his hand and remain while the departure portal opens");
			test.assertFalse(player.isNoGravity(),
					"The encounter must restore the player's gravity after death");
			player.setHealth(player.getMaxHealth());
			test.assertTrue(player.isAlive(),
					"The departure regression fixture must restore a live player state");
		});
		test.runAfterDelay(510, () -> {
			var returningUnknowns = test.getEntities(ModEntityTypes.KIRILL_V2);
			test.assertTrue(returningUnknowns.size() == 1
						&& returningUnknowns.getFirst().position().distanceToSqr(portalPosition)
						< heldUnknownPosition[0].distanceToSqr(portalPosition),
					"The Unknown must walk back toward the same glitch portal after killing the player");
		});
		test.runAfterDelay(580, () -> {
			test.assertTrue(test.getEntities(ModEntityTypes.KIRILL_V2).isEmpty(),
					"The Unknown must disappear inside the portal before it closes");
			test.assertFalse(dev.kirill.hbk.world.UnknownEncounter.isActive(player),
					"The departure must finish even if the player's alive state changes after death");
			test.getLevel().getServer().getPlayerList().remove(player);
			test.succeed();
		});
	}

	@GameTest
	public void unknownEncounterRequiresClearSpaceAhead(GameTestHelper test) {
		for (int z = 0; z <= 6; z++) {
			test.setBlock(2, 1, z, Blocks.STONE);
		}
		test.setBlock(2, 2, 3, Blocks.STONE);
		var player = test.makeMockServerPlayerInLevel();
		Vec3 position = test.absoluteVec(new Vec3(2.5, 2.0, 1.5));
		player.teleportTo(position.x, position.y, position.z);
		player.setYRot(0.0f);
		player.setXRot(0.0f);

		var result = dev.kirill.hbk.world.UnknownEncounter.start(player);
		test.assertTrue(result == dev.kirill.hbk.world.UnknownEncounter.StartResult.BLOCKED,
				"A block in front of the player must prevent the encounter");
		test.assertTrue(test.getEntities(ModEntityTypes.KIRILL_V2).isEmpty(),
				"A blocked encounter must not spawn the Unknown");
		test.getLevel().getServer().getPlayerList().remove(player);
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
