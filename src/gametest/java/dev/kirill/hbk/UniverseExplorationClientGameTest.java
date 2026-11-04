package dev.kirill.hbk;

import dev.kirill.hbk.universe.*;
import dev.kirill.hbk.world.AnomalyZones;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;

/** Visual acceptance: real generated rooms, landmarks, shared anomaly rendering and live storms. */
public final class UniverseExplorationClientGameTest implements FabricClientGameTest {
	private final boolean landmarksOnly;
	public UniverseExplorationClientGameTest() { this(false); }
	public UniverseExplorationClientGameTest(boolean landmarksOnly) { this.landmarksOnly = landmarksOnly; }
	@Override public void runTest(ClientGameTestContext context) {
		int distance = context.computeOnClient(c -> c.options.renderDistance().get());
		int simulation = context.computeOnClient(c -> c.options.simulationDistance().get());
		try {
			context.runOnClient(c -> { c.options.renderDistance().set(5); c.options.simulationDistance().set(3); c.options.broadcastOptions(); });
			try (var world = context.worldBuilder().adjustSettings(ui -> {
				ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE); ui.setAllowCommands(true); ui.setGenerateStructures(false);
			}).create()) {
				var server = world.getServer();
				// A bright, low-fog address makes the silhouette of each large feature inspectable.
				server.runCommand("execute as @a run universe tp 00109");
				context.waitFor(c -> c.level != null && c.level.dimension().equals(UniverseManager.key(new UniverseCode("00109"))), 300);
				world.getConnection().waitForChunksDownload(1200);
				context.waitFor(c -> c.gui.screen() == null, 1200);
				if (!landmarksOnly) for (var kind : UniverseStructures.Kind.values()) {
					server.runOnServer(instance -> {
						var player = instance.getPlayerList().getPlayers().getFirst(); var level = player.level();
						var generator = (UniverseChunkGenerator) level.getChunkSource().getGenerator();
						var site = generator.structures().nearest(BlockPos.ZERO, kind);
						var camera = site.origin().offset(30, kind == UniverseStructures.Kind.WATCHTOWER ? 65 : 32, 40);
						player.teleportTo(level,camera.getX()+.5,camera.getY(),camera.getZ()+.5,Set.of(),145,20,true);
						player.getAbilities().flying = true; player.onUpdateAbilities();
						player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,24000));
						var chest = site.treasures().getFirst().pos(); level.getChunk(chest.getX()>>4,chest.getZ()>>4);
						if (!(level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)) throw new AssertionError("Structure loot missing: "+kind);
					});
					HbkMod.LOGGER.info("Exploration visual check: {}", kind);
					world.getConnection().waitForClientboundPackets();
					context.waitFor(c -> c.gui.screen() == null && c.level.hasChunk(c.player.getBlockX() >> 4,c.player.getBlockZ() >> 4),1200);
					world.getConnection().waitForChunksRender(false, 1200); context.waitTicks(20); look(context,145,20); context.takeScreenshot("hbk-exploration-structure-"+kind.name().toLowerCase());
					if (kind == UniverseStructures.Kind.UNDERGROUND_LAB) {
						server.runOnServer(instance -> {
							var player = instance.getPlayerList().getPlayers().getFirst(); var level = player.level();
							var site = ((UniverseChunkGenerator)level.getChunkSource().getGenerator()).structures().nearest(BlockPos.ZERO,kind);
							var camera = site.world(4,2,4); player.teleportTo(level,camera.getX()+.5,camera.getY(),camera.getZ()+.5,Set.of(),135,5,true);
						});
						world.getConnection().waitForClientboundPackets();
						world.getConnection().waitForChunksRender(false, 1200); context.waitTicks(20); look(context,135,5); context.takeScreenshot("hbk-exploration-laboratory-interior");
					}
				}
				for (var kind : UniverseLandmarks.Kind.values()) {
					var target = server.computeOnServer(instance -> {
						var player = instance.getPlayerList().getPlayers().getFirst(); var level = player.level();
						var planner = ((UniverseChunkGenerator)level.getChunkSource().getGenerator()).landmarks();
						var site = planner.nearest(BlockPos.ZERO,kind);
						if (AnomalyZones.at(level,site.center().getX(),site.center().getZ()) != null) {
							search: for (int gx=-4;gx<=4;gx++) for (int gz=-4;gz<=4;gz++) {
								var candidate=planner.site(gx,gz);
								if (candidate.kind()==kind && AnomalyZones.at(level,candidate.center().getX(),candidate.center().getZ())==null) { site=candidate; break search; }
							}
						}
						int u=0,v=0,h=16;
						switch (kind) {
							case FISSURE -> v=96;
							case MONOLITHS -> { v=32; h=158-site.center().getY(); }
							case CRYSTAL_FOREST -> { u=12+(int)(site.seed()&7); v=10+(int)(site.seed()>>>3&7); h=20; }
							case PETRIFIED_WAVE -> { u=-75; h=25; }
							case WORLD_EYE -> { u=50; v=-11; h=72; }
							default -> {}
						}
						var focus=site.center().offset(site.rotated()?v:u,h,site.rotated()?u:v);
						level.getChunk(focus.getX() >> 4,focus.getZ() >> 4);
						if (kind==UniverseLandmarks.Kind.FISSURE) focus=new BlockPos(focus.getX(),level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,focus.getX(),focus.getZ())-1,focus.getZ());
						if (level.getBlockState(focus).isAir()) throw new AssertionError("Expected real landmark geometry at "+focus+": "+kind);
						var pos=focus.offset(25,35,45);
						player.teleportTo(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,Set.of(),151,35,true);
						player.getAbilities().flying = true; player.onUpdateAbilities(); player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
						player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,24000));
						return focus;
					});
					HbkMod.LOGGER.info("Exploration visual check: {}", kind);
					world.getConnection().waitForClientboundPackets();
					context.waitFor(c -> c.gui.screen() == null && c.level.hasChunk(c.player.getBlockX() >> 4,c.player.getBlockZ() >> 4),1200);
					context.waitFor(c -> c.level.hasChunk(target.getX() >> 4,target.getZ() >> 4),1200);
					look(context,151,35);
					context.waitFor(c -> !c.level.getBlockState(target).isAir(),1200);
					context.waitFor(c -> c.levelRenderer.isSectionCompiledAndVisible(target),1200);
					world.getConnection().waitForChunksRender(false, 1200); context.waitTicks(20); context.takeScreenshot("hbk-exploration-landmark-"+kind.name().toLowerCase());
				}
				if (!landmarksOnly) for (int kind = 1; kind <= 7; kind++) {
					final int number = kind;
					server.runOnServer(instance -> {
						var player = instance.getPlayerList().getPlayers().getFirst(); var level = player.level();
						var zone = AnomalyZones.nearest(AnomalyZones.seed(level),0,0,number);
						player.teleportTo(level,zone.centerX(),180,zone.centerZ(),Set.of(),20,25,true);
					});
					context.waitFor(c -> c.player != null && AnomalyZones.contains(c.player),200);
					world.getConnection().waitForClientboundPackets();
					context.waitFor(c -> c.gui.screen() == null && c.level.hasChunk(c.player.getBlockX() >> 4,c.player.getBlockZ() >> 4),1200);
					world.getConnection().waitForChunksRender(false, 1200); context.waitTicks(10); look(context,20,25); context.takeScreenshot("hbk-exploration-anomaly-"+number);
				}
				if (!landmarksOnly) {
				for (String code : java.util.List.of("00000","10000","20000","30000")) {
					server.runCommand("execute as @a run universe tp "+code);
					context.waitFor(c -> c.level != null && c.level.dimension().equals(UniverseManager.key(new UniverseCode(code))),300);
					server.runOnServer(instance -> {
						for (var level : instance.getAllLevels()) if (UniverseProfile.of(level) != null && level.players().isEmpty()) UniverseManager.unload(instance,level);
					});
					world.getConnection().waitForChunksDownload(1200); context.waitFor(c -> c.gui.screen() == null,1200);
					server.runOnServer(instance -> {
						var player = instance.getPlayerList().getPlayers().getFirst(); var level = player.level(); var p=UniverseProfile.of(level);
						player.teleportTo(level,0.5,180,0.5,Set.of(),0,15,true); player.getAbilities().flying=true; player.onUpdateAbilities();
						for (int x=-5;x<=5;x++) for (int z=-5;z<=5;z++) {
							level.setBlockAndUpdate(new BlockPos(x,179,z),Blocks.SMOOTH_STONE.defaultBlockState());
							for (int y=180;y<=205;y++) level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
						}
						long time=0; while (!UniverseDisasters.cataclysm(p,time).active()) time++;
						instance.getWorldData().overworldData().setGameTime(time+100); instance.forceGameTimeSynchronization();
					});
					world.getConnection().waitForChunksRender(false, 1200); context.waitTicks(25); look(context,0,15); context.takeScreenshot("hbk-exploration-cataclysm-"+code);
				}
				server.runOnServer(instance -> {
					var player=instance.getPlayerList().getPlayers().getFirst(); var level=player.level(); var p=UniverseProfile.of(level);
					long time=0; while (!UniverseDisasters.gravity(p,time).active()) time++;
					instance.getWorldData().overworldData().setGameTime(time+300); instance.forceGameTimeSynchronization();
					for (int i=0;i<8;i++) { var item=new net.minecraft.world.entity.item.ItemEntity(level,i-4,181,3,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE)); level.addFreshEntity(item); }
				});
				context.waitTicks(30); look(context,0,15); context.takeScreenshot("hbk-exploration-gravity-storm");
				}
			}
		} finally {
			context.runOnClient(c -> { c.options.renderDistance().set(distance); c.options.simulationDistance().set(simulation); c.options.broadcastOptions(); });
		}
	}
	private static void look(ClientGameTestContext context, float yaw, float pitch) {
		context.runOnClient(client -> {
			client.player.setYRot(yaw); client.player.yRotO = yaw;
			client.player.setXRot(pitch); client.player.xRotO = pitch;
			client.gui.hud.getChat().clearMessages(true);
		});
		context.waitTicks(3);
	}
}
