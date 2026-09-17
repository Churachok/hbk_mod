package dev.kirill.hbk.command;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.world.GulagPlacer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class ModCommands {
	private static final TagKey<Structure> GULAGS = TagKey.create(Registries.STRUCTURE, HbkMod.id("gulags"));

	private ModCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
				Commands.literal("gulag")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ctx -> locate(ctx.getSource()))
						.then(Commands.literal("spawn").executes(ctx -> spawnHere(ctx.getSource())))
		));
	}

	private static int locate(CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		if (!level.dimension().equals(Level.OVERWORLD)) {
			source.sendFailure(Component.literal("ГУЛАГ генерируется только в обычном мире."));
			return 0;
		}

		BlockPos from = BlockPos.containing(source.getPosition());
		BlockPos gulag = level.findNearestMapStructure(GULAGS, from, 100, false);
		if (gulag == null) {
			source.sendFailure(Component.literal("ГУЛАГ не найден в радиусе 1600 блоков."));
			return 0;
		}

		int distance = (int) Math.sqrt(gulag.distSqr(from));
		source.sendSuccess(
				() -> Component.literal("Ближайший ГУЛАГ: " + gulag.getX() + " " + gulag.getY() + " " + gulag.getZ()
						+ " (" + distance + " блоков). Телепорт: /tp @s " + gulag.getX() + " ~ " + gulag.getZ()
						+ ". Поставить здесь: /gulag spawn"),
				false
		);
		return 1;
	}

	private static int spawnHere(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = source.getLevel();
		BlockPos placed = GulagPlacer.placeGulag(level, player.blockPosition());
		if (placed == null) {
			source.sendFailure(Component.literal("Не удалось поставить ГУЛАГ здесь."));
			return 0;
		}
		dev.kirill.hbk.world.ModWorldData.get(level).addGulagPosition(placed);
		source.sendSuccess(
				() -> Component.literal("ГУЛАГ поставлен: " + placed.getX() + " " + placed.getY() + " " + placed.getZ()),
				true
		);
		return 1;
	}
}
