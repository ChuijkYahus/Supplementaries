package net.mehvahdjukaar.supplementaries.client.renderers.items;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.client.ItemStackRenderer;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.mehvahdjukaar.supplementaries.client.BlackboardTextureManager;
import net.mehvahdjukaar.supplementaries.common.items.components.BlackboardData;
import net.mehvahdjukaar.supplementaries.reg.ClientRegistry;
import net.mehvahdjukaar.supplementaries.reg.ModComponents;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;


public class BlackboardItemRenderer extends ItemStackRenderer {
    private static final BlockState STATE = ModRegistry.BLACKBOARD.get().defaultBlockState();

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {

        matrixStackIn.pushPose();
        matrixStackIn.translate(0,0,-0.34375);

        var blockRenderer = Minecraft.getInstance().getBlockRenderer();
        var model = ClientHelper.getModel(blockRenderer.getBlockModelShaper().getModelManager(),
                ClientRegistry.BLACKBOARD_FRAME);
        blockRenderer.getModelRenderer().renderModel(matrixStackIn.last(), bufferIn.getBuffer(ItemBlockRenderTypes.getRenderType(STATE, false)),
                STATE, model, 1, 1, 1, combinedLightIn, combinedOverlayIn);

        BlackboardData data = stack.getOrDefault(ModComponents.BLACKBOARD.get(), BlackboardData.EMPTY);

        var blackboard = BlackboardTextureManager.getInstance(data);
        VertexConsumer builder = bufferIn.getBuffer(blackboard.getRenderType());

        int lu = VertexUtil.lightU(combinedLightIn);
        int lv = VertexUtil.lightV(combinedLightIn);

        matrixStackIn.translate(0, 0, 0.6875);
        VertexUtil.addQuad(builder, matrixStackIn, 0, 0, 1, 1, lu, lv);

        matrixStackIn.popPose();
    }
}