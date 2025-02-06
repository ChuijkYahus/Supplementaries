package net.mehvahdjukaar.supplementaries.client.renderers.items;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.client.ItemStackRenderer;
import net.mehvahdjukaar.moonlight.api.client.util.RotHlpr;
import net.mehvahdjukaar.supplementaries.client.renderers.tiles.FlagBlockTileRenderer;
import net.mehvahdjukaar.supplementaries.common.items.FlagItem;
import net.mehvahdjukaar.supplementaries.configs.ClientConfigs;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static net.mehvahdjukaar.supplementaries.client.renderers.tiles.FlagBlockTileRenderer.renderBanner;


public class FlagItemRenderer extends ItemStackRenderer {

    private static final BlockState state = ModRegistry.FLAGS.get(DyeColor.BLACK).get().defaultBlockState();

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {

        matrixStackIn.pushPose();
        matrixStackIn.translate(-0.71875, 0, 0);

     CompoundTag com = stack.getTagElement("BlockEntityTag");
        ListTag listnbt = null;
        if (com != null && com.contains("Patterns")) {
            listnbt = com.getList("Patterns", 10);
        }
        List<Pair<Holder<BannerPattern>, DyeColor>> patterns = BannerBlockEntity.createPatterns(((FlagItem) stack.getItem()).getColor(), listnbt);

        matrixStackIn.pushPose();
        DyeColor color = ((FlagItem) stack.getItem()).getColor();

        if (ClientConfigs.Blocks.FLAG_BANNER.get()) {
            renderBanner(0, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn, patterns, color);
            return;
        }

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
        List<Pair<Holder<BannerPattern>, DyeColor>> patterns = BannerBlockEntity.createPatterns(((FlagItem) stack.getItem()).getColor(), listnbt);
        matrixStackIn.translate(0.5 + 0.0625, 0, 0.5);
        matrixStackIn.mulPose(RotHlpr.Y90);
        FlagBlockTileRenderer.renderPatterns(matrixStackIn, bufferIn, patterns, combinedLightIn);

        matrixStackIn.popPose();

    }
}