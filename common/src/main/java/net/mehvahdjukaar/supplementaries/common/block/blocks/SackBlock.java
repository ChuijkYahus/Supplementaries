package net.mehvahdjukaar.supplementaries.common.block.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.moonlight.api.entity.ImprovedFallingBlockEntity;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.supplementaries.common.block.IRopeConnection;
import net.mehvahdjukaar.supplementaries.common.block.tiles.SackBlockTile;
import net.mehvahdjukaar.supplementaries.common.utils.BlockUtil;
import net.mehvahdjukaar.supplementaries.reg.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SackBlock extends ColoredFallingBlock implements EntityBlock, SimpleWaterloggedBlock {
    public static final MapCodec<SackBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(ColorRGBA.CODEC.fieldOf("falling_dust_color").forGetter(coloredFallingBlock -> coloredFallingBlock.dustColor), propertiesCodec())
                    .apply(instance, SackBlock::new)
    );

    public static final VoxelShape SHAPE_CLOSED = Shapes.or(Block.box(2, 0, 2, 14, 12, 14),
            Block.box(6, 12, 6, 10, 13, 10), Block.box(5, 13, 5, 11, 16, 11));
    public static final VoxelShape SHAPE_OPEN = Shapes.or(Block.box(2, 0, 2, 14, 12, 14),
            Block.box(6, 12, 6, 10, 13, 10), Block.box(3, 13, 3, 13, 14, 13));


    public static final ResourceLocation CONTENTS = ResourceLocation.withDefaultNamespace("contents");
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public SackBlock(ColorRGBA color, Properties properties) {
        super(color, properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, false).setValue(WATERLOGGED, false));
    }

    @SuppressWarnings("all")
    @Override
    public MapCodec<ColoredFallingBlock> codec() {
        return (MapCodec) CODEC;
    }

    //falling block
    @Override
    public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (state.getBlock() != oldState.getBlock()) {
            worldIn.scheduleTick(pos, this, this.getDelayAfterPlace());
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN, WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (stateIn.getValue(WATERLOGGED)) {
            worldIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean flag = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(WATERLOGGED, flag);
    }

    public static boolean canFall(BlockPos pos, LevelAccessor world) {
        return (world.isEmptyBlock(pos.below()) || isFree(world.getBlockState(pos.below()))) &&
                pos.getY() >= world.getMinBuildHeight() && !IRopeConnection.isSupportingCeiling(pos.above(), world);
    }

    //schedule block tick
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        if (level.getBlockEntity(pos) instanceof SackBlockTile tile) {
            tile.recheckOpen();
            if (canFall(pos, level)) {
                ImprovedFallingBlockEntity entity = ImprovedFallingBlockEntity.fall(ModEntities.FALLING_SACK.get(),
                        level, pos, state, true);
                entity.blockData = tile.saveWithFullMetadata(level.registryAccess());
                float power = this.getAnalogOutputSignal(state, level, pos) / 15f;
                entity.setHurtsEntities(1 + power * 5, 40);
            }
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new SackBlockTile(pPos, pState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (player.isSpectator()) {
            return InteractionResult.CONSUME;
        } else {
            if (level.getBlockEntity(pos) instanceof SackBlockTile tile && player instanceof ServerPlayer sp) {
                TileOrEntityTarget target = TileOrEntityTarget.of(tile);
                PlatHelper.openCustomMenu(sp, tile, p -> {
                    target.write(p);
                    p.writeInt(tile.getContainerSize());
                });
                PiglinAi.angerNearbyPiglins(player, true);

                return InteractionResult.CONSUME;
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    //for creative drop
    @Override
    public BlockState playerWillDestroy(Level worldIn, BlockPos pos, BlockState state, Player player) {
        if (worldIn.getBlockEntity(pos) instanceof SackBlockTile tile) {
            Utils.spawnItemWithTileData(player, tile);
        }
        return super.playerWillDestroy(worldIn, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof SackBlockTile tile) {
            builder = builder.withDynamicDrop(CONTENTS, (context) -> {
                for (int i = 0; i < tile.getContainerSize(); ++i) {
                    context.accept(tile.getItem(i));
                }
            });
        }
        return super.getDrops(state, builder);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof SackBlockTile tile) {
            Utils.saveTileToItem(tile);
        }
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPEN))
            return SHAPE_OPEN;
        return SHAPE_CLOSED;
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            worldIn.updateNeighbourForOutputSignal(pos, state.getBlock());
            super.onRemove(state, worldIn, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
        if (worldIn.getBlockEntity(pos) instanceof Container tile) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(tile);
        }
        return 0;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
        return worldIn.getBlockEntity(pos) instanceof MenuProvider menuProvider ? menuProvider : null;
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState state1, FallingBlockEntity blockEntity) {
        super.onLand(level, pos, state, state1, blockEntity);
        //land sound
        if (!blockEntity.isSilent()) {
            level.playSound(null, pos, state.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }
        level.scheduleTick(pos, this, this.getDelayAfterPlace());
    }

}
