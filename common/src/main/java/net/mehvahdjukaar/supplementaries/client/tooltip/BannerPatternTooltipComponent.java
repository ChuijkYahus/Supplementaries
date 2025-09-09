package net.mehvahdjukaar.supplementaries.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.moonlight.api.client.util.RenderUtil;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.supplementaries.common.items.tooltip_components.BannerPatternTooltip;
import net.mehvahdjukaar.supplementaries.common.utils.ItemsUtil;
import net.mehvahdjukaar.supplementaries.configs.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.registries.Registries;

import java.util.Optional;

public class BannerPatternTooltipComponent implements ClientTooltipComponent {

    private final int size = ClientConfigs.Tweaks.TOOLTIP_IMAGE_SIZE.get();
    private final Optional<Material> material;

    public BannerPatternTooltipComponent(BannerPatternTooltip tooltip) {
        var reg = Minecraft.getInstance().level.registryAccess().registryOrThrow(
                Registries.BANNER_PATTERN
        );
        this.material = reg.getTag(tooltip.pattern())
                .flatMap(n -> n.stream().findAny())
                .map(Sheets::getBannerMaterial);
    }

    @Override
    public int getHeight() {
        return size + 2;
    }

    @Override
    public int getWidth(Font pFont) {
        return size;
    }

    @Override
    public void renderImage(Font pFont, int x, int y, GuiGraphics graphics) {
        if (material.isPresent()) {
            graphics.pose().pushPose();
            var sprite = material.get().sprite();
            RenderSystem.enableBlend();
            var contents = sprite.contents();

            int width = contents.width();
            int height = contents.height();
             
            RenderUtil.blitSpriteSection(graphics, x, y, size, size,
                    1f / width, (1f / height) * 12, (int) (20f / 64 * width), (int) (20f / 64 * height), sprite);

            graphics.pose().popPose();
        }
    }
}