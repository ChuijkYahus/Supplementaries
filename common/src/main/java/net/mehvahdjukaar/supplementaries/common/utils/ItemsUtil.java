package net.mehvahdjukaar.supplementaries.common.utils;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.moonlight.api.item.additional_placements.AdditionalItemPlacement;
import net.mehvahdjukaar.moonlight.api.item.additional_placements.AdditionalItemPlacementsAPI;
import net.mehvahdjukaar.moonlight.api.item.additional_placements.BlockPlacerItem;
import net.mehvahdjukaar.supplementaries.SuppPlatformStuff;
import net.mehvahdjukaar.supplementaries.common.block.tiles.KeyLockableTile;
import net.mehvahdjukaar.supplementaries.common.events.overrides.InteractEventsHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemsUtil {

    //placeable item stuff

    @Nullable
    public static BlockState getPlacementState(BlockPlaceContext context, Block block) {
        return BlockPlacerItem.get().mimicGetPlacementState(context, block);
    }

    public static InteractionResult place(BlockPlaceContext context, Block blockToPlace) {
        return place(context, blockToPlace, null);
    }

    public static InteractionResult place(BlockPlaceContext context, Block blockToPlace, @Nullable SoundType placeSound) {
        return BlockPlacerItem.get().mimicPlace(context, blockToPlace, placeSound);
    }

    //helper for slingshot. Calls both block item and this in case it as additional behavior
    public static InteractionResult place(Item item, BlockPlaceContext pContext) {

        Player player = pContext.getPlayer();
        ItemStack stack = pContext.getItemInHand();
        Level level = pContext.getLevel();
        BlockHitResult hit = pContext.getHitResult();

        //block override. mimic forge event that would have called these
        //we cant call all forge events unfortunately otherwise we could alter the world as if player was there and not just place extra blocks
        InteractionResult overrideResult = InteractEventsHandler.onItemUsedOnBlockHP(player, level, stack, InteractionHand.MAIN_HAND, hit);
        if (overrideResult.consumesAction()) {
            return overrideResult;
        }
        overrideResult = InteractEventsHandler.onItemUsedOnBlock(player, level, stack, InteractionHand.MAIN_HAND, hit);
        if (overrideResult.consumesAction()) {
            return overrideResult;
        }

        //this also would be called from useOn. Since we aren't calling use on we must call it directly
        var placement = AdditionalItemPlacementsAPI.getBehavior(item);
        if (placement != null) return placement.overridePlace(pContext);
        if (item instanceof BlockItem bi) {
            return SuppPlatformStuff.placeBlockItem(bi,pContext);
        }
        return InteractionResult.PASS;
    }


    public static void addStackToExisting(Player player, ItemStack stack, boolean avoidHands) {
        var inv = player.getInventory();
        boolean added = false;
        for (int j = 0; j < inv.items.size(); j++) {
            if (inv.getItem(j).is(stack.getItem()) && inv.add(j, stack)) {
                added = true;
                break;
            }
        }
        if (avoidHands && !added) {
            for (int j = 0; j < inv.items.size(); j++) {
                if (inv.getItem(j).isEmpty() && j != inv.selected && inv.add(j, stack)) {
                    added = true;
                    break;
                }
            }
        }
        if (!added && inv.add(stack)) {
            player.drop(stack, false);
        }
    }

    public static boolean tryInteractingWithContainerItem(ItemStack containerStack, ItemStack incoming, Slot slot, ClickAction action, Player player, boolean inSlot) {
        if (action != ClickAction.PRIMARY) {
            //drop content in empty invSlot
            if (incoming.isEmpty()) {
                if (!inSlot) {
                    return ItemsUtil.extractFromContainerItemIntoSlot(player, containerStack, slot);
                }
            } else if (ItemsUtil.addToContainerItem(player, containerStack, incoming, slot, true, inSlot)) {
                return ItemsUtil.addToContainerItem(player, containerStack, incoming, slot, false, inSlot);
            }
        }
        return false;
    }


    @ExpectPlatform
    public static boolean addToContainerItem(Player player, ItemStack containerStack, ItemStack stack, Slot slot, boolean simulate, boolean inSlot) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean extractFromContainerItemIntoSlot(Player player, ItemStack containerStack, Slot slot) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static float getEncumbermentFromInventory(ItemStack stack, ServerPlayer player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static KeyLockableTile.KeyStatus getPlayerKeyStatus(Player player, String key) {
        throw new AssertionError();
    }


    //TODO: move to lib
    @ExpectPlatform
    public static ItemStack tryExtractingItem(Level level, @Nullable Direction dir, BlockPos pos, @Nullable BlockEntity container) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ItemStack tryAddingItem(ItemStack stack, Level level, Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ItemStack tryAddingItem(ItemStack stack, Level level, Direction direction,
                                          BlockPos pos, BlockState state, @Nullable BlockEntity tile) {
        throw new AssertionError();
    }

    private static final Component UNKNOWN_CONTENTS = Component.translatable("container.shulkerBox.unknownContents");

    public static void addShulkerLikeTooltips(ItemStack stack, List<Component> tooltip) {
        if (stack.has(DataComponents.CONTAINER_LOOT)) {
            tooltip.add(UNKNOWN_CONTENTS);
        }
        int i = 0;
        int j = 0;

        for (ItemStack itemStack : (stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)).nonEmptyItems()) {
            ++j;
            if (i <= 4) {
                ++i;
                tooltip.add(Component.translatable("container.shulkerBox.itemCount", itemStack.getHoverName(), itemStack.getCount()));
            }
        }

        if (j - i > 0) {
            tooltip.add(Component.translatable("container.shulkerBox.more", j - i).withStyle(ChatFormatting.ITALIC));
        }
    }
}
