package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import dev.kirill.hbk.network.ModNetworking;

public class HbkClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KirillDoomMusic.register();
		ModEntityModelLayers.register();
		ArmorRenderer.register(UraniumArmorRenderer::new, ModItems.URANIUM_HELMET, ModItems.URANIUM_CHESTPLATE,
				ModItems.URANIUM_LEGGINGS, ModItems.URANIUM_BOOTS);
		EntityRendererRegistry.register(ModEntityTypes.STALIN, context -> new GiantBossRenderer<>(context, HbkMod.id("textures/entity/stalin.png")));
		EntityRendererRegistry.register(ModEntityTypes.CJ, context -> new GiantBossRenderer<>(context, HbkMod.id("textures/entity/cj.png")));
		EntityRendererRegistry.register(ModEntityTypes.MAD_LIBERAL, context -> new HumanoidBossRenderer<>(context,
				ModEntityModelLayers.MAD_LIBERAL, 1.5f,
				HbkMod.id("textures/entity/mad_liberal.png"),
				HbkMod.id("textures/entity/mad_liberal_cracked_1.png"),
				HbkMod.id("textures/entity/mad_liberal_cracked_2.png"),
				HbkMod.id("textures/entity/mad_liberal_cracked_3.png"),
				HbkMod.id("textures/entity/mad_liberal_cracked_4.png")));
		EntityRendererRegistry.register(ModEntityTypes.KIRILL_DOOM, context -> new HumanoidBossRenderer<>(context, 1.5f,
				HbkMod.id("textures/entity/kirill_doom.png")));
		EntityRendererRegistry.register(ModEntityTypes.NKVD, NkvdRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.NURSE, NurseRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.KIRILL, KirillRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.LIZA, LizaRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.ANTON, context -> new ReferenceNpcRenderer(context, "anton"));
		EntityRendererRegistry.register(ModEntityTypes.DENIS, context -> new ReferenceNpcRenderer(context, "denis"));
		EntityRendererRegistry.register(ModEntityTypes.GOSHA, context -> new ReferenceNpcRenderer(context, "gosha"));
		EntityRendererRegistry.register(ModEntityTypes.GRISHA, context -> new ReferenceNpcRenderer(context, "grisha"));
		EntityRendererRegistry.register(ModEntityTypes.LESHA, context -> new ReferenceNpcRenderer(context, "lesha"));
		EntityRendererRegistry.register(ModEntityTypes.SASHA, context -> new ReferenceNpcRenderer(context, "sasha"));
		EntityRendererRegistry.register(ModEntityTypes.VLAD, context -> new ReferenceNpcRenderer(context, "vlad"));
		EntityRendererRegistry.register(ModEntityTypes.LEX, LexRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.PINK_FURRY_WOLF, PinkFurryWolfRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.CATGIRL, CatgirlRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.GIANT_ROCKET, context -> new ThrownItemRenderer<>(context, 1.75f, true));
		EntityRendererRegistry.register(ModEntityTypes.ATTACKING_MEMBER_BULLET, context -> new ThrownItemRenderer<>(context, 0.55f, true));
		EntityRendererRegistry.register(ModEntityTypes.FOUNDING_PENIS_PROJECTILE, context -> new ThrownItemRenderer<>(context, 3.0f, true));
		EntityRendererRegistry.register(ModEntityTypes.COLOSSAL_BOMB, context -> new ThrownItemRenderer<>(context, 8.0f, true));
		EntityRendererRegistry.register(ModEntityTypes.FLYING_CARPET, FlyingCarpetRenderer::new);
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (entityType == EntityTypes.PLAYER) {
				@SuppressWarnings("unchecked")
				RenderLayerParent<AvatarRenderState, PlayerModel> playerRenderer =
						(RenderLayerParent<AvatarRenderState, PlayerModel>) renderer;
				helper.register(new AttackingMemberBodyLayer(playerRenderer));
			}
		});
		registerFlyingBlockRenderer();
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OpenStrangeChestPayload.TYPE, (payload, context) ->
				context.client().execute(() -> context.client().setScreenAndShow(new StrangeChestScreen(payload.pos())))
		);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void registerFlyingBlockRenderer() {
		EntityRendererRegistry.register(ModEntityTypes.FLYING_BLOCK, context -> (net.minecraft.client.renderer.entity.EntityRenderer) new FallingBlockRenderer(context));
	}
}
