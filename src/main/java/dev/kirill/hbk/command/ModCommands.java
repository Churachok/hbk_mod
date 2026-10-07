package dev.kirill.hbk.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModGameRules;
import dev.kirill.hbk.world.GulagPlacer;
import dev.kirill.hbk.world.UnknownEncounter;
import dev.kirill.hbk.world.SquirrelEncounter;
import net.minecraft.commands.arguments.EntityArgument;
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
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
			dispatcher.register(Commands.literal("gulag")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ctx -> locate(ctx.getSource()))
						.then(Commands.literal("spawn").executes(ctx -> spawnHere(ctx.getSource())))
			);
			dispatcher.register(Commands.literal("unknown")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(ctx -> startUnknown(ctx.getSource(), ctx.getSource().getPlayerOrException()))
					.then(Commands.argument("player", EntityArgument.player())
							.executes(ctx -> startUnknown(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))
			);
			dispatcher.register(Commands.literal("konata")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.literal("shperm")
							.then(Commands.literal("limit")
									.executes(ctx -> getKonataShpermLimit(ctx.getSource()))
									.then(Commands.argument("amount", IntegerArgumentType.integer(0))
											.executes(ctx -> setKonataShpermLimit(ctx.getSource(),
													IntegerArgumentType.getInteger(ctx, "amount"))))))
			);
			dispatcher.register(Commands.literal("squirrel")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(ctx -> startSquirrel(ctx.getSource(), ctx.getSource().getPlayerOrException()))
					.then(Commands.argument("player", EntityArgument.player())
							.executes(ctx -> startSquirrel(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))
			);
		});
	}

	private static int startSquirrel(CommandSourceStack source, ServerPlayer player) {
		return switch (SquirrelEncounter.start(player)) {
			case STARTED -> {
				source.sendSuccess(() -> Component.translatable("message.hbk.squirrel_started", player.getScoreboardName()), false);
				yield 1;
			}
			case BLOCKED -> {
				source.sendFailure(Component.translatable("message.hbk.squirrel_blocked"));
				yield 0;
			}
			case ALREADY_ACTIVE -> {
				source.sendFailure(Component.translatable("message.hbk.squirrel_already_active"));
				yield 0;
			}
			case UNAVAILABLE -> {
				source.sendFailure(Component.translatable("message.hbk.squirrel_unavailable"));
				yield 0;
			}
		};
	}

	private static int getKonataShpermLimit(CommandSourceStack source) {
		int limit = source.getLevel().getGameRules().get(ModGameRules.KONATA_SHPERM_LIMIT);
		source.sendSuccess(() -> Component.literal("Дневной лимит подарков Конаты: " + limit), false);
		return limit;
	}

	private static int setKonataShpermLimit(CommandSourceStack source, int limit) {
		source.getLevel().getGameRules().set(ModGameRules.KONATA_SHPERM_LIMIT, limit, source.getServer());
		source.sendSuccess(() -> Component.literal("Дневной лимит подарков Конаты изменён на " + limit), true);
		return limit;
	}

	private static int startUnknown(CommandSourceStack source, ServerPlayer player) {
		UnknownEncounter.StartResult result = UnknownEncounter.start(player);
		return switch (result) {
			case STARTED -> {
				source.sendSuccess(() -> Component.literal("Неизвестный нашёл " + player.getScoreboardName() + "."), true);
				yield 1;
			}
			case BLOCKED -> {
				source.sendFailure(Component.literal("Перед игроком нет свободного места для Неизвестного."));
				yield 0;
			}
			case ALREADY_ACTIVE -> {
				source.sendFailure(Component.literal("Неизвестный уже смотрит на этого игрока."));
				yield 0;
			}
			case UNAVAILABLE -> {
				source.sendFailure(Component.literal("Событие Неизвестного сейчас недоступно."));
				yield 0;
			}
		};
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
