package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModEntityTypes;
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
		ModEntityModelLayers.register();
		EntityRendererRegistry.register(ModEntityTypes.STALIN, context -> new GiantBossRenderer<>(context, HbkMod.id("textures/entity/stalin.png")));
		EntityRendererRegistry.register(ModEntityTypes.CJ, context -> new GiantBossRenderer<>(context, HbkMod.id("textures/entity/cj.png")));
		EntityRendererRegistry.register(ModEntityTypes.NKVD, NkvdRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.NURSE, NurseRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.KIRILL, KirillRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.LIZA, LizaRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.GIANT_ROCKET, context -> new ThrownItemRenderer<>(context, 1.75f, true));
		EntityRendererRegistry.register(ModEntityTypes.ATTACKING_MEMBER_BULLET, context -> new ThrownItemRenderer<>(context, 0.55f, true));
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
