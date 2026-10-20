package dev.kirill.hbk.universe;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class UniverseCommands {
	private UniverseCommands() {}
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(Commands.literal("universe")
				.then(Commands.literal("return").executes(ctx -> UniverseManager.returnHome(ctx.getSource().getPlayerOrException()) ? 1 : 0))
				.then(Commands.literal("info").executes(ctx -> info(ctx.getSource(), UniverseProfile.of(ctx.getSource().getLevel()))))
				.then(Commands.literal("ecology").executes(ctx -> ecology(ctx.getSource(), UniverseProfile.of(ctx.getSource().getLevel()))))
				.then(Commands.literal("hazards").executes(ctx -> hazards(ctx.getSource())))
				.then(Commands.literal("profile").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("code", StringArgumentType.word()).executes(ctx -> {
							String address = StringArgumentType.getString(ctx, "code");
							return UniverseCode.valid(address) ? info(ctx.getSource(), UniverseProfile.of(new UniverseCode(address))) : invalid(ctx.getSource());
						})))
				.then(Commands.literal("tp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("code", StringArgumentType.word()).executes(ctx -> {
							String address = StringArgumentType.getString(ctx, "code");
							if (!UniverseCode.valid(address)) return invalid(ctx.getSource());
							return UniverseManager.travel(ctx.getSource().getPlayerOrException(), new UniverseCode(address)) ? 1 : 0;
						})))));
	}
	private static int invalid(CommandSourceStack source) { source.sendFailure(Component.translatable("message.hbk.universe.invalid")); return 0; }
	private static int hazards(CommandSourceStack source) {
		var p = UniverseProfile.of(source.getLevel());
		if (p == null) { source.sendFailure(Component.translatable("message.hbk.universe.outside")); return 0; }
		for (var kind : java.util.List.of(UniverseHazards.gasKind(p), UniverseHazards.Kind.UPDRAFT, UniverseHazards.Kind.HEAVY_FIELD, UniverseHazards.Kind.RIFT)) {
			var zone = UniverseHazards.nearest(p, source.getPosition(), kind);
			if (zone != null) source.sendSuccess(() -> Component.translatable("message.hbk.universe.hazard_location", Component.translatable(kind.key()),
					(int) zone.center().x, (int) zone.center().y, (int) zone.center().z), false);
		}
		return 1;
	}
	private static int ecology(CommandSourceStack source, UniverseProfile p) {
		if (p == null) { source.sendFailure(Component.translatable("message.hbk.universe.outside")); return 0; }
		var habitat = UniverseEcology.HABITATS.get(p.geometry());
		source.sendSuccess(() -> Component.translatable("message.hbk.universe.ecology", Component.translatable("habitat.hbk." + habitat.name()),
				habitat.log().getName(), habitat.focusOre().getName()), false);
		source.sendSuccess(() -> Component.translatable("message.hbk.universe.harvest", UniverseBotany.block(p.geometry(), "plant").getName(),
				new net.minecraft.world.item.ItemStack(UniverseBotany.fruit(p.geometry())).getHoverName(),
				Component.translatable("ecology.hbk." + UniverseBotany.SPECIES.get(p.geometry()))), false);
		return 1;
	}
	private static int info(CommandSourceStack source, UniverseProfile p) {
		if (p == null) { source.sendFailure(Component.translatable("message.hbk.universe.outside")); return 0; }
		source.sendSuccess(() -> Component.literal("HBK " + p.code() + " | v" + p.version() + " | seed=" + p.seed()
				+ " | geometry/relief/geology/matter/water=" + p.geometry() + "/" + p.relief() + "/" + p.geology() + "/" + p.distribution() + "/" + p.hydrology()
				+ " | materials=" + UniverseMaterials.FAMILIES.get(p.geometry())), false);
		ecology(source, p);
		for (HiddenParameter parameter : HiddenParameter.values()) source.sendSuccess(() -> Component.literal(parameter.name() + "=" + p.get(parameter)), false);
		if (source.getLevel().getChunkSource().getGenerator() instanceof UniverseChunkGenerator g) source.sendSuccess(
				() -> Component.literal("chunks=" + g.generatedChunks() + "; density/material generation mean=" + g.averageMillis() + " ms"), false);
		return 1;
	}
}
