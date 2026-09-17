package dev.kirill.hbk.mechanic;

import dev.kirill.hbk.entity.FlyingBlockEntity;
import dev.kirill.hbk.item.ShovelSwordItem;
import dev.kirill.hbk.item.SickleAndHammerItem;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class ModMechanics {
	private ModMechanics() {
	}

	public static void register() {
		registerHandImmortalityBreaking();
		registerBalalaikaPickaxe();
		registerCrumblingPlacement();
		registerShovelSwordCombat();
		registerSickleAndHammer();
	}

	private static void registerSickleAndHammer() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			ItemStack stack = player.getMainHandItem();
			if (!stack.is(ModItems.SICKLE_AND_HAMMER) || level.isClientSide()) {
				return true;
			}
			return !SickleAndHammerItem.mowArea((ServerLevel) level, player, pos, stack);
		});
	}

	private static void registerHandImmortalityBreaking() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!player.hasEffect(ModEffects.HAND_IMMORTALITY) || !player.getMainHandItem().isEmpty() || level.isClientSide()) {
				return true;
			}
			ServerLevel serverLevel = (ServerLevel) level;
			if (state.getBlock().asItem() != net.minecraft.world.item.Items.AIR) {
				Block.popResource(serverLevel, pos, new ItemStack(state.getBlock()));
			}
			serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
			serverLevel.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
			return false;
		});
	}

	private static void registerBalalaikaPickaxe() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			ItemStack stack = player.getMainHandItem();
			if (!stack.is(ModItems.BALALAIKA_PICKAXE) || level.isClientSide()) {
				return true;
			}
			ServerLevel serverLevel = (ServerLevel) level;
			if (player.getRandom().nextFloat() >= 0.15f || state.isAir()) {
				return true;
			}

			serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
			FlyingBlockEntity flyingBlock = new FlyingBlockEntity(serverLevel, state, player, pos);
			serverLevel.addFreshEntity(flyingBlock);
			stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
			playString(serverLevel, pos, player);
			serverLevel.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 5, 0.35, 0.35, 0.35, 0.1);
			return false;
		});

		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!level.isClientSide() && player.getMainHandItem().is(ModItems.BALALAIKA_PICKAXE)) {
				playString((ServerLevel) level, pos, player);
			}
		});
	}

	private static void playString(ServerLevel level, BlockPos pos, Player player) {
		level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BANJO.value(), SoundSource.PLAYERS, 0.9f, 0.75f + player.getRandom().nextFloat() * 0.7f);
	}

	private static void registerCrumblingPlacement() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!player.hasEffect(ModEffects.HAND_IMMORTALITY) || !(stack.getItem() instanceof BlockItem)) {
				return InteractionResult.PASS;
			}
			if (!level.isClientSide()) {
				if (!player.isCreative()) {
					stack.shrink(1);
				}
				ServerLevel serverLevel = (ServerLevel) level;
				BlockPos crumblePos = hit.getBlockPos().relative(hit.getDirection());
				serverLevel.sendParticles(ParticleTypes.POOF, crumblePos.getX() + 0.5, crumblePos.getY() + 0.5, crumblePos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.03);
				serverLevel.playSound(null, crumblePos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.8f, 0.65f);
			}
			return InteractionResult.SUCCESS;
		});
	}

	private static void registerShovelSwordCombat() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (player.hasEffect(ModEffects.SOULFULNESS)) {
				if (!level.isClientSide()) {
					player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 0.8f, 1.25f);
				}
				return InteractionResult.SUCCESS;
			}

			ItemStack weapon = player.getItemInHand(hand);
			if (!weapon.is(ModItems.SHOVEL_SWORD) || !ShovelSwordItem.isAttackMode(weapon) || !(entity instanceof LivingEntity target)) {
				return InteractionResult.PASS;
			}
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}

			int buckwheat = countBuckwheat(player);
			if (buckwheat > 0) {
				removeOneBuckwheat(player);
				target.hurtServer((ServerLevel) level, player.damageSources().playerAttack(player), buckwheat);
				weapon.hurtAndBreak(1, player, hand);
				player.playSound(SoundEvents.IRON_GOLEM_ATTACK, 0.9f, 0.85f);
			} else {
				target.addEffect(new MobEffectInstance(ModEffects.SOULFULNESS, 20 * 10, 0, false, true, true), player);
				player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 1.0f, 0.75f);
			}
			player.swing(hand, true);
			return InteractionResult.SUCCESS;
		});
	}

	private static int countBuckwheat(Player player) {
		int result = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(ModItems.BUCKWHEAT)) {
				result += stack.getCount();
			}
		}
		return result;
	}

	private static void removeOneBuckwheat(Player player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(ModItems.BUCKWHEAT)) {
				stack.shrink(1);
				return;
			}
		}
	}
}
