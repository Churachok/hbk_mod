package dev.kirill.hbk;

import dev.kirill.hbk.universe.*;
import dev.kirill.hbk.world.AnomalyZones;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class UniverseExplorationGameTests {
	@GameTest(maxTicks = 400)
	public void allFamiliesHaveFiveDeterministicStructuresWithSecretsAndWorkingRooms(GameTestHelper test) {
		for (int f = 0; f < 10; f++) {
			var p = UniverseProfile.of(new UniverseCode(f + "1234")); var planner = new UniverseStructures(p);
			for (var kind : UniverseStructures.Kind.values()) {
				var site = planner.nearest(BlockPos.ZERO, kind);
				test.assertTrue(site != null && site.rooms().size() >= 3, "Every address offers a modular " + kind);
				test.assertTrue(site.treasures().stream().anyMatch(UniverseStructures.Treasure::secret), "Every structure contains a hidden vault");
				for (var chest : site.treasures()) test.assertTrue(planner.material(List.of(site), chest.pos().getX(), chest.pos().getY(), chest.pos().getZ()).is(Blocks.CHEST), "Loot positions agree with voxel plan");
				if (kind == UniverseStructures.Kind.WATCHTOWER) {
					var doorway = site.world(9,89,0);
					test.assertTrue(planner.material(List.of(site),doorway.getX(),doorway.getY(),doorway.getZ()).isAir(),"Archive opens onto the secret service bridge");
				}
				int cx = Math.floorDiv(site.origin().getX(), UniverseStructures.SPACING), cz = Math.floorDiv(site.origin().getZ(), UniverseStructures.SPACING);
				test.assertTrue(new UniverseStructures(p).site(cx, cz).orElseThrow().equals(site), "Plan independent of cache and query order");
			}
		}
		test.succeed();
	}
	@GameTest(maxTicks = 400)
	public void realStructureChestsPromoteLoadLootAndRetainPlayerChanges(GameTestHelper test) {
		var level = UniverseManager.load(test.getLevel().getServer(), new UniverseCode("32119"));
		var planner = ((UniverseChunkGenerator) level.getChunkSource().getGenerator()).structures();
		for (var kind : UniverseStructures.Kind.values()) {
			var site = planner.nearest(BlockPos.ZERO, kind); var target = site.treasures().stream().filter(UniverseStructures.Treasure::secret).findFirst().orElseThrow().pos();
			level.getChunk(target.getX() >> 4, target.getZ() >> 4);
			test.assertTrue(level.getBlockEntity(target) instanceof ChestBlockEntity, "Promoted secret chest: " + kind);
			var chest = (ChestBlockEntity) level.getBlockEntity(target);
			chest.clearContent(); chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, HbkMod.id("chests/universe_secret_cavernous")));
			chest.unpackLootTable(null);
			boolean mineral = false; for (int slot = 0; slot < chest.getContainerSize(); slot++) mineral |= chest.getItem(slot).is(UniverseMaterials.resource(3, 0));
			test.assertTrue(mineral, "Vault includes the local mineral");
			boolean journal = false;
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				var book = chest.getItem(slot).get(DataComponents.WRITTEN_BOOK_CONTENT);
				journal |= book != null && book.pages().size() == 2;
			}
			test.assertTrue(journal, "Vault includes a real readable two-page journal and survival clues");
			chest.setItem(0, new ItemStack(net.minecraft.world.item.Items.EMERALD, 23));
			test.assertTrue(chest.getItem(0).getCount() == 23, "Player contents are ordinary persistent chest data");
		}
		test.succeed();
	}
	@GameTest
	public void giganticFormationsHaveRealGeometryAndKilometreFissures(GameTestHelper test) {
		var planner = new UniverseLandmarks(UniverseProfile.of(new UniverseCode("76543")));
		for (var kind : UniverseLandmarks.Kind.values()) {
			var site = planner.nearest(BlockPos.ZERO, kind); test.assertTrue(site != null, "Find " + kind);
			int occupied = 0;
			for (int dx = -150; dx <= 150; dx += 5) for (int dy = 0; dy < 160; dy += 4) for (int dz = -95; dz <= 95; dz += 10) {
				var state = planner.material(List.of(site), site.center().getX() + dx, site.center().getY() + dy, site.center().getZ() + dz);
				if (state != null) occupied++;
			}
			test.assertTrue(occupied > 20, "Formation has a substantial traversable world-space shape: " + kind);
			if (kind == UniverseLandmarks.Kind.FISSURE) {
				int count = 0;
				for (int u = -580; u <= 580; u += 20) {
					for (int v = -80; v <= 80; v++) {
						int x = site.center().getX() + (site.rotated() ? v : u), z = site.center().getZ() + (site.rotated() ? u : v);
						var state = planner.material(List.of(site), x, 0, z); if (state != null && state.isAir()) { count++; break; }
					}
				}
				test.assertTrue(count == 59, "The continuous crack spans more than 1.1 km");
			}
		}
		var level = UniverseManager.load(test.getLevel().getServer(),new UniverseCode("00109"));
		var actual = ((UniverseChunkGenerator)level.getChunkSource().getGenerator()).landmarks().nearest(BlockPos.ZERO,UniverseLandmarks.Kind.SKELETON);
		test.assertTrue(level.getBlockState(actual.center().above(16)).is(Blocks.BONE_BLOCK),"The promoted world contains the skeleton's planned spine");
		test.succeed();
	}
	@GameTest
	public void stormsWarnLastExactlyOneMinuteAndReturnGravityToNormal(GameTestHelper test) {
		for (int f = 0; f < 10; f++) {
			var p = UniverseProfile.of(new UniverseCode(f + "1234")); long start = 0;
			while (!UniverseDisasters.gravity(p, start).active()) start++;
			test.assertTrue(UniverseDisasters.gravity(p, start - 200).warning() && !UniverseDisasters.gravity(p, start - 201).warning(), "Ten second warning");
			test.assertTrue(UniverseDisasters.gravity(p, start).remaining() == 1200 && UniverseDisasters.gravity(p, start + 1199).active() && !UniverseDisasters.gravity(p, start + 1200).active(), "Exactly 60 seconds");
			boolean inverse = false, heavy = false;
			for (int n = 0; n < 1200; n++) { double multiplier = UniverseDisasters.gravityMultiplier(p, start + n); inverse |= multiplier < 0; heavy |= multiplier > 2; }
			test.assertTrue(inverse && heavy && UniverseDisasters.gravityMultiplier(p, start + 1200) == 1, "Alternates levitation/heavy phases and restores gravity");
			test.assertTrue(UniverseDisasters.gravity(p, start + 18000).equals(UniverseDisasters.gravity(p, start)), "Schedule survives restarts without a transient timer");
		}
		var level = test.getLevel(); var pos = test.absolutePos(new BlockPos(2,4,2));
		for (int dy = 0; dy < 20; dy++) level.setBlock(pos.above(dy),Blocks.AIR.defaultBlockState(),2);
		var item = new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,new ItemStack(net.minecraft.world.item.Items.COBBLESTONE));
		item.setDeltaMovement(Vec3.ZERO);
		double startY = item.getY();
		for (int tick = 0; tick < 40; tick++) { if (tick % 5 == 0) UniverseDisasters.liftItem(item,tick); item.tick(); }
		test.assertTrue(item.getY() > startY + 1 && !item.isNoGravity(),"Dropped items actually float against positive gravity without a persistent flag: y="+item.getY()+", start="+startY+", motion="+item.getDeltaMovement()+", gravity="+item.getGravity()+", sheltered="+UniverseDisasters.sheltered(level,item.position()));
		level.setBlock(BlockPos.containing(item.position()).above(3),Blocks.STONE.defaultBlockState(),2);
		test.assertFalse(UniverseDisasters.liftItem(item,40),"A roof protects dropped items too");
		test.succeed();
	}
	@GameTest
	public void realShelterAndEquipmentProtectFromFamilyCataclysms(GameTestHelper test) {
		var level = test.getLevel(); var pos = test.absolutePos(new BlockPos(1, 3, 1));
		for (int y = 0; y < 16; y++) if (!level.isOutsideBuildHeight(pos.above(y))) level.setBlockAndUpdate(pos.above(y), Blocks.AIR.defaultBlockState());
		var player = test.makeMockPlayer(GameType.SURVIVAL); player.setPos(Vec3.atBottomCenterOf(pos));
		player.setHealth(20); UniverseDisasters.affect(level, player, UniverseDisasters.Kind.ACID, 40);
		test.assertTrue(player.getHealth() < 20, "Acid actually damages an exposed survival player");
		level.setBlockAndUpdate(pos.above(4), Blocks.STONE.defaultBlockState());
		test.assertTrue(UniverseDisasters.sheltered(level, player.position()), "Roof recognized");
		player.setHealth(20); UniverseDisasters.affect(level, player, UniverseDisasters.Kind.ACID, 80);
		test.assertTrue(player.getHealth() == 20, "Roof prevents acid damage");
		level.setBlockAndUpdate(pos.above(4), Blocks.AIR.defaultBlockState());
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(UniverseAlloys.item("hermetic_helmet")));
		UniverseDisasters.affect(level, player, UniverseDisasters.Kind.ACID, 120);
		test.assertTrue(player.getHealth() == 20 && UniverseDisasters.protectedFromAsh(player), "Hermetic gear protects from acid and ash");
		var types = new HashSet<UniverseDisasters.Kind>(); for (int f = 0; f < 10; f++) types.add(UniverseDisasters.kind(UniverseProfile.of(new UniverseCode(f + "0000"))));
		test.assertTrue(types.size() == 4, "All four cataclysm families are assigned"); test.succeed();
	}
	@GameTest(maxTicks = 400)
	public void parallelAddressesHaveAllSevenAnomaliesAndTheirSharedRules(GameTestHelper test) {
		var server = test.getLevel().getServer(); var level = UniverseManager.load(server, new UniverseCode("86424"));
		long seed = AnomalyZones.seed(level);
		for (int kind = 1; kind <= 7; kind++) {
			var zone = AnomalyZones.nearest(seed, 0, 0, kind); test.assertTrue(zone != null, "Find parallel anomaly " + kind);
			test.assertTrue(AnomalyZones.at(level, zone.centerX(), zone.centerZ()).equals(zone), "Common client/server zone lookup");
			BlockPos pos = new BlockPos(zone.centerX(), 110, zone.centerZ());
			level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
			test.assertTrue(AnomalyZones.contains(level, pos) && zone.gravity(zone.centerX(), zone.centerZ()) < .05, "Anomaly physics shared");
			level.setBlockAndUpdate(pos, Blocks.REDSTONE_BLOCK.defaultBlockState());
			test.assertTrue(level.getBestNeighborSignal(pos.east()) == 0, "Signals disabled inside the parallel anomaly");
		}
		test.succeed();
	}
	@GameTest(maxTicks = 400)
	public void repeatedAnomalyTilesIgnoreExplorationOrderAndKeepAddressesSeparate(GameTestHelper test) {
		var level = UniverseManager.load(test.getLevel().getServer(), new UniverseCode("86425"));
		var zone = AnomalyZones.nearest(AnomalyZones.seed(level), 0, 0, 6);
		var second = level.getChunk((zone.minX() >> 4) + 3, (zone.minZ() >> 4) + 2);
		var first = level.getChunk(zone.minX() >> 4, zone.minZ() >> 4);
		for (int x = 3; x < 13; x++) for (int z = 3; z < 13; z++) for (int y = -60; y < 290; y++) {
			var a = first.getBlockState(new BlockPos(zone.minX() + x, y, zone.minZ() + z));
			var b = second.getBlockState(new BlockPos(second.getPos().getMinBlockX() + x, y, second.getPos().getMinBlockZ() + z));
			if (!a.equals(b)) {
				var column = new StringBuilder();
				for (int dy = -1; dy <= 8; dy++) column.append("\n").append(y + dy).append(": ").append(first.getBlockState(new BlockPos(zone.minX()+x,y+dy,zone.minZ()+z))).append(" / ").append(second.getBlockState(new BlockPos(second.getPos().getMinBlockX()+x,y+dy,second.getPos().getMinBlockZ()+z)));
				throw new AssertionError("Repeated mismatch at " + x + "," + y + "," + z + column);
			}
		}
		test.succeed();
	}
	@GameTest
	public void everyAlloyRewardUsesItsOwnArtAndFunctionalItemModel(GameTestHelper test) throws Exception {
		var resources = getClass().getClassLoader(); var silhouettes = new HashSet<Integer>(); int count = 0;
		for (var alloy : UniverseAlloys.CATALOG) for (var reward : alloy.rewards()) {
			count++;
			String texture = "assets/hbk/textures/" + (reward.role().equals("device") ? "block/alloys/"+reward.id()+"_front" : "item/alloys/"+reward.id()) + ".png";
			try (var stream = resources.getResourceAsStream(texture)) {
				test.assertTrue(stream != null, "Every functional reward has original pixel art: " + reward.id());
				var image = javax.imageio.ImageIO.read(stream); int hash = 1;
				test.assertTrue(image.getWidth()==16 && image.getHeight()==16,"Native pixel density");
				for (int x=0;x<16;x++) for (int y=0;y<16;y++) hash = hash * 31 + image.getRGB(x,y);
				test.assertTrue(silhouettes.add(hash), "Different reward designs, not one reused texture");
			}
			var model = UniverseAlloys.item(reward.id()).components().get(DataComponents.ITEM_MODEL);
			test.assertTrue(HbkMod.id(reward.id()).equals(model),"Runtime must actually use the custom model, including shields/compasses/crossbow");
		}
		test.assertTrue(count==46,"All 46 rewards covered"); test.succeed();
	}
}
