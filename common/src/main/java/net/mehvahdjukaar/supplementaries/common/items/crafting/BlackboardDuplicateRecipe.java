package net.mehvahdjukaar.supplementaries.common.items.crafting;

import net.mehvahdjukaar.moonlight.api.platform.ForgeHelper;
import net.mehvahdjukaar.moonlight.core.network.ClientBoundOpenScreenPacket;
import net.mehvahdjukaar.supplementaries.reg.ModComponents;
import net.mehvahdjukaar.supplementaries.reg.ModRecipes;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class BlackboardDuplicateRecipe extends CustomRecipe {
    public BlackboardDuplicateRecipe(CraftingBookCategory category) {
        super(category);
    }

    private boolean isDrawnBlackboard(ItemStack stack) {
        return stack.has(ModComponents.BLACKBOARD.get());
    }

    @Override
    public boolean matches(CraftingInput inv, Level worldIn) {

        ItemStack itemstack = null;
        ItemStack itemstack1 = null;
        for (int i = 0; i < inv.size(); ++i) {
            ItemStack stack = inv.getItem(i);
            Item item = stack.getItem();
            if (item == ModRegistry.BLACKBOARD_ITEM.get()) {

                if (isDrawnBlackboard(stack)) {
                    if (itemstack != null) {
                        return false;
                    }

                    itemstack = stack;
                } else {
                    if (itemstack1 != null) {
                        return false;
                    }

                    itemstack1 = stack;
                }
            } else if (!stack.isEmpty()) return false;
        }

        return itemstack != null && itemstack1 != null;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider provider) {
        for (int i = 0; i < inv.size(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (isDrawnBlackboard(stack)) {
                ItemStack s = stack.copy();
                s.setCount(1);
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < stacks.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            if (!itemstack.isEmpty()) {
                Optional<ItemStack> container = ForgeHelper.getCraftingRemainingItem(itemstack);
                if (container.isPresent()) {
                    stacks.set(i, container.get());
                } else if (isDrawnBlackboard(itemstack)) {
                    ItemStack copy = itemstack.copy();
                    copy.setCount(1);
                    stacks.set(i, copy);
                }
            }
        }
        return stacks;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BLACKBOARD_DUPLICATE.get();
    }


}
