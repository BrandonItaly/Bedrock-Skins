package com.brandonitaly.bedrockskins.client.dummy;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.ServerLinks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
//? if >=1.21.11 {
import net.minecraft.world.attribute.EnvironmentAttributeMap;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}
import net.minecraft.util.valueproviders.ConstantInt;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public class DummyClientPacketListener extends ClientPacketListener {
    private static DummyClientPacketListener instance;
    private static final MappedRegistry<DimensionType> DUMMY_DIMENSION_TYPE_REGISTRY = new MappedRegistry<>(Registries.DIMENSION_TYPE, Lifecycle.stable());
    private static final RegistryAccess.Frozen DUMMY_REGISTRY_ACCESS = createRegistryAccess();

    public static DummyClientPacketListener getInstance() {
        if (instance == null) {
            instance = new DummyClientPacketListener();
        }
        return instance;
    }

    static Holder<DimensionType> getDummyDimensionTypeHolder() {
        return DUMMY_DIMENSION_TYPE_REGISTRY.getOrThrow(BuiltinDimensionTypes.OVERWORLD);
    }

    private DummyClientPacketListener() {
        super(
            Minecraft.getInstance(),
            new Connection(PacketFlow.CLIENTBOUND),
            new CommonListenerCookie(
                new LevelLoadTracker(),
                getProfile(),
                new WorldSessionTelemetryManager(TelemetryEventSender.DISABLED, true, Duration.ZERO, null),
                DUMMY_REGISTRY_ACCESS,
                FeatureFlags.DEFAULT_FLAGS,
                "bedrockskins-dummy",
                null,
                null,
                Map.of(),
                null,
                Map.of(),
                ServerLinks.EMPTY,
                Map.of(),
                true
            )
        );
    }

    private static GameProfile getProfile() {
        Minecraft minecraft = Minecraft.getInstance();
        GameProfile profile = minecraft.getGameProfile();
        return profile != null ? profile : new GameProfile(UUID.randomUUID(), "Preview");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RegistryAccess.Frozen createRegistryAccess() {
        RegistryAccess.Frozen builtins = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        Map registries = new HashMap();
        builtins.registries().forEach(entry -> registries.put(entry.key(), entry.value()));

        MappedRegistry<Biome> biomeRegistry = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        Biome dummyBiome = new Biome.BiomeBuilder()
            .hasPrecipitation(true)
            .temperature(0.8f)
            .downfall(0.4f)
            .specialEffects(
                new BiomeSpecialEffects.Builder()
                    .waterColor(4159204)
                    //? if <=1.21.10 {
                    /*.waterFogColor(329011)
                    .fogColor(12638463)
                    .skyColor(7907327)*/
                    //?}
                    .build()
            )
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build();
        Registry.register(biomeRegistry, Biomes.PLAINS, dummyBiome);

        MappedRegistry<DamageType> damageTypeRegistry = new MappedRegistry<>(Registries.DAMAGE_TYPE, Lifecycle.stable());
        registerDamageTypes(damageTypeRegistry);

        //? if >=1.21.11 {
        DimensionType dummyDimension = new DimensionType(
            false,
            true,
            false,
            1.0,
            -64,
            384,
            384,
            BlockTags.INFINIBURN_OVERWORLD,
            0.0f,
            new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
            DimensionType.Skybox.OVERWORLD,
            DimensionType.CardinalLightType.DEFAULT,
            EnvironmentAttributeMap.EMPTY,
            HolderSet.empty()
        );
        //?} else {
        /*DimensionType dummyDimension = new DimensionType(
            OptionalLong.empty(),
            true,
            false,
            false,
            false,
            1.0,
            true,
            false,
            -64,
            384,
            384,
            BlockTags.INFINIBURN_OVERWORLD,
            ResourceLocation.withDefaultNamespace("overworld"),
            0.0f,
            Optional.of(384),
            new DimensionType.MonsterSettings(false, true, ConstantInt.of(0), 0)
        );*/
        //?}
        Registry.register(DUMMY_DIMENSION_TYPE_REGISTRY, BuiltinDimensionTypes.OVERWORLD, dummyDimension);

        registries.put(Registries.BIOME, biomeRegistry);
        registries.put(Registries.DAMAGE_TYPE, damageTypeRegistry);
        registries.put(Registries.DIMENSION_TYPE, DUMMY_DIMENSION_TYPE_REGISTRY);

        return new RegistryAccess.ImmutableRegistryAccess(registries).freeze();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerDamageTypes(MappedRegistry<DamageType> damageTypeRegistry) {
        try {
            for (Field field : DamageTypes.class.getFields()) {
                if (!field.getType().equals(net.minecraft.resources.ResourceKey.class)) continue;
                var key = (net.minecraft.resources.ResourceKey<DamageType>) field.get(null);
                //? if >=1.21.11 {
                DamageType type = new DamageType(key.identifier().toString(), 0.0f);
                //?} else {
                /*DamageType type = new DamageType(key.location().toString(), 0.0f);*/
                //?}
                Registry.register(damageTypeRegistry, key, type);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to initialize dummy damage type registry", e);
        }
    }
}