package dev.kirill.hbk;

import com.mojang.serialization.JsonOps;
import dev.kirill.hbk.universe.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class UniverseGameTests {
	@GameTest
	public void materialsHaveTexturesModelsToolsAndLoot(GameTestHelper test) throws Exception {
		var resources = UniverseGameTests.class.getClassLoader();
		Set<Integer> textureHashes = new HashSet<>();
		for (int f = 0; f < 10; f++) for (int role = 0; role < 5; role++) {
			String name = UniverseMaterials.FAMILIES.get(f) + "_" + UniverseMaterials.ROLES.get(role);
			test.assertTrue(resources.getResource("assets/hbk/blockstates/" + name + ".json") != null, "Blockstate present: " + name);
			test.assertTrue(resources.getResource("assets/hbk/items/" + name + ".json") != null, "Inventory model present: " + name);
			for (int variant = 0; variant < 4; variant++) for (String face : List.of("", "_top")) {
				try (var stream = resources.getResourceAsStream("assets/hbk/textures/block/universe/" + name + "_" + variant + face + ".png")) {
					test.assertTrue(stream != null, "Texture present");
					var image = javax.imageio.ImageIO.read(stream);
					test.assertTrue(image.getWidth() == 16 && image.getHeight() == 16, "Native 16px texture");
					Set<Integer> colors = new HashSet<>(); int hash = 1;
					for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { colors.add(image.getRGB(x, y)); hash = 31 * hash + image.getRGB(x, y); }
					test.assertTrue(colors.size() > 8, "Original pixel texture, not a solid placeholder");
					test.assertTrue(textureHashes.add(hash), "Distinct family/role/variant/face texture");
				}
			}
			final int family = f;
			var state = UniverseMaterials.state(f, role, 2);
			var drops = Block.getDrops(state, test.getLevel(), test.absolutePos(BlockPos.ZERO), null, null, new ItemStack(role == 4 ? Items.DIAMOND_PICKAXE : Items.IRON_PICKAXE));
			test.assertFalse(drops.isEmpty(), "Every material has usable loot: " + name);
			if (role == 4) test.assertTrue(drops.stream().anyMatch(stack -> stack.is(UniverseMaterials.resource(family, 0))), "Ore yields the family's mineral");
			if (role == 3) test.assertTrue(drops.stream().anyMatch(stack -> stack.is(UniverseMaterials.resource(family, 1))), "Formation yields shards");
		}
		for (int f = 0; f < 10; f++) for (String role : UniverseMaterials.RESOURCES) {
			String name = UniverseMaterials.FAMILIES.get(f) + "_" + role;
			test.assertTrue(resources.getResource("assets/hbk/textures/item/" + name + ".png") != null, "Resource item texture");
			test.assertTrue(resources.getResource("assets/hbk/items/" + name + ".json") != null, "Resource item model");
		}
		test.succeed();
	}

	@GameTest(maxTicks = 200)
	public void allAddressesAndProfilesAreDeterministic(GameTestHelper test) {
		Set<Long> seeds = new HashSet<>(140000);
		Set<String> dimensions = new HashSet<>(140000);
		for (int i = 0; i < 100000; i++) {
			UniverseCode code = UniverseCode.of(i);
			var p = UniverseProfile.create(code);
			test.assertTrue(code.value().length() == 5, "Address must retain all five digits");
			for (int position = 0, divisor = 10000; position < 5; position++, divisor /= 10)
				test.assertTrue(code.pattern(position) == i / divisor % 10, "Digit must select its own pattern");
			test.assertTrue(p.equals(UniverseProfile.create(new UniverseCode(code.value()))), "Profile must be repeatable");
			test.assertTrue(p.hidden().size() == 36 && p.hidden().stream().allMatch(v -> v >= 0 && v <= 9), "36 decimal hidden parameters");
			test.assertTrue(seeds.add(p.seed()), "Universe seeds must not collide");
			test.assertTrue(dimensions.add(UniverseManager.key(code).identifier().toString()), "Save directories must not collide");
		}
		for (String invalid : List.of("", "1234", "123456", "12a45", "１２３４５", "-0001", " 0000")) test.assertFalse(UniverseCode.valid(invalid), "Reject malformed address");
		test.assertFalse(UniverseCode.valid(null), "Null is invalid");
		var p = UniverseProfile.of(new UniverseCode("00001"));
		var encoded = UniverseProfile.CODEC.encodeStart(JsonOps.INSTANCE, p).getOrThrow();
		test.assertTrue(UniverseProfile.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(p), "Saved profile round trip");
		test.succeed();
	}
	@GameTest(maxTicks = 200)
	public void allFiftyPatternsAffectTheirDensityOrWaterField(GameTestHelper test) {
		var base = UniverseProfile.create(new UniverseCode("00000"));
		for (int position = 0; position < 5; position++) {
			Set<Long> signatures = new HashSet<>();
			for (int value = 0; value < 10; value++) {
				char[] code = "01200".toCharArray(); code[position] = (char) ('0' + value);
				// Hold the random streams fixed, to prove the pattern's rule actually changes.
				var p = new UniverseProfile(new UniverseCode(new String(code)), 1, base.seed(), base.hidden());
				var field = new UniverseDensity(p);
				long signature = 1;
				for (int x = -130; x <= 130; x += 13) for (int z = -117; z <= 117; z += 13) for (int y = -30; y <= 290; y += 16) {
					double d = field.sample(x, y, z);
					test.assertTrue(Double.isFinite(d), "Finite density");
					int state = d > 0 ? 2 : field.water(x, y, z, field.sample(x, field.waterSupportY(y), z)) ? 1 : 0;
					signature = signature * 31 + state;
				}
				test.assertTrue(signatures.add(signature), "Distinct occupied/wet volumes, position=" + position + ", value=" + value);
			}
		}
		test.succeed();
	}
	private static ProtoChunk generate(GameTestHelper test, UniverseChunkGenerator generator, int x, int z) {
		var level = test.getLevel();
		var chunk = new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, level, level.palettedContainerFactory(), null);
		generator.fillFromNoise(Blender.empty(), level.getChunkSource().randomState(), level.structureManager(), chunk).join();
		return chunk;
	}
	private static long fingerprint(ChunkAccess chunk) {
		long fingerprint = 1;
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = -64; y < 320; y++)
			fingerprint = fingerprint * 31 + Block.getId(chunk.getBlockState(new BlockPos(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z)));
		return fingerprint;
	}
	@GameTest(maxTicks = 200)
	public void chunksIgnoreGenerationOrderAndAgreeWithGlobalBoundaryQueries(GameTestHelper test) {
		var p = UniverseProfile.of(new UniverseCode("13857"));
		var biome = new FixedBiomeSource(test.getLevel().getBiome(test.absolutePos(BlockPos.ZERO)));
		var a = new UniverseChunkGenerator(biome, p);
		var b = new UniverseChunkGenerator(biome, p);
		var west = generate(test, a, -1, 0); var east = generate(test, a, 0, 0);
		var eastAgain = generate(test, b, 0, 0); var westAgain = generate(test, b, -1, 0);
		test.assertTrue(fingerprint(west) == fingerprint(westAgain) && fingerprint(east) == fingerprint(eastAgain), "Full chunk contents independent of generation order");
		for (int x : new int[]{-17, -16, -1, 0, 15, 16}) for (int z = 0; z < 16; z++) {
			var column = a.getBaseColumn(x, z, test.getLevel(), test.getLevel().getChunkSource().randomState());
			var chunk = x >= 0 && x < 16 ? east : x >= -16 && x < 0 ? west : generate(test, a, Math.floorDiv(x, 16), 0);
			for (int y = -64; y < 320; y++) test.assertTrue(column.getBlock(y).equals(chunk.getBlockState(new BlockPos(x, y, z))), "Height/column queries must match chunks, also at negative borders");
		}
		test.assertTrue(a.averageMillis() < 2000, "A chunk density pass should complete in less than two seconds");
		HbkMod.LOGGER.info("Universe benchmark: {} density passes, mean {} ms", a.generatedChunks(), a.averageMillis());
		test.succeed();
	}
	@GameTest(maxTicks = 200)
	public void isolatedWorldsPersistBlocksContainersAndProfilesAfterUnload(GameTestHelper test) {
		var server = test.getLevel().getServer();
		var code = new UniverseCode("34050");
		var level = UniverseManager.load(server, code);
		// Failed prior test runs may leave a persisted forceload ticket in the reused test save.
		for (long packed : level.getForceLoadedChunks().toLongArray()) {
			var chunk = ChunkPos.unpack(packed); level.setChunkForced(chunk.x(), chunk.z(), false);
		}
		var other = UniverseManager.load(server, new UniverseCode("34051"));
		test.assertTrue(!level.dimension().equals(other.dimension()), "Independent dimensions");
		BlockPos pos = new BlockPos(5, 280, 5);
		level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
		((ChestBlockEntity) level.getBlockEntity(pos)).setItem(0, new ItemStack(Items.DIAMOND, 7));
		var entity = net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		for (int y = 281; y <= 285; y++) level.setBlock(new BlockPos(5, y, 5), Blocks.AIR.defaultBlockState(), 2);
		entity.setPos(5.5, 282, 5.5); entity.setNoGravity(true); entity.setInvulnerable(true);
		entity.setCustomName(net.minecraft.network.chat.Component.literal("Universe persistence marker"));
		level.addFreshEntity(entity);
		var entityId = entity.getUUID();
		test.assertFalse(other.getBlockState(pos).is(Blocks.CHEST), "Address must not see another universe's container");
		test.assertTrue(UniverseManager.unload(server, level), "Unused dimension can unload");
		var reloaded = server.getLevel(UniverseManager.key(code));
		// A loaded terrain chunk alone is not entity-visible; native entity IO follows a ticking ticket.
		reloaded.setChunkForced(0, 0, true);
		test.assertTrue(reloaded != level, "getLevel lazily restores a saved dimension");
		test.assertTrue(reloaded.getBlockState(pos).is(Blocks.CHEST), "Player blocks survive unload/reload");
		var stack = ((ChestBlockEntity) reloaded.getBlockEntity(pos)).getItem(0);
		test.assertTrue(stack.is(Items.DIAMOND) && stack.getCount() == 7, "Container contents survive");
		var saved = reloaded.getDataStorage().computeIfAbsent(UniverseState.type(UniverseProfile.of(code)));
		test.assertTrue(saved.profile.equals(UniverseProfile.of(code)), "Persist all 36 selected parameters and generator version");
		test.succeedWhen(() -> {
			test.assertTrue(reloaded.getEntity(entityId) != null, "Entities retain their identity after a universe unload; loaded="
					+ reloaded.areEntitiesLoaded(ChunkPos.pack(0, 0)) + ", ticking=" + reloaded.isPositionEntityTicking(pos)
					+ ", chunks=" + reloaded.getChunkSource().getLoadedChunksCount());
			test.assertTrue(other.getEntity(entityId) == null, "Entities must not leak to another address");
			reloaded.setChunkForced(0, 0, false);
			UniverseManager.unload(server, other); UniverseManager.unload(server, reloaded);
		});
	}
	@GameTest(maxTicks = 200)
	public void travelKeepsInventoryAndReturnClearsUniversePhysics(GameTestHelper test) {
		var player = test.makeMockServerPlayerInLevel();
		BlockPos home = test.absolutePos(new BlockPos(2, 3, 2));
		test.getLevel().setBlockAndUpdate(home.below(), Blocks.STONE.defaultBlockState());
		for (int i = 0; i < 3; i++) test.getLevel().setBlockAndUpdate(home.above(i), Blocks.AIR.defaultBlockState());
		player.setPos(home.getX() + .5, home.getY(), home.getZ() + .5);
		player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 11));
		var furnacePos = home.south();
		test.getLevel().setBlockAndUpdate(furnacePos, dev.kirill.hbk.registry.ModBlocks.TIME_FURNACE.defaultBlockState());
		dev.kirill.hbk.registry.ModBlocks.TIME_FURNACE.setPlacedBy(test.getLevel(), furnacePos,
				test.getLevel().getBlockState(furnacePos), player, new ItemStack(dev.kirill.hbk.registry.ModItems.TIME_FURNACE));
		var menu = new dev.kirill.hbk.menu.TimeFurnaceMenu(5,
				(dev.kirill.hbk.block.entity.TimeFurnaceBlockEntity) test.getLevel().getBlockEntity(furnacePos));
		test.assertTrue(menu.saveCode(player, "00000"), "A nearby furnace can select address zero");
		double gravity = player.getGravity();
		test.assertTrue(UniverseManager.travel(player, new UniverseCode("00000")), "Machine destination zero is valid");
		test.assertTrue(player.level().dimension().equals(UniverseManager.key(new UniverseCode("00000"))), "Player reaches exact address");
		test.assertTrue(player.getInventory().getItem(0).getCount() == 11, "Inventory preserved on entry");
		test.assertTrue(player.level().getBlockState(player.blockPosition()).isAir()
				&& !player.level().getBlockState(player.blockPosition().below()).isAir(), "No suffocation, drowning or immediate void fall");
		var arrival = player.position();
		var station = UniverseArrival.findReturnFurnace(player.level(), player.blockPosition());
		test.assertTrue(station != null && player.level().getBlockEntity(station) instanceof dev.kirill.hbk.block.entity.TimeFurnaceBlockEntity, "Arrival always provides an actual return furnace");
		player.setPos(home.getX() + .5, home.getY(), home.getZ() + .5);
		test.assertFalse(menu.saveCode(player, "99999"), "Equal coordinates in another dimension cannot authorize a furnace edit");
		player.setPos(arrival);
		var returnMenu = new dev.kirill.hbk.menu.TimeFurnaceMenu(6, (dev.kirill.hbk.block.entity.TimeFurnaceBlockEntity) player.level().getBlockEntity(station));
		test.assertTrue(returnMenu.travel(player, "", true), "An unconfigured arrival machine returns the player without a command");
		test.assertTrue(player.level() == test.getLevel() && Math.abs(player.getGravity() - gravity) < 1e-9, "Gravity restored without accumulated modifiers");
		test.assertTrue(player.getInventory().getItem(0).getCount() == 11, "Inventory preserved on return");
		player.level().getServer().getPlayerList().remove(player);
		test.succeed();
	}
}
