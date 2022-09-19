package net.mehvahdjukaar.supplementaries.common.items.forge;

import net.mehvahdjukaar.supplementaries.common.entities.IQuiverEntity;
import net.mehvahdjukaar.supplementaries.common.items.QuiverItem;
import net.mehvahdjukaar.supplementaries.configs.CommonConfigs;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class QuiverItemImpl {

    public static ItemStack getQuiver(LivingEntity entity) {
        if (!(entity instanceof Player) && entity instanceof IQuiverEntity e) return e.getQuiver();
        var cap = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (cap != null) {
            for (int i = 0; i < cap.getSlots(); i++) {
                ItemStack quiver = cap.getStackInSlot(i);
                if (quiver.getItem() == ModRegistry.QUIVER_ITEM.get()) return quiver;
            }
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    public static QuiverItem.IQuiverData getQuiverData(ItemStack stack) {
        return (QuiverCapability) stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
    }

    public static class QuiverCapability extends ItemStackHandler implements ICapabilitySerializable<CompoundTag>, QuiverItem.IQuiverData {

        private final LazyOptional<IItemHandler> lazyOptional = LazyOptional.of(() -> this);

        //Provider
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, Direction side) {
            return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, lazyOptional);
        }

        @Override
        public CompoundTag serializeNBT() {
            var c = super.serializeNBT();
            c.putInt("SelectedSlot", this.selectedSlot);
            return c;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            super.deserializeNBT(nbt);
            this.selectedSlot = nbt.getByte("SelectedSlot");
        }

        //actual cap

        private int selectedSlot = 0;

        public QuiverCapability() {
            super(CommonConfigs.Items.QUIVER_SLOTS.get());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return this.canAcceptItem(stack);
        }

        public List<ItemStack> getContentView() {
            return this.stacks;
        }

        @Override
        public int getSelectedSlot() {
            return selectedSlot;
        }

        public void setSelectedSlot(int selectedSlot) {
            if (!stacks.get(selectedSlot).isEmpty()) {
                this.selectedSlot = selectedSlot;
            }
        }

        public boolean cycle(int slotsMoved) {
            int originalSlot = this.selectedSlot;
            int maxSlots = this.stacks.size();
            slotsMoved = slotsMoved % maxSlots;
            this.selectedSlot = (maxSlots + (this.selectedSlot + slotsMoved)) % maxSlots;
            for (int i = 0; i < maxSlots; i++) {
                var stack = this.getStackInSlot(selectedSlot);
                if (!stack.isEmpty()) break;
                this.selectedSlot = (maxSlots + (this.selectedSlot + (slotsMoved >= 0 ? 1 : -1))) % maxSlots;
            }
            return originalSlot != selectedSlot;
        }

        public ItemStack tryAdding(ItemStack toInsert) {
            if (!toInsert.isEmpty() && toInsert.getItem().canFitInsideContainerItems()) {
                return ItemHandlerHelper.insertItem(this, toInsert, false);
            }
            return ItemStack.EMPTY;
        }

        public Optional<ItemStack> removeOneStack() {
            int i = 0;
            for (var s : this.getContentView()) {
                if (!s.isEmpty()) {
                    var extracted = this.extractItem(i, s.getCount(), false);
                    this.updateSelectedIfNeeded();
                    return Optional.of(extracted);
                }
                i++;
            }
            return Optional.empty();
        }

        @Override
        public void consumeArrow() {
            var s = this.getSelected();
            s.shrink(1);
            if (s.isEmpty()) this.stacks.set(this.selectedSlot, ItemStack.EMPTY);
            this.updateSelectedIfNeeded();
            //not implemented because it isn't needed
        }
    }
}