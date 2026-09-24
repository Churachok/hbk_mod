package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.CjEntity;
import dev.kirill.hbk.entity.ReferenceNpcEntity;
import dev.kirill.hbk.entity.LexEntity;
import dev.kirill.hbk.entity.SashaEntity;
import dev.kirill.hbk.entity.AttackingMemberBulletEntity;
import dev.kirill.hbk.entity.FoundingPenisProjectileEntity;
import dev.kirill.hbk.entity.GiantBossEntity;
import dev.kirill.hbk.entity.GiantRocketEntity;
import dev.kirill.hbk.entity.FlyingBlockEntity;
import dev.kirill.hbk.entity.FlyingCarpetEntity;
import dev.kirill.hbk.entity.KirillEntity;
import dev.kirill.hbk.entity.KirillDoomEntity;
import dev.kirill.hbk.entity.LizaEntity;
import dev.kirill.hbk.entity.MadLiberalEntity;
import dev.kirill.hbk.entity.NkvdEntity;
import dev.kirill.hbk.entity.NurseEntity;
import dev.kirill.hbk.entity.PinkFurryWolfEntity;
import dev.kirill.hbk.entity.StalinEntity;
import dev.kirill.hbk.entity.ColossalBombEntity;
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

	public static final EntityType<MadLiberalEntity> MAD_LIBERAL = register(
			"mad_liberal",
			EntityType.Builder.<MadLiberalEntity>of(MadLiberalEntity::new, MobCategory.MONSTER)
					.sized(0.9f, 2.7f).eyeHeight(2.43f).canSpawnFarFromPlayer()
					.clientTrackingRange(12).updateInterval(2).notInPeaceful()
	);

	public static final EntityType<KirillDoomEntity> KIRILL_DOOM = register(
			"kirill_doom",
			EntityType.Builder.<KirillDoomEntity>of(KirillDoomEntity::new, MobCategory.MONSTER)
					.sized(0.9f, 2.7f).eyeHeight(2.43f).canSpawnFarFromPlayer()
					.clientTrackingRange(16).updateInterval(2).notInPeaceful()
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

	public static final EntityType<FoundingPenisProjectileEntity> FOUNDING_PENIS_PROJECTILE = register(
			"founding_penis_projectile",
			EntityType.Builder.<FoundingPenisProjectileEntity>of(FoundingPenisProjectileEntity::new, MobCategory.MISC)
					.sized(2.0f, 2.0f)
					.clientTrackingRange(16)
					.updateInterval(1)
	);

	public static final EntityType<ColossalBombEntity> COLOSSAL_BOMB = register(
			"colossal_bomb",
			EntityType.Builder.<ColossalBombEntity>of(ColossalBombEntity::new, MobCategory.MISC)
					.sized(2.0f, 8.0f).clientTrackingRange(16).updateInterval(2)
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

	public static final EntityType<ReferenceNpcEntity> ANTON = register("anton",
			EntityType.Builder.<ReferenceNpcEntity>of(ReferenceNpcEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(8).updateInterval(3));

	public static final EntityType<ReferenceNpcEntity> DENIS = register("denis",
			EntityType.Builder.<ReferenceNpcEntity>of(ReferenceNpcEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(8).updateInterval(3));

	public static final EntityType<ReferenceNpcEntity> GOSHA = register("gosha",
			EntityType.Builder.<ReferenceNpcEntity>of(ReferenceNpcEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(8).updateInterval(3));

	public static final EntityType<ReferenceNpcEntity> GRISHA = register("grisha",
			EntityType.Builder.<ReferenceNpcEntity>of(ReferenceNpcEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(8).updateInterval(3));

	public static final EntityType<ReferenceNpcEntity> LESHA = register("lesha",
			EntityType.Builder.<ReferenceNpcEntity>of(ReferenceNpcEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(8).updateInterval(3));

	public static final EntityType<SashaEntity> SASHA = register("sasha",
			EntityType.Builder.<SashaEntity>of(SashaEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(8).updateInterval(3).notInPeaceful());

	public static final EntityType<ReferenceNpcEntity> VLAD = register("vlad",
			EntityType.Builder.<ReferenceNpcEntity>of(ReferenceNpcEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(8).updateInterval(3));

	public static final EntityType<LexEntity> LEX = register("lex",
			EntityType.Builder.<LexEntity>of(LexEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 0.7f).eyeHeight(0.35f).clientTrackingRange(8).updateInterval(3));

	public static final EntityType<PinkFurryWolfEntity> PINK_FURRY_WOLF = register("furry_wolf",
			EntityType.Builder.<PinkFurryWolfEntity>of(PinkFurryWolfEntity::new, MobCategory.CREATURE)
					.sized(0.7f, 1.95f).eyeHeight(1.72f).clientTrackingRange(8).updateInterval(3));

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
		FabricDefaultAttributeRegistry.register(MAD_LIBERAL, MadLiberalEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(KIRILL_DOOM, KirillDoomEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(NKVD, NkvdEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(NURSE, NurseEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(KIRILL, KirillEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LIZA, LizaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ANTON, ReferenceNpcEntity.createAttributes(1.0));
		FabricDefaultAttributeRegistry.register(DENIS, ReferenceNpcEntity.createAttributes(20.0));
		FabricDefaultAttributeRegistry.register(GOSHA, ReferenceNpcEntity.createAttributes(20.0));
		FabricDefaultAttributeRegistry.register(GRISHA, ReferenceNpcEntity.createAttributes(20.0));
		FabricDefaultAttributeRegistry.register(LESHA, ReferenceNpcEntity.createAttributes(1.0));
		FabricDefaultAttributeRegistry.register(SASHA, ReferenceNpcEntity.createAttributes(20.0));
		FabricDefaultAttributeRegistry.register(VLAD, ReferenceNpcEntity.createAttributes(20.0));
		FabricDefaultAttributeRegistry.register(LEX, LexEntity.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 20.0));
		FabricDefaultAttributeRegistry.register(PINK_FURRY_WOLF, PinkFurryWolfEntity.createAttributes());
	}
}
