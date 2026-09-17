package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.CjEntity;
import dev.kirill.hbk.entity.AttackingMemberBulletEntity;
import dev.kirill.hbk.entity.GiantBossEntity;
import dev.kirill.hbk.entity.GiantRocketEntity;
import dev.kirill.hbk.entity.FlyingBlockEntity;
import dev.kirill.hbk.entity.FlyingCarpetEntity;
import dev.kirill.hbk.entity.KirillEntity;
import dev.kirill.hbk.entity.LizaEntity;
import dev.kirill.hbk.entity.NkvdEntity;
import dev.kirill.hbk.entity.NurseEntity;
import dev.kirill.hbk.entity.StalinEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntityTypes {
	public static final EntityType<StalinEntity> STALIN = register(
			"stalin",
			giant(StalinEntity::new)
	);

	public static final EntityType<CjEntity> CJ = register(
			"cj",
			giant(CjEntity::new)
	);

	public static final EntityType<NkvdEntity> NKVD = register(
			"nkvd",
			EntityType.Builder.<NkvdEntity>of(NkvdEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.8f)
					.eyeHeight(1.62f)
					.clientTrackingRange(8)
					.updateInterval(3)
	);

	public static final EntityType<NurseEntity> NURSE = register(
			"nurse",
			EntityType.Builder.<NurseEntity>of(NurseEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f)
					.eyeHeight(1.62f)
					.clientTrackingRange(8)
					.updateInterval(3)
	);

	public static final EntityType<KirillEntity> KIRILL = register(
			"kirill",
			EntityType.Builder.<KirillEntity>of(KirillEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f)
					.eyeHeight(1.62f)
					.clientTrackingRange(8)
					.updateInterval(3)
	);

	public static final EntityType<LizaEntity> LIZA = register(
			"liza",
			EntityType.Builder.<LizaEntity>of(LizaEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f)
					.eyeHeight(1.62f)
					.clientTrackingRange(8)
					.updateInterval(3)
	);

	public static final EntityType<GiantRocketEntity> GIANT_ROCKET = register(
			"giant_rocket",
			EntityType.Builder.<GiantRocketEntity>of(GiantRocketEntity::new, MobCategory.MISC)
					.sized(0.4f, 0.4f)
					.clientTrackingRange(8)
					.updateInterval(10)
					.fireImmune()
	);

	public static final EntityType<AttackingMemberBulletEntity> ATTACKING_MEMBER_BULLET = register(
			"attacking_member_bullet",
			EntityType.Builder.<AttackingMemberBulletEntity>of(AttackingMemberBulletEntity::new, MobCategory.MISC)
					.sized(0.22f, 0.22f)
					.clientTrackingRange(8)
					.updateInterval(1)
	);

	public static final EntityType<FlyingBlockEntity> FLYING_BLOCK = register(
			"flying_block",
			EntityType.Builder.<FlyingBlockEntity>of(FlyingBlockEntity::new, MobCategory.MISC)
					.sized(0.98f, 0.98f)
					.clientTrackingRange(10)
					.updateInterval(2)
	);

	public static final EntityType<FlyingCarpetEntity> FLYING_CARPET = register(
			"flying_carpet",
			EntityType.Builder.<FlyingCarpetEntity>of(FlyingCarpetEntity::new, MobCategory.MISC)
					.sized(2.0f, 0.18f)
					.clientTrackingRange(12)
					.updateInterval(1)
	);

	private static <T extends GiantBossEntity> EntityType.Builder<T> giant(EntityType.EntityFactory<T> factory) {
		return EntityType.Builder.of(factory, MobCategory.MONSTER)
				.sized(GiantBossEntity.HITBOX_WIDTH, GiantBossEntity.HITBOX_HEIGHT)
				.eyeHeight(GiantBossEntity.EYE_HEIGHT)
				.canSpawnFarFromPlayer()
				.clientTrackingRange(16)
				.updateInterval(3);
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, HbkMod.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void register() {
		HbkMod.LOGGER.info("Registered entity types for {}", HbkMod.MOD_ID);
	}

	public static void registerAttributes() {
		FabricDefaultAttributeRegistry.register(STALIN, StalinEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CJ, CjEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(NKVD, NkvdEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(NURSE, NurseEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(KIRILL, KirillEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LIZA, LizaEntity.createAttributes());
	}
}
