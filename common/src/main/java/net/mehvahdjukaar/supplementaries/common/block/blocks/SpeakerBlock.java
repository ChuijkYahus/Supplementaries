package net.mehvahdjukaar.supplementaries.common.block.blocks;

import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.supplementaries.common.block.ModBlockProperties;
import net.mehvahdjukaar.supplementaries.common.block.tiles.SpeakerBlockTile;
import net.mehvahdjukaar.supplementaries.reg.ModParticles;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SpeakerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ANTIQUE = ModBlockProperties.ANTIQUE;

    private static final int POWERED_TICKS = 40;

    public SpeakerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(ANTIQUE, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, ANTIQUE);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        this.updatePower(state, worldIn, pos);
        if (worldIn.getBlockEntity(pos) instanceof SpeakerBlockTile tile) {
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                //TODO: check if its needed
                tile.setCustomName(stack.getHoverName());
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, moving);
        if (!level.isClientSide) {
            boolean hasPower = state.getValue(POWERED);
            if (hasPower != level.hasNeighborSignal(pos)) {
                if (hasPower) {
                    level.scheduleTick(pos, this, POWERED_TICKS);
                } else {
                    //turn on immediately
                    this.updatePower(state, level, pos);
                }
            }
        }
    }


    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        //turn off
        this.updatePower(state, level, pos);
    }

    public void updatePower(BlockState state, Level world, BlockPos pos) {
        if (!world.isClientSide()) {
            boolean pow = world.hasNeighborSignal(pos);
            // state changed
            if (pow != state.getValue(POWERED)) {
                //known shape
                world.setBlock(pos, state.setValue(POWERED, pow), 2);
                // can I emit sound?
                Direction facing = state.getValue(FACING);
                if (pow && world.isEmptyBlock(pos.relative(facing))) {
                    if (world.getBlockEntity(pos) instanceof SpeakerBlockTile tile) {
                        tile.sendMessage();
                        world.gameEvent(null, GameEvent.INSTRUMENT_PLAY, pos);
                    }
                }
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof SpeakerBlockTile tile && tile.isAccessibleBy(player)) {
            //ink
            if (!state.getValue(ANTIQUE) && Utils.mayPerformBlockAction(player, pos, stack)) {
                if (stack.is(ModRegistry.ANTIQUE_INK.get())) {
                    level.playSound(null, pos, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.setBlockAndUpdate(pos, state.setValue(ANTIQUE, true));
                    level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, level.getBlockState(pos)));

                    stack.consume(1, player);
                    if (player instanceof ServerPlayer serverPlayer) {
                        CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
                        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            if (player instanceof ServerPlayer serverPlayer) {
                tile.tryOpeningEditGui(serverPlayer, pos, stack);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new SpeakerBlockTile(pPos, pState);
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
        if (eventID == 0) {
            Direction facing = state.getValue(FACING);
            world.addParticle(ModParticles.SPEAKER_SOUND.get(), pos.getX() + 0.5 + facing.getStepX() * 0.725, pos.getY() + 0.5,
                    pos.getZ() + 0.5 + facing.getStepZ() * 0.725, world.random.nextInt(24) / 24.0D, 0.0D, 0.0D);
            return true;
        }
        return super.triggerEvent(state, world, pos, eventID, eventParam);
    }

}