package dev.kirill.hbk.world;

import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.util.NkvdSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class StrangeChestManager {
	private static final Map<ServerLevel, Map<BlockPos, Long>> VANISHING_CHESTS = new WeakHashMap<>();

	private StrangeChestManager() {
	}

	public static void answer(ServerPlayer player, BlockPos chestPos, boolean isThief) {
		ServerLevel level = player.level();
		if (!level.getBlockState(chestPos).is(ModBlocks.STRANGE_CHEST) || chestPos.distToCenterSqr(player.position()) > 64.0) {
			return;
		}

		ModWorldData data = ModWorldData.get(level);
		if (data.isStrangeChestAnswered(chestPos.asLong())) {
			return;
		}
		data.markStrangeChestAnswered(chestPos.asLong());

		if (!isThief) {
			ItemStack diamonds = new ItemStack(Items.DIAMOND, 3);
			if (!player.getInventory().add(diamonds)) {
				player.drop(diamonds, false);
			}
			player.sendSystemMessage(Component.translatable("message.hbk.strange_chest.reward"));
			level.playSound(null, chestPos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.15f);
			return;
		}

		level.playSound(null, chestPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.4f, 0.55f);
		BlockPos vanishingPos = findVanishingChestPos(level, chestPos);
		if (vanishingPos != null) {
			level.setBlockAndUpdate(vanishingPos, Blocks.CHEST.defaultBlockState());
			VANISHING_CHESTS.computeIfAbsent(level, ignored -> new HashMap<>())
					.put(vanishingPos.immutable(), level.getGameTime() + 10L);
		}

		player.getInventory().clearContent();
		player.inventoryMenu.broadcastChanges();
		NkvdSpawning.spawnSquad(level, player.blockPosition(), 3);
		player.sendSystemMessage(Component.translatable("message.hbk.strange_chest.punishment"));
	}

	private static BlockPos findVanishingChestPos(ServerLevel level, BlockPos origin) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos candidate = origin.relative(direction);
			if (level.getBlockState(candidate).canBeReplaced()) {
				return candidate;
			}
		}
		return null;
	}

	public static void tick(ServerLevel level) {
		Map<BlockPos, Long> pending = VANISHING_CHESTS.get(level);
		if (pending == null || pending.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<BlockPos, Long>> iterator = pending.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<BlockPos, Long> entry = iterator.next();
			if (level.getGameTime() < entry.getValue()) {
				continue;
			}
			BlockPos pos = entry.getKey();
			if (level.getBlockState(pos).is(Blocks.CHEST)) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 18, 0.35, 0.35, 0.35, 0.04);
				level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.9f, 1.25f);
			}
			iterator.remove();
		}
	}
}
