package net.mehvahdjukaar.supplementaries.neoforge;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.mehvahdjukaar.supplementaries.configs.CommonConfigs;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.mehvahdjukaar.supplementaries.reg.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.ForgeHooks;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.minecraftforge.event.entity.item.ItemEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import org.jetbrains.annotations.NotNull;
import sfiomn.legendarysurvivaloverhaul.common.items.drink.CanteenItem;

import java.util.function.Supplier;

public class ReplaceRopeByConfigModifier extends LootModifier {
    public static final Supplier<MapCodec<ReplaceRopeByConfigModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, ReplaceRopeByConfigModifier::new)));

    protected ReplaceRopeByConfigModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        var ropeConfig = CommonConfigs.Functional.ROPE_REPLACE_LOOT_TABLES.get();
        if (ropeConfig == CommonConfigs.ReplaceTableMode.NONE) return generatedLoot;
        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            if (stack.is(ModTags.ROPES) && !stack.is(ModRegistry.ROPE_ITEM.get())) {
                if (ropeConfig == CommonConfigs.ReplaceTableMode.REPLACE) {
                    generatedLoot.set(i, new ItemStack(ModRegistry.ROPE_ITEM.get(), stack.getCount()));
                } else if (ropeConfig == CommonConfigs.ReplaceTableMode.REMOVE) {
                    generatedLoot.add(ItemStack.EMPTY);
                }
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}

