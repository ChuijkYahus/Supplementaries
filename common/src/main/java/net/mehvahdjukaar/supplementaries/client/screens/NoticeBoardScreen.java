package net.mehvahdjukaar.supplementaries.client.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.moonlight.api.client.util.LOD;
import net.mehvahdjukaar.supplementaries.client.renderers.tiles.NoticeBoardBlockTileRenderer;
import net.mehvahdjukaar.supplementaries.common.block.tiles.NoticeBoardBlockTile;
import net.mehvahdjukaar.supplementaries.common.inventories.NoticeBoardContainerMenu;
import net.mehvahdjukaar.supplementaries.integration.CompatHandler;
import net.mehvahdjukaar.supplementaries.integration.ImmediatelyFastCompat;
import net.mehvahdjukaar.supplementaries.reg.ModTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ComplexItem;


public class NoticeBoardScreen extends AbstractContainerScreen<NoticeBoardContainerMenu> {

    private final NoticeBoardBlockTile tile;
    private final CyclingSlotBackground slotBG = new CyclingSlotBackground(0);

    public NoticeBoardScreen(NoticeBoardContainerMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.tile = container.getContainer();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        if (CompatHandler.IMMEDIATELY_FAST) ImmediatelyFastCompat.startBatching();
        graphics.blit(ModTextures.NOTICE_BOARD_GUI_TEXTURE, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight);
        this.slotBG.render(this.menu, graphics, partialTicks, this.leftPos, this.topPos);
        var stack = tile.getDisplayedItem();
        if (!stack.isEmpty()) {

            graphics.blit(ModTextures.NOTICE_BOARD_GUI_TEXTURE, this.leftPos + 88, this.topPos + 13,
                    this.imageWidth, 0, 48, 56);

            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(this.leftPos + 112, this.topPos + 41, 1.0F);
            poseStack.scale(64, -64, -1);
            if (stack.getItem() instanceof ComplexItem) {
                poseStack.scale(15 / 16f, 15 / 16f, 1);
            }

            MapRenderer mr = this.minecraft.gameRenderer.getMapRenderer();
            MultiBufferSource.BufferSource buffer = graphics.bufferSource();
            NoticeBoardBlockTileRenderer.renderNoticeBoardContent(mr, font, minecraft.getItemRenderer(),
                    tile, graphics.pose(), buffer,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, stack, Direction.UP, LOD.MAX
            );
            poseStack.popPose();
        }
        if (CompatHandler.IMMEDIATELY_FAST) ImmediatelyFastCompat.endBatching();
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        return super.keyPressed(key, b, c);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.slotBG.tick(ModTextures.NOTICE_BOARD_SLOT_ICONS);
    }

}