package dev.kirill.hbk.mixin.gametest;

import dev.kirill.hbk.world.AnomalyZones;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Test runtime only: ordinary NPC/boat fixtures must not randomly land in a freeze zone.
 * Anomaly tests still visit real zones explicitly, using their existing helpers.
 */
@Mixin(GameTestServer.class)
public abstract class GameTestPositionMixin {
	@ModifyVariable(method = "startTests", at = @At("STORE"), ordinal = 0)
	private BlockPos hbk$ordinaryTestGround(BlockPos original) {
		long seed = AnomalyZones.layoutSeed(((MinecraftServer) (Object) this).overworld().getSeed());
		// Include the structure grid and the squirrel fixtures offset by up to 500/600 blocks.
		for (int cell = 0; cell < 64; cell++) for (int oz = 0; oz < 1536; oz += 128) for (int ox = 0; ox < 1536; ox += 128) {
			int x = (cell % 8) * 1536 + ox, z = (cell / 8) * 1536 + oz;
			boolean clear = true;
			for (int cx = Math.floorDiv(x - 32, 1536); cx <= Math.floorDiv(x + 768, 1536) && clear; cx++)
				for (int cz = Math.floorDiv(z - 32, 1536); cz <= Math.floorDiv(z + 1152, 1536) && clear; cz++)
					for (var zone : AnomalyZones.zonesInCell(seed, cx, cz))
						if (zone.intersects(x - 32, z - 32, x + 768, z + 1152)) { clear = false; break; }
			if (clear) return new BlockPos(x, original.getY(), z);
		}
		throw new IllegalStateException("Could not find ordinary terrain for GameTest fixtures");
	}
}
