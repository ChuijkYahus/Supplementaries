package net.mehvahdjukaar.supplementaries.common.items.crafting;

import net.mehvahdjukaar.supplementaries.common.items.RopeArrowItem;
import net.mehvahdjukaar.supplementaries.reg.ModRecipes;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class RopeArrowAddRecipe extends CustomRecipe {
    public RopeArrowAddRecipe(ResourceLocation idIn, CraftingBookCategory category) {
        super(idIn, category);
    }


    @Override
    public boolean matches(CraftingContainer inv, Level worldIn) {

        ItemStack arrow = null;
        ItemStack rope = null;
        int missingRopes = 0;

        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == ModRegistry.ROPE_ARROW_ITEM.get() && stack.getDamageValue() != 0) {
                if (arrow != null) {
                    return false;
                }
                arrow = stack;
                missingRopes += arrow.getDamageValue();
            } else if (RopeArrowItem.isValidRope(stack)) {
                rope = stack;
                missingRopes--;
            } else if (!stack.isEmpty()) return false;
        }
        return arrow != null && rope != null && missingRopes >= 0;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        int ropes = 0;
        ItemStack arrow = null;
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (RopeArrowItem.isValidRope(stack)) {
                ropes++;
            }
            if (stack.getItem() == ModRegistry.ROPE_ARROW_ITEM.get()) {
                arrow = stack;
            }
        }
        ItemStack returnArrow = arrow.copy();
        RopeArrowItem.addRopes(returnArrow, ropes);
        return returnArrow;

    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ROPE_ARROW_ADD.get();
    }


}
