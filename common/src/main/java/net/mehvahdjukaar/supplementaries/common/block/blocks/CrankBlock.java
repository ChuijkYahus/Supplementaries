package net.mehvahdjukaar.supplementaries.common.block.blocks;


import net.mehvahdjukaar.moonlight.api.block.WaterBlock;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.mehvahdjukaar.supplementaries.reg.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class CrankBlock extends WaterBlock {
    protected static final VoxelShape SHAPE_DOWN = Block.box(2, 11, 2, 14, 16, 14);
    protected static final VoxelShape SHAPE_UP = Block.box(2, 0, 2, 14, 5, 14);
    protected static final VoxelShape SHAPE_NORTH = Block.box(2, 2, 11, 14, 14, 16);
    protected static final VoxelShape SHAPE_SOUTH = Block.box(2, 2, 0, 14, 14, 5);
    protected static final VoxelShape SHAPE_EAST = Block.box(0, 2, 2, 5, 14, 14);
    protected static final VoxelShape SHAPE_WEST = Block.box(11, 2, 2, 16, 14, 14);

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public CrankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false).setValue(POWER, 0).setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos,
                                  BlockPos facingPos) {
        if (stateIn.getValue(WATERLOGGED)) {
            worldIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        }
        return facing.getOpposite() == stateIn.getValue(FACING) && !stateIn.canSurvive(worldIn, currentPos)
                ? Blocks.AIR.defaultBlockState()
                : stateIn;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos blockpos = pos.relative(direction.getOpposite());
        BlockState blockstate = worldIn.getBlockState(blockpos);
        if (direction == Direction.UP || direction == Direction.DOWN) {
            return canSupportCenter(worldIn, blockpos, direction);
        } else {
            return blockstate.isFaceSturdy(worldIn, blockpos, direction);
        }
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id == 0) {
            if (level.isClientSide) addParticle(state, level, pos, ParticleTypes.SMOKE);
            return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            //send particles
            level.blockEvent(pos, this, 0, 0);

            boolean ccw = player.isShiftKeyDown();
            this.turn(state, level, pos, ccw, player);

            Direction dir = state.getValue(FACING).getOpposite();
            if (dir.getAxis() != Direction.Axis.Y) {
                BlockPos behind = pos.relative(dir);
                BlockState backState = level.getBlockState(behind);
                if (backState.is(ModRegistry.PULLEY_BLOCK.get()) && dir.getAxis() == backState.getValue(PulleyBlock.AXIS)) {
                    ((PulleyBlock) backState.getBlock()).windPulley(backState, behind, level, ccw ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90, dir);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (explosion.canTriggerBlocks()) {
            this.turn(state, level, pos, true, null);
        }
        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }

    public void turn(BlockState state, Level level, BlockPos pos, boolean ccw, @Nullable Player player) {
        int newPower = (16 + state.getValue(POWER) + (ccw ? -1 : 1)) % 16;
        state = state.setValue(POWER, newPower);
        level.setBlock(pos, state, 3);
        this.updateNeighbors(state, level, pos);
        float f = 0.55f + state.getValue(POWER) * 0.04f; //(ccw ? 0.6f : 0.7f)+ MthUtils.nextWeighted(level.random, 0.04f)
        level.playSound(null, pos, ModSounds.CRANK.get(), SoundSource.BLOCKS, 0.5F, f);
        level.gameEvent(player, newPower == 0 ? GameEvent.BLOCK_DEACTIVATE : GameEvent.BLOCK_ACTIVATE, pos);
    }

    private void updateNeighbors(BlockState state, Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), this);
    }

    @Override
    public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getValue(POWER);
    }

    @Override
    public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getValue(FACING) == side ? blockState.getValue(POWER) : 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }


    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            if (state.getValue(POWER) != 0) {
                this.updateNeighbors(state, worldIn, pos);
            }
            super.onRemove(state, worldIn, pos, newState, isMoving);
        }
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
        if (stateIn.getValue(POWER) > 0 && rand.nextFloat() < 0.25F) {
            addParticle(stateIn, worldIn, pos, new DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, 0.5f));
        }
    }

    private static void addParticle(BlockState stateIn, Level worldIn, BlockPos pos,
                                    ParticleOptions particle) {
        Direction direction = stateIn.getValue(FACING).getOpposite();
        double x = pos.getX() + 0.5D + 0.1D * direction.getStepX() + 0.2D * direction.getStepX();
        double y = pos.getY() + 0.5D + 0.1D * direction.getStepY() + 0.2D * direction.getStepY();
        double z = pos.getZ() + 0.5D + 0.1D * direction.getStepZ() + 0.2D * direction.getStepZ();
        worldIn.addParticle(particle, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            default -> SHAPE_SOUTH;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER, WATERLOGGED);
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
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean flag = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        BlockState blockstate = this.defaultBlockState();
        LevelReader level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        Direction[] directions = context.getNearestLookingDirections();

        for (Direction direction : directions) {

            Direction direction1 = direction.getOpposite();
            blockstate = blockstate.setValue(FACING, direction1);
            if (blockstate.canSurvive(level, blockpos)) {
                return blockstate.setValue(WATERLOGGED, flag);
            }

        }
        return null;
    }
}
