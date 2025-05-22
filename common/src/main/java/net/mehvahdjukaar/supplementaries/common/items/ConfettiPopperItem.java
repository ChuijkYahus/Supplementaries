package net.mehvahdjukaar.supplementaries.common.items;

import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.supplementaries.common.entities.IPartyCreeper;
import net.mehvahdjukaar.supplementaries.common.network.ClientBoundParticlePacket;
import net.mehvahdjukaar.supplementaries.common.network.ClientReceivers;
import net.mehvahdjukaar.supplementaries.common.utils.BlockPredicate;
import net.mehvahdjukaar.supplementaries.common.utils.VibeChecker;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfettiPopperItem extends BlockItem {

    public ConfettiPopperItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        return super.canPlace(context, state) && false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, InteractionHand hand) {
        VibeChecker.assertSameLevel(level, player);
        //no clue why im doing this from server side
        Vec3 pos = player.getEyePosition().add(player.getLookAngle().scale(0.2)).add(0d, -0.25, 0d);
        //hack
        float oldRot = player.getXRot();
        player.setXRot((float) (oldRot - 20 * Math.cos(oldRot * Mth.DEG_TO_RAD)));
        ClientBoundParticlePacket packet = new ClientBoundParticlePacket(pos, ClientBoundParticlePacket.Kind.CONFETTI,
                null, player.getLookAngle());
        player.setXRot(oldRot);
        if (!level.isClientSide) {
            NetworkHelper.sendToAllClientPlayersTrackingEntity(player, packet);

            level.gameEvent(player, GameEvent.EXPLODE, player.position());
        } else {
            //play immediately for client
            ClientReceivers.spawnConfettiParticles(packet, level, level.random);
        }

        ItemStack heldItem = player.getItemInHand(hand);
        heldItem.consume(1, player);
        return InteractionResultHolder.sidedSuccess(heldItem, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand usedHand) {
        if (entity.getType() == EntityType.CREEPER && entity instanceof Creeper c &&
                entity instanceof IPartyCreeper pc &&
                !c.isIgnited() && !pc.supplementaries$isFestive()) {
            pc.supplementaries$setFestive(true);
            stack.consume(1, player);
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactLivingEntity(stack, player, entity, usedHand);
    }

    @ForgeOverride
    public @Nullable EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.HEAD;
    }

}
