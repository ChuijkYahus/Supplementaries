package net.mehvahdjukaar.supplementaries.integration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.supplementaries.common.block.IKeyLockable;
import net.mehvahdjukaar.supplementaries.common.block.tiles.KeyLockableTile;
import net.mehvahdjukaar.supplementaries.common.items.KeyItem;
import net.mehvahdjukaar.supplementaries.common.utils.SlotReference;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.mehvahdjukaar.supplementaries.reg.ModTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;

import java.util.List;

public class CuriosCompat {

    public static void init(){
    }

    static KeyLockableTile.KeyStatus getKey(Player player, String password) {
        List<SlotResult> found = CuriosApi.getCuriosHelper().findCurios(player, i ->
                i.is(ModTags.KEYS) || i.getItem() instanceof KeyItem);
        if (found.isEmpty()) return KeyLockableTile.KeyStatus.NO_KEY;
        for (var slot : found) {
            ItemStack stack = slot.stack();
            if (IKeyLockable.getKeyStatus(stack, password).isCorrect()) {
                return KeyLockableTile.KeyStatus.CORRECT_KEY;
            }
        }
        return KeyLockableTile.KeyStatus.INCORRECT_KEY;
    }

    static SlotReference getQuiver(Player player) {
        ICuriosHelper curiosHelper = CuriosApi.getCuriosHelper();
        List<SlotResult> found = curiosHelper.findCurios(player, i -> i.is(ModRegistry.QUIVER_ITEM.get()));
        if (!found.isEmpty()) {
            var context = found.get(0).slotContext();
            return new Curio(context.identifier(), context.index());
        }
        return SlotReference.EMPTY;
    }

    public record Curio(String id, int index) implements SlotReference {

        public static final Codec<Curio> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.STRING.fieldOf("id").forGetter(Curio::id),
                        Codec.INT.fieldOf("index").forGetter(Curio::index)
                ).apply(instance, Curio::new)
        );

        @Override
        public ItemStack get(LivingEntity player) {
            ICuriosHelper curiosHelper = CuriosApi.getCuriosHelper();
            return curiosHelper.findCurio(player, id, index)
                    .map(SlotResult::stack).orElse(ItemStack.EMPTY);
        }

        @Override
        public Codec<? extends SlotReference> getCodec() {
            return CODEC;
        }
    }

    static {
        SlotReference.REGISTRY.register("curio", Curio.CODEC);
    }
}
