package net.mehvahdjukaar.supplementaries.integration.create;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.FlapDisplaySection;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.CreateLang;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mehvahdjukaar.supplementaries.common.block.tiles.ClockBlockTile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class ClockDisplaySource extends SingleLineDisplaySource {
    public static final MutableComponent EMPTY_TIME = Component.literal("--:--");

    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        Level level = context.level();

        if (level instanceof ServerLevel sLevel) {
            if (context.getSourceBlockEntity() instanceof ClockBlockTile tile) {
                boolean c12 = context.sourceConfig().getInt("Cycle") == 0;
                boolean isNatural = sLevel.dimensionType().natural();
                int dayTime = (int) (sLevel.getDayTime() % 24000L);
                int hours = (dayTime / 1000 + 6) % 24;
                int minutes = dayTime % 1000 * 60 / 1000;
                MutableComponent suffix = CreateLang.translateDirect("generic.daytime." + (hours > 11 ? "pm" : "am"));
                minutes = minutes / 5 * 5;
                if (c12) {
                    hours %= 12;
                    if (hours == 0) {
                        hours = 12;
                    }
                }
                if (!isNatural) {
                    hours = sLevel.random.nextInt(70) + 24;
                    minutes = sLevel.random.nextInt(40) + 60;
                }
                MutableComponent component = Component.literal(
                        (hours < 10 ? " " : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes + (c12 ? " " : "")
                );
                return c12 ? component.append(suffix) : component;
            }
        }
        return EMPTY_TIME;
    }

    @Override
    protected String getFlapDisplayLayoutName(DisplayLinkContext context) {
        return "Instant";
    }

    @Override
    protected FlapDisplaySection createSectionForValue(DisplayLinkContext context, int size) {
        return new FlapDisplaySection(size * 7.0F, "instant", false, false);
    }

    @Override
    protected String getTranslationKey() {
        return "time_of_day";
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
        super.initConfigurationWidgets(context, builder, isFirstLine);
        if (!isFirstLine) {
            builder.addSelectionScrollInput(
                    0,
                    60,
                    (si, l) -> si.forOptions(CreateLang.translatedOptions("display_source.time", "12_hour", "24_hour"))
                            .titled(CreateLang.translateDirect("display_source.time.format")),
                    "Cycle"
            );
        }
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }
}
