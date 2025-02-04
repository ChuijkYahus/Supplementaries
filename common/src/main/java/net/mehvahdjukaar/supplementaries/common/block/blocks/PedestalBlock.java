package net.mehvahdjukaar.supplementaries.common.block.blocks;

import net.mehvahdjukaar.moonlight.api.block.ItemDisplayTile;
import net.mehvahdjukaar.moonlight.api.block.WaterBlock;
import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.mehvahdjukaar.supplementaries.common.block.ModBlockProperties;
import net.mehvahdjukaar.supplementaries.common.block.ModBlockProperties.DisplayStatus;
import net.mehvahdjukaar.supplementaries.common.block.tiles.PedestalBlockTile;
import net.mehvahdjukaar.supplementaries.common.items.SackItem;
import net.mehvahdjukaar.supplementaries.common.utils.BlockUtil;
import net.mehvahdjukaar.supplementaries.configs.CommonConfigs;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PedestalBlock extends WaterBlock implements EntityBlock, WorldlyContainerHolder {
    protected static final VoxelShape SHAPE = Shapes.or(Shapes.box(0.1875D, 0.125D, 0.1875D, 0.815D, 0.885D, 0.815D),
            Shapes.box(0.0625D, 0.8125D, 0.0625D, 0.9375D, 1D, 0.9375D),
            Shapes.box(0.0625D, 0D, 0.0625D, 0.9375D, 0.1875D, 0.9375D));
    protected static final VoxelShape SHAPE_UP = Shapes.or(Shapes.box(0.1875D, 0.125D, 0.1875D, 0.815D, 1, 0.815D),
            Shapes.box(0.0625D, 0D, 0.0625D, 0.9375D, 0.1875D, 0.9375D));
    protected static final VoxelShape SHAPE_DOWN = Shapes.or(Shapes.box(0.1875D, 0, 0.1875D, 0.815D, 0.885D, 0.815D),
            Shapes.box(0.0625D, 0.8125D, 0.0625D, 0.9375D, 1D, 0.9375D));
    protected static final VoxelShape SHAPE_UP_DOWN = Shapes.box(0.1875D, 0, 0.1875D, 0.815D, 1, 0.815D);

    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final EnumProperty<DisplayStatus> ITEM_STATUS = ModBlockProperties.ITEM_STATUS;
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public PedestalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(UP, false).setValue(AXIS, Direction.Axis.X)
                .setValue(DOWN, false).setValue(WATERLOGGED, false).setValue(ITEM_STATUS, DisplayStatus.EMPTY));
    }

    @ForgeOverride
    public float getEnchantPowerBonus(BlockState state, LevelReader world, BlockPos pos) {
        double power = CommonConfigs.Building.CRYSTAL_ENCHANTING.get();
        if (power != 0 && world.getBlockEntity(pos) instanceof PedestalBlockTile te) {
            if (te.getDisplayType() == PedestalBlockTile.DisplayType.CRYSTAL) return (float) power;
        }
        return 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, WATERLOGGED, ITEM_STATUS, AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean flag = level.getFluidState(pos).getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(WATERLOGGED, flag).setValue(AXIS, context.getHorizontalDirection().getAxis())
                .setValue(ITEM_STATUS, getStatus(level, pos, false))
                .setValue(UP, canConnectTo(level.getBlockState(pos.above()), pos, level, Direction.UP, false))
                .setValue(DOWN, canConnectTo(level.getBlockState(pos.below()), pos, level, Direction.DOWN, false));
    }

    public static boolean canConnectTo(BlockState state, BlockPos pos, LevelAccessor world, Direction dir, boolean hasItem) {
        if (state.getBlock() instanceof PedestalBlock) {
            if (dir == Direction.DOWN) {
                return !state.getValue(ITEM_STATUS).hasTile();
            } else if (dir == Direction.UP) {
                return !hasItem;
            }
        }
        return false;
    }

    //called when a neighbor is placed
    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        super.updateShape(stateIn, facing, facingState, level, currentPos, facingPos);
        if (facing == Direction.UP) {
            boolean hasItem = stateIn.getValue(ITEM_STATUS).hasItem();

            return stateIn.setValue(ITEM_STATUS, getStatus(level, currentPos, hasItem))
                    .setValue(UP, canConnectTo(facingState, currentPos, level, facing, hasItem));
        } else if (facing == Direction.DOWN) {
            return stateIn.setValue(DOWN, canConnectTo(facingState, currentPos, level, facing, stateIn.getValue(ITEM_STATUS).hasItem()));
        }
        return stateIn;
    }

    @ForgeOverride
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader world, BlockPos pos, Player player) {
        if (target.getLocation().y() > pos.getY() + 1 - 0.1875) {
            if (world.getBlockEntity(pos) instanceof ItemDisplayTile tile) {
                ItemStack i = tile.getDisplayedItem();
                if (!i.isEmpty()) return i;
            }
        }
        return super.getCloneItemStack(world, pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {

        ItemInteractionResult resultType = ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(ITEM_STATUS).hasTile() && level.getBlockEntity(pos) instanceof PedestalBlockTile tile) {

            //Indiana Jones swap
            if (stack.getItem() instanceof SackItem) {

                ItemStack it = stack.copy();
                it.setCount(1);
                ItemStack removed = tile.removeItemNoUpdate(0);
                tile.setDisplayedItem(it);

                stack.consume(1, player);
                if (!level.isClientSide()) {
                    player.setItemInHand(hand, removed);
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.10F + 0.95F);
                    tile.setChanged();
                } else {
                    //also refreshTextures visuals on client. will get overwritten by packet tho
                    tile.updateClientVisualsOnLoad();
                }
                resultType = ItemInteractionResult.sidedSuccess(level.isClientSide);
            } else {
                resultType = tile.interactWithPlayerItem(player, hand, stack);
            }
        }
        return resultType;
    }

    public static boolean canHaveItemAbove(LevelAccessor level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return !above.is(ModRegistry.PEDESTAL.get()) && !above.isRedstoneConductor(level, pos.above());
    }

    public static DisplayStatus getStatus(LevelAccessor level, BlockPos pos, boolean hasItem) {
        if (hasItem) return DisplayStatus.FULL;
        return canHaveItemAbove(level, pos) ? DisplayStatus.EMPTY : DisplayStatus.NONE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        boolean up = state.getValue(UP);
        boolean down = state.getValue(DOWN);
        if (!up) {
            if (!down) {
                return SHAPE;
            } else {
                return SHAPE_DOWN;
            }
        } else {
            if (!down) {
                return SHAPE_UP;
            } else {
                return SHAPE_UP_DOWN;
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        if (pState.getValue(ITEM_STATUS).hasTile()) {
            return new PedestalBlockTile(pPos, pState);
        }
        return null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof PedestalBlockTile tile)
            return tile.isEmpty() ? 0 : 15;
        else
            return 0;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        if (rotation == Rotation.CLOCKWISE_180) {
            return state;
        } else {
            return switch (state.getValue(AXIS)) {
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                default -> state;
            };
        }
    }

    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(ITEM_STATUS).hasTile()) {
            return (PedestalBlockTile) level.getBlockEntity(pos);
        }
        return new TileLessContainer(state, level, pos);
    }

    @Deprecated(forRemoval = true)
    static class TileLessContainer extends SimpleContainer implements WorldlyContainer {
        private final BlockState state;
        private final LevelAccessor level;
        private final BlockPos pos;
        private PedestalBlockTile tileReference = null;

        public TileLessContainer(BlockState blockState, LevelAccessor levelAccessor,
                                 BlockPos blockPos) {
            super(1);
            this.state = blockState;
            this.level = levelAccessor;
            this.pos = blockPos;
        }

        @Override
        public boolean stillValid(Player player) {
            return tileReference == null;
        }

        @Override
        public ItemStack getItem(int slot) {
            if (tileReference != null) return tileReference.getItem(slot);
            return super.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (tileReference != null) return tileReference.removeItem(slot, amount);
            return super.removeItem(slot, amount);
        }

        @Override
        public boolean isEmpty() {
            if (tileReference != null) return tileReference.isEmpty();
            return super.isEmpty();
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (tileReference != null) return tileReference.removeItemNoUpdate(slot);
            return super.removeItemNoUpdate(slot);
        }

        @Override
        public void clearContent() {
            if (tileReference != null) tileReference.clearContent();
            else super.clearContent();
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            if (tileReference != null) return tileReference.canPlaceItem(index, stack);
            return super.canPlaceItem(index, stack);
        }

        @Override
        public int getMaxStackSize() {
            if (tileReference != null) return tileReference.getMaxStackSize();
            return 1;
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            if (tileReference != null) return tileReference.getSlotsForFace(side);
            return new int[]{0};
        }

        @Override
        public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
            if (tileReference != null) return canTakeItemThroughFace(index, itemStack, direction);
            return true;
        }

        @Override
        public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
            if (tileReference != null) return tileReference.canTakeItemThroughFace(index, stack, direction);
            return !this.isEmpty();
        }

        @Override
        public void setChanged() {
            if (!this.isEmpty()) {
                var item = this.getItem(0);
                if (!item.isEmpty()) {
                    level.setBlock(pos, state.setValue(PedestalBlock.ITEM_STATUS, DisplayStatus.EMPTY), 3);
                }
                if (level.getBlockEntity(pos) instanceof PedestalBlockTile tile) {

                    this.tileReference = tile;
                    tile.setDisplayedItem(item);
                }
            }
        }
    }
}