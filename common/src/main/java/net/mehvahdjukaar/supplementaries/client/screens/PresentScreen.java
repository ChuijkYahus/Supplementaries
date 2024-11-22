package net.mehvahdjukaar.supplementaries.client.screens;


import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.supplementaries.client.screens.widgets.MultiLineEditBoxWidget;
import net.mehvahdjukaar.supplementaries.client.screens.widgets.PlayerSuggestionBoxWidget;
import net.mehvahdjukaar.supplementaries.common.block.tiles.PresentBlockTile;
import net.mehvahdjukaar.supplementaries.common.inventories.PresentContainerMenu;
import net.mehvahdjukaar.supplementaries.common.network.ServerBoundSetPresentPacket;
import net.mehvahdjukaar.supplementaries.reg.ModTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class PresentScreen extends AbstractContainerScreen<PresentContainerMenu> implements ContainerListener {

    private static final int DESCRIPTION_BOX_X = 53;
    private static final int DESCRIPTION_BOX_Y = 33;
    private static final int DESCRIPTION_BOX_H = 36;
    private static final int DESCRIPTION_BOX_W = 105;
    private static final int SUGGESTION_BOX_Y = 19;
    private static final int SUGGESTION_BOX_W = 99;
    private static final int SUGGESTION_BOX_H = 12;

    private final PresentBlockTile tile;

    private PackButton packButton;
    private PlayerSuggestionBoxWidget recipient;
    private MultiLineEditBoxWidget descriptionBox;

    private boolean packed;
    //hasn't received items yet
    private boolean needsInitialization = true;

    public PresentScreen(PresentContainerMenu menu, Inventory inventory, Component text) {
        super(menu, inventory, text);
        this.imageWidth = 176;
        this.imageHeight = 166;

        this.tile = (PresentBlockTile) menu.getContainer();
    }

    @Override
    public void init() {
        super.init();

        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        int i = this.leftPos;
        int j = this.topPos;

        this.packButton = this.addRenderableWidget(new PackButton(i + 14, j + 45));

        this.recipient = this.addRenderableWidget(new PlayerSuggestionBoxWidget(this.minecraft,
                i + DESCRIPTION_BOX_X, j + SUGGESTION_BOX_Y, SUGGESTION_BOX_W, SUGGESTION_BOX_H));

        this.recipient.setOutOfBoundResponder(up -> {
            if (!up) {
                this.setFocused(descriptionBox);
            }
        });

        this.descriptionBox = this.addRenderableWidget(new MultiLineEditBoxWidget(this.minecraft,
                i + DESCRIPTION_BOX_X, j + DESCRIPTION_BOX_Y, DESCRIPTION_BOX_W, DESCRIPTION_BOX_H));

        this.descriptionBox.setOutOfBoundResponder(up -> {
            if (up) {
                this.setFocused(recipient);
            }
        });

        this.setFocused(this.recipient);

        this.recipient.setText(this.tile.getRecipient());
        this.descriptionBox.setText(this.tile.getDescription());
        this.packed = tile.isPacked();

        this.updateState();

        this.menu.addSlotListener(this);
    }

    public void onAddPlayer(PlayerInfo info) {
        this.recipient.addPlayer(info);
    }

    public void onRemovePlayer(UUID uuid) {
        this.recipient.removePlayer(uuid);
    }

    private void pack() {
        this.updateStateAndTryToPack(true);
    }

    private void updateState() {
        this.updateStateAndTryToPack(false);
    }

    private void updateStateAndTryToPack(boolean tryToPack) {
        boolean hasItem = this.needsInitialization ? this.packed : this.menu.getSlot(0).hasItem();
        //pack
        boolean hasChanged = false;
        //truth table shit. idk, could be written more readable
        if (this.packed && !hasItem) {
            this.packed = false;
            hasChanged = true;
        } else if (tryToPack && !this.packed && hasItem) {
            this.packed = true;
            hasChanged = true;
        }

        if (hasChanged) {
            String sender = Minecraft.getInstance().player.getName().getString();
            String recipient = this.recipient.getText();
            String description = this.descriptionBox.getText();
            NetworkHelper.sendToServer(new ServerBoundSetPresentPacket(this.tile.getBlockPos(),
                    this.packed, recipient, sender, description));
            this.tile.updateState(this.packed, recipient, sender, description, Minecraft.getInstance().player);

            //close on client when packed. server side is handled by packet when it arrives
            if (this.packed) this.minecraft.player.clientSideCloseContainer();
        }

        this.recipient.setState(hasItem, this.packed);
        this.packButton.setState(hasItem, this.packed);
        this.descriptionBox.setState(hasItem, this.packed);
    }

    @Override
    public void slotChanged(AbstractContainerMenu container, int slot, ItemStack stack) {
        if (slot == 0) {
            updateState();
        }
    }

    @Override
    public void dataChanged(AbstractContainerMenu container, int dataSlotIndex, int value) {
        this.slotChanged(container, 0, container.getSlot(0).getItem());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        ResourceLocation presentGuiTexture = this.menu.getSlot(0).getItem().isEmpty() ?
                ModTextures.PRESENT_EMPTY_GUI_TEXTURE :
                ModTextures.PRESENT_GUI_TEXTURE;
        graphics.blit(presentGuiTexture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (this.packed) {
            int k = this.leftPos;
            int l = this.topPos;
            Slot slot = this.menu.getSlot(0);

            graphics.blitSprite(ModTextures.PRESENT_OVERLAY_SPRITE, k + slot.x, l + slot.y,  16, 16);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int key) {
        this.recipient.setFocused(false);
        this.descriptionBox.setFocused(false);
        return super.mouseClicked(mouseX, mouseY, key);
    }

    @Override
    public boolean keyPressed(int key, int a, int b) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
        }
        return this.recipient.keyPressed(key, a, b) || this.recipient.canConsumeInput() ||
                this.descriptionBox.keyPressed(key, a, b) || this.descriptionBox.canConsumeInput()
                || super.keyPressed(key, a, b);
    }

    @Override
    public boolean mouseDragged(double dx, double dy, int key, double mouseX, double mouseY) {
        if (key == 0) {
            if (this.descriptionBox.mouseDragged(dx, dy, key, mouseX, mouseY)) return true;
        }
        return super.mouseDragged(dx, dy, key, mouseX, mouseY);
    }

    @Override
    public void containerTick() {
        this.needsInitialization = false;
        super.containerTick();
        this.recipient.tick();
        this.descriptionBox.tick();
    }

    @Override
    public void removed() {
        super.removed();
        this.menu.removeSlotListener(this);
    }

    public class PackButton extends AbstractButton {
        private static final Tooltip TOOLTIP = Tooltip.create(Component.translatable("gui.supplementaries.present.pack"));

        private boolean packed;

        protected PackButton(int x, int y) {
            super(x, y, 22, 22, CommonComponents.EMPTY);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation texture;
            if (!this.active) {
                texture = ModTextures.PRESENT_BUTTON_SELECTED_SPRITE;
            } else if (this.packed) {
                texture = ModTextures.PRESENT_BUTTON_DISABLED_SPRITE;
            } else if (this.isHovered) {
                texture = ModTextures.PRESENT_BUTTON_HIGHLIGHTED_SPRITE;
            } else {
                texture = ModTextures.PRESENT_BUTTON_SPRITE;
            }
            graphics.blitSprite(texture, this.getX(),this.getY(), this.width, this.height);
        }

        public void setState(boolean hasItem, boolean packed) {
            this.packed = packed;
            this.active = hasItem;
            this.setTooltip(!packed ? TOOLTIP : null);
        }

        @Override
        public void onPress() {
            PresentScreen.this.pack();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }

}
