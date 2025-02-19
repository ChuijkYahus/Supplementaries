package net.mehvahdjukaar.supplementaries.common.items.crafting;

import com.google.gson.JsonObject;
import net.mehvahdjukaar.supplementaries.common.block.tiles.SusGravelBricksTile;
import net.mehvahdjukaar.supplementaries.reg.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class SusRecipe extends CustomRecipe {
    private final Ingredient ingredient;
    private final ItemStack result;


    public SusRecipe(ResourceLocation recipeId, CraftingBookCategory craftingBookCategory, Ingredient ingredient, ItemStack itemStack) {
        super(recipeId, craftingBookCategory);
        this.ingredient = ingredient;
        this.result = itemStack;
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        ItemStack gravel = null;
        ItemStack something = null;

        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (gravel == null && ingredient.test(stack)) {
                gravel = stack;
            } else if (!stack.isEmpty()) {
                if (something != null) {
                    return false;
                }
                something = stack;
            }
        }
        return gravel != null && something != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        ItemStack gravel = null;
        ItemStack something = null;

        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (gravel == null && ingredient.test(stack)) {
                gravel = stack;
            } else if (!stack.isEmpty()) {
                something = stack;
            }
        }
        ItemStack result = this.result.copyWithCount(1);
        result.getOrCreateTagElement("BlockEntityTag")
                .put("item", something.copyWithCount(1).save(new CompoundTag()));

        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        return NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SUS_CRAFTING.get();
    }

    public static class Serializer implements RecipeSerializer<SusRecipe> {


        @Override
        public SusRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient ingredient;
            if (GsonHelper.isArrayNode(json, "ingredient")) {
                ingredient = Ingredient.fromJson(GsonHelper.getAsJsonArray(json, "ingredient"), false);
            } else {
                ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"), false);
            }
            ItemStack itemStack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            CraftingBookCategory craftingBookCategory = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", (String) null), CraftingBookCategory.MISC);

            return new SusRecipe(recipeId, craftingBookCategory, ingredient, itemStack);
        }

        @Override
        public SusRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            CraftingBookCategory craftingBookCategory = buffer.readEnum(CraftingBookCategory.class);
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ItemStack itemStack = buffer.readItem();
            return new SusRecipe(recipeId, craftingBookCategory, ingredient, itemStack);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SusRecipe recipe) {
            buffer.writeEnum(recipe.category());
            recipe.ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
        }
    }
}
