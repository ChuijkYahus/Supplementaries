package net.mehvahdjukaar.supplementaries.client.renderers.items;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.mehvahdjukaar.moonlight.api.client.ItemStackRenderer;
import net.mehvahdjukaar.moonlight.api.client.model.BakedQuadBuilder;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.mehvahdjukaar.supplementaries.configs.ClientConfigs;
import net.mehvahdjukaar.supplementaries.reg.ClientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AltimeterItemRenderer extends ItemStackRenderer {

    private static final Map<ResourceKey<Level>, Pair<TextureAtlasSprite, Int2ObjectMap<BakedModel>>> MODEL_CACHE = new HashMap<>();

    public static Map<ResourceKey<Level>, Pair<TextureAtlasSprite, Int2ObjectMap<BakedModel>>> getModelCache() {
        if (MODEL_CACHE.isEmpty()) {
            List<ResourceLocation> resourceLocations = new ArrayList<>(ClientConfigs.Items.DEPTH_METER_DIMENSIONS.get());
            resourceLocations.add(Level.OVERWORLD.location());
            for (var d : resourceLocations) {
                ResourceKey<Level> res = ResourceKey.create(Registries.DIMENSION, d);
                TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(
                        Supplementaries.res("item/altimeter/" + d.toString().replace(":", "_"))
                );
                if (sprite != null) {
                    MODEL_CACHE.put(res, Pair.of(sprite, new Int2ObjectOpenHashMap<>()));
                }
            }
        }
        return MODEL_CACHE;
    }

    public static void onReload() {
        MODEL_CACHE.clear();
    }


    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);


        ClientLevel level = Minecraft.getInstance().level;
        ResourceKey<Level> dimension = level == null ? Level.OVERWORLD : level.dimension();

        var cache = getModelCache();

        var pair = cache.getOrDefault(dimension, cache.get(Level.OVERWORLD));
        if (pair == null) {
            Supplementaries.error();
            return;
        }
        TextureAtlasSprite sprite = pair.getFirst();
        int textureH = sprite.contents().height();

        double stripDepth = calculateDepthIndex(level, textureH);
        int mult = ClientConfigs.Items.DEPTH_METER_STEP_MULT.get();
        int index = (int) Math.round(stripDepth * mult);

        BakedModel model = pair.getSecond().computeIfAbsent(index, i ->
                new AltimeterModel(index / (float) mult, textureH, sprite));
        Minecraft.getInstance().getItemRenderer().render(Items.DIAMOND.getDefaultInstance(), transformType,
                false, poseStack, buffer, packedLight, packedOverlay, model);
        poseStack.popPose();
    }

    private static double calculateDepthIndex(@Nullable ClientLevel level, int textureH) {
        int min = level == null ? -64 : level.getMinBuildHeight();
        int max = level == null ? 312 : level.getMaxBuildHeight();

        LocalPlayer player = Minecraft.getInstance().player;
        double depth = player == null ? 64 : player.position().y;
        //from 0 to 1
        double normDepth = Mth.clamp((depth - min) / (max - min), 0, 1);
        return (normDepth * (textureH - 6));
    }



    private static class AltimeterModel implements BakedModel {

        private final List<BakedQuad> quads = new ArrayList<>();
        private final ItemOverrides overrides;
        private final ItemTransforms transforms;

        AltimeterModel(float depth, int textureH, TextureAtlasSprite sprite) {
            int h = 5;

            float invDepth = textureH - depth - h;
            float shrink = sprite.uvShrinkRatio();
            try (BakedQuadBuilder builder = BakedQuadBuilder.create(sprite, quads::add)) {
                builder.setAutoDirection();
                PoseStack ps = new PoseStack();
                float u0 = 0;
                float u1 = 0.25f;
                float u3 = 0.25f;
                float u4 = 0.5f;
                for (int j = 0; j < 2; j++) {
                    ps.translate(0, 0, 15 / 32f);
                    addScaledQuad(builder, ps, shrink,
                            false,
                            0.375f, 0.375f, 0.625f, 0.6875f,
                            u0, invDepth / textureH, u1, (invDepth + h) / textureH);
                    addScaledQuad(builder, ps, shrink,
                            true,
                            0.375f, 0.6875f, 0.625f, 0.75f,
                            u3, (invDepth - 1) / textureH, u4, invDepth / textureH);
                    ps.scale(-1, 1, -1);
                    ps.translate(-1, 0, -17 / 32f);
                    u0 = 0.25f;
                    u1 = 0;
                    u3 = 0.5f;
                    u4 = 0.25f;
                }
            } catch (Exception e) {
                Supplementaries.error("Failed to create altimeter model");
            }


            BakedModel copy = ClientHelper.getModel(Minecraft.getInstance().getModelManager(), ClientRegistry.ALTIMETER_TEMPLATE);
            this.quads.addAll(copy.getQuads(null, null, RandomSource.create()));
            this.overrides = copy.getOverrides();
            this.transforms = copy.getTransforms();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
            return direction == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public boolean isGui3d() {
            return false;
        }

        @Override
        public boolean usesBlockLight() {
            return false;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return null;
        }

        @Override
        public ItemTransforms getTransforms() {
            return transforms;
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }


        private static void addScaledQuad(BakedQuadBuilder builder, PoseStack ps,
                                          float shrink,
                                          boolean top,
                                          float x0, float y0,
                                          float x1, float y1,
                                          float u0, float v0, float u1, float v1) {
            float ix0 = shrink * (x0 - 0.5f) * 2;
            float ix1 = shrink * (x1 - 0.5f) * 2;
            float iy0 = top ? 0 : shrink * (y0 - 0.5f) * 2;
            float iy1 = !top ? 0 : shrink * (y1 - 0.5f) * 2;
            VertexUtil.addQuad(builder, ps,
                    x0 + ix0, y0 + iy0, x1 + ix1, y1 + iy1,
                    u0 / 16f, v0 / 16f, u1 / 16f, v1 / 16f,
                    255, 255, 255, 255,
                    0, 0);
        }
    }
}
