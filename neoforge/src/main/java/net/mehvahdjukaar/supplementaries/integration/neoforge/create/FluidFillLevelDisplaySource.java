package net.mehvahdjukaar.supplementaries.integration.neoforge.create;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.PercentOrProgressBarDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.CreateLang;
import net.mehvahdjukaar.moonlight.api.block.ISoftFluidTankProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class FluidFillLevelDisplaySource extends PercentOrProgressBarDisplaySource {

    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (context.sourceConfig().getInt("Mode") == 2) {
            if (context.getSourceBlockEntity() instanceof ISoftFluidTankProvider tp) {
                return Component.literal(tp.getSoftFluidTank().getFluidCount() + " mBtl");
            }
        }
        return super.provideLine(context, stats);
    }

    @Override
    protected Float getProgress(DisplayLinkContext context) {
        BlockEntity te = context.getSourceBlockEntity();
        if (te instanceof ISoftFluidTankProvider tp) {
            return tp.getSoftFluidTank().getHeight(1);
        }
        return null;
    }

    @Override
    protected boolean progressBarActive(DisplayLinkContext context) {
        return context.sourceConfig().getInt("Mode") == 1;
    }

    @Override
    protected String getTranslationKey() {
        return "fluid_amount";
    }

    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
        super.initConfigurationWidgets(context, builder, isFirstLine);
        if (!isFirstLine) {
            builder.addSelectionScrollInput(
                    0,
                    120,
                    (si, l) -> si.forOptions(CreateLang.translatedOptions("display_source.fill_level", "percent", "progress_bar", "fluid_amount"))
                            .titled(CreateLang.translateDirect("display_source.fill_level.display")),
                    "Mode"
            );
        }
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }
}
