package net.mehvahdjukaar.supplementaries.common.misc.mob_container;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluid;
import net.mehvahdjukaar.moonlight.api.misc.BiggerStreamCodecs;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.supplementaries.api.CapturedMobInstance;
import net.mehvahdjukaar.supplementaries.api.ICatchableMob;
import net.mehvahdjukaar.supplementaries.common.items.JarItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class DataDefinedCatchableMob implements ICatchableMob {

    public static final Codec<DataDefinedCatchableMob> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().fieldOf("owners").forGetter(p -> p.owners),
            Codec.FLOAT.fieldOf("width_increment").forGetter(p -> p.widthIncrement),
            Codec.FLOAT.fieldOf("height_increment").forGetter(p -> p.heightIncrement),
            Codec.intRange(0, 15).optionalFieldOf("light_level", 0).forGetter(p -> p.lightLevel),
            CaptureSettings.CODEC.optionalFieldOf("allowed_in").forGetter(p -> p.captureSettings),
            Codec.INT.optionalFieldOf("fish_index", 0).forGetter(p -> p.fishIndex),
            BuiltinAnimation.Type.CODEC.optionalFieldOf("animation", BuiltinAnimation.Type.NONE)
                    .forGetter(b -> b.builtinAnimation),
            TickMode.CODEC.optionalFieldOf("tick_mode", TickMode.NONE).forGetter(p -> p.tickMode),
            SoftFluid.HOLDER_CODEC.optionalFieldOf("render_fluid").forGetter(p -> p.renderFluid),
            LootParam.CODEC.optionalFieldOf("loot").forGetter(p -> p.loot)
    ).apply(instance, DataDefinedCatchableMob::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DataDefinedCatchableMob> STREAM_CODEC = BiggerStreamCodecs
            .composite(
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), p -> p.owners,
                    ByteBufCodecs.FLOAT, p -> p.widthIncrement,
                    ByteBufCodecs.FLOAT, p -> p.heightIncrement,
                    ByteBufCodecs.INT, p -> p.lightLevel,
                    CaptureSettings.STREAM_CODEC.apply(ByteBufCodecs::optional), p -> p.captureSettings,
                    ByteBufCodecs.INT, p -> p.fishIndex,
                    BuiltinAnimation.Type.STREAM_CODEC, p -> p.builtinAnimation,
                    TickMode.STREAM_CODEC, p -> p.tickMode,
                    SoftFluid.STREAM_CODEC.apply(ByteBufCodecs::optional), p -> p.renderFluid,
                    (r, w, h, l, s, f, a, t, sf) -> new DataDefinedCatchableMob(r, w, h, l, s, f, a, t, sf, Optional.empty())
            );

    private final List<ResourceLocation> owners;
    final float widthIncrement;
    final float heightIncrement;
    final int lightLevel;
    final Optional<CaptureSettings> captureSettings;
    final int fishIndex;
    final BuiltinAnimation.Type builtinAnimation;
    final TickMode tickMode;
    final Optional<LootParam> loot;
    final Optional<Holder<SoftFluid>> renderFluid;

    private DataDefinedCatchableMob(List<ResourceLocation> owners, float widthIncrement, float heightIncrement, int lightLevel,
                                    Optional<CaptureSettings> captureSettings,
                                    int fishIndex, BuiltinAnimation.Type builtinAnimation, TickMode tickMode,
                                    Optional<Holder<SoftFluid>> forceFluidID, Optional<LootParam> loot) {
        this.widthIncrement = widthIncrement;
        this.heightIncrement = heightIncrement;
        this.lightLevel = lightLevel;
        this.captureSettings = captureSettings;
        this.fishIndex = fishIndex;
        this.builtinAnimation = builtinAnimation;
        this.renderFluid = forceFluidID;
        this.loot = loot;
        this.tickMode = tickMode;
        this.owners = owners;
    }

    List<ResourceLocation> getOwners() {
        return owners;
    }

    @Override
    public <T extends Entity> CapturedMobInstance<T> createCapturedMobInstance(T self, float containerWidth, float containerHeight) {
        return new DataCapturedMobInstance<>(self, this);
    }

    @Override
    public boolean canBeCaughtWithItem(Entity entity, Item item, Player player) {
        return captureSettings.map(settings -> settings.canCapture(entity, item))
                .orElseGet(() -> ICatchableMob.super.canBeCaughtWithItem(entity, item, player));
    }

    @Override
    public int getLightLevel(Level world, BlockPos pos) {
        return lightLevel;
    }

    @Override
    public boolean shouldHover(Entity self, boolean waterlogged) {
        var cat = this.builtinAnimation;
        if (cat.isLand()) return false;
        return cat.isFlying() || ICatchableMob.super.shouldHover(self, waterlogged);
    }

    @Override
    public Optional<Holder<SoftFluid>> getForceFluid() {
        return this.renderFluid;
    }

    @Override
    public int getFishTextureIndex() {
        return fishIndex;
    }

    @Override
    public float getHitBoxWidthIncrement(Entity entity) {
        return widthIncrement;
    }

    @Override
    public float getHitBoxHeightIncrement(Entity entity) {
        return heightIncrement;
    }


    protected record CaptureSettings(CatchMode jarMode, CatchMode cageMode) {
        private static final Codec<CaptureSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CatchMode.CODEC.fieldOf("jar").forGetter(CaptureSettings::jarMode),
                CatchMode.CODEC.fieldOf("cage").forGetter(CaptureSettings::cageMode)
        ).apply(instance, CaptureSettings::new));

        private static final StreamCodec<FriendlyByteBuf, CaptureSettings> STREAM_CODEC = StreamCodec.composite(
                CatchMode.STREAM_CODEC, CaptureSettings::jarMode,
                CatchMode.STREAM_CODEC, CaptureSettings::cageMode,
                CaptureSettings::new
        );

        public boolean canCapture(Entity entity, Item item) {
            if (item instanceof JarItem) {
                return this.jarMode.on && (!this.jarMode.onlyBaby ||
                        (!(entity instanceof LivingEntity le) || le.isBaby()));
            } else {
                return this.cageMode.on && (!this.cageMode.onlyBaby ||
                        (!(entity instanceof LivingEntity le) || le.isBaby()));
            }
        }
    }

    protected record CatchMode(boolean on, boolean onlyBaby) {
        private static final Codec<CatchMode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("allow").forGetter(CatchMode::on),
                Codec.BOOL.optionalFieldOf("only_baby", false).forGetter(CatchMode::onlyBaby)
        ).apply(instance, CatchMode::new));

        private static final StreamCodec<FriendlyByteBuf, CatchMode> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, CatchMode::on,
                ByteBufCodecs.BOOL, CatchMode::onlyBaby,
                CatchMode::new
        );
    }

    protected record LootParam(ResourceKey<LootTable> tableId, float chance) {
        private static final Codec<LootParam> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC
                        .xmap(i -> ResourceKey.create(Registries.LOOT_TABLE, i), ResourceKey::location)
                        .fieldOf("loot_table").forGetter(LootParam::tableId),
                Codec.floatRange(0, 1).fieldOf("chance").forGetter(LootParam::chance)
        ).apply(instance, LootParam::new));

        public void tryDropping(ServerLevel serverLevel, BlockPos pos, Entity entity) {
            if (serverLevel.random.nextFloat() < chance) {
                LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(tableId);
                LootParams.Builder builder = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.BLOCK_STATE, serverLevel.getBlockState(pos))
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, entity);

                var l = lootTable.getRandomItems(builder.create(LootContextParamSets.GIFT));
                for (var o : l) {
                    entity.spawnAtLocation(o);
                }
                //ItemsUtil.addToInventory(serverLevel, pos.below(), );
            }
        }
    }

    enum TickMode implements StringRepresentable {
        NONE, SERVER, CLIENT, BOTH;

        boolean isValid(Level level) {
            return switch (this) {
                case NONE -> false;
                case CLIENT -> level.isClientSide;
                case SERVER -> !level.isClientSide;
                case BOTH -> true;
            };
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public static final Codec<TickMode> CODEC = StringRepresentable.fromEnum(TickMode::values);
        public static final StreamCodec<FriendlyByteBuf, TickMode> STREAM_CODEC = Utils.enumStreamCodec(TickMode.class);

    }

}
