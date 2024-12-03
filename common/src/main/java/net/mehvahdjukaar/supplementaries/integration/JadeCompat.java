package net.mehvahdjukaar.supplementaries.integration;

import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.mehvahdjukaar.supplementaries.common.block.tiles.AbstractPresentBlockTile;
import net.mehvahdjukaar.supplementaries.common.block.tiles.SafeBlockTile;
import net.mehvahdjukaar.supplementaries.common.entities.HatStandEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.jetbrains.annotations.Nullable;
import snownee.jade.addon.universal.ItemStorageProvider;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.view.*;

import java.util.List;

@WailaPlugin
public class JadeCompat implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerItemStorage(new HideItemsProvider<>(Supplementaries.res("present")), AbstractPresentBlockTile.class);
        registration.registerItemStorage(new HideItemsProvider<>(Supplementaries.res("safe")), SafeBlockTile.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerItemStorageClient(new HideItemsProvider<>(Supplementaries.res("present")));
        registration.registerItemStorageClient(new HideItemsProvider<>(Supplementaries.res("safe")));
        registration.registerEntityComponent(new HatStandProvider(Supplementaries.res("hat_stand")), HatStandEntity.class);
    }

    public record HideItemsProvider<T extends BaseContainerBlockEntity>(ResourceLocation id) implements
            IServerExtensionProvider<ItemStack>, IClientExtensionProvider<ItemStack, ItemView> {

        @Override
        public ResourceLocation getUid() {
            return id;
        }

        @Override
        public @Nullable List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
            var te = accessor.getTarget();
            if (te instanceof SafeBlockTile || te instanceof AbstractPresentBlockTile) {
                Player player = accessor.getPlayer();
                RandomizableContainerBlockEntity tile = (RandomizableContainerBlockEntity) te;
                if (tile.canOpen(player)) {
                    return ItemStorageProvider.Extension.INSTANCE.getGroups(accessor);
                }
            }
            return List.of();
        }

        @Override
        public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
            return ClientViewGroup.map(groups, ItemView::new, null);
        }


    }

    public record HatStandProvider(ResourceLocation id) implements IEntityComponentProvider {

        @Override
        public ResourceLocation getUid() {
            return id;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            HatStandEntity entity = (HatStandEntity) accessor.getEntity();

            for (ItemStack stack : entity.getArmorSlots()) {
                if (!stack.isEmpty()) {
                    tooltip.add(IElementHelper.get().smallItem(stack));
                    tooltip.append(IDisplayHelper.get().stripColor(stack.getHoverName()));
                }
            }

        }

    }

}
