package net.mehvahdjukaar.supplementaries.common.items;


import net.mehvahdjukaar.supplementaries.common.items.components.BlackboardData;
import net.mehvahdjukaar.supplementaries.reg.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

public class BlackboardItem extends BlockItem {
    public BlackboardItem(Block blockIn, Properties builder) {
        super(blockIn, builder);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        BlackboardData data = stack.get(ModComponents.BLACKBOARD.get());
        if (data != null) {
            data.addToTooltip(context, tooltipComponents::add, tooltipFlag);
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack pStack) {
        BlackboardData data = pStack.get(ModComponents.BLACKBOARD.get());
        if (data != null && !data.isEmpty()) {
            return Optional.of(data);
        }
        return Optional.empty();
    }

}
