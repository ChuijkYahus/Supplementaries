package net.mehvahdjukaar.supplementaries.items;

import net.mehvahdjukaar.supplementaries.blocks.JarBlock;
import net.mehvahdjukaar.supplementaries.blocks.JarBlockTile;
import net.mehvahdjukaar.supplementaries.common.CommonUtil;
import net.mehvahdjukaar.supplementaries.setup.Registry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DrinkHelper;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class JarItem extends BlockItem {
    public JarItem(Block blockIn, Properties properties) {
        super(blockIn, properties);
    }


    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        CompoundNBT compoundnbt = stack.getChildTag("BlockEntityTag");
        if (compoundnbt != null) {
            if (compoundnbt.contains("LootTable", 8)) {
                tooltip.add(new StringTextComponent("???????"));
            }

            if (compoundnbt.contains("Items", 9)) {
                NonNullList<ItemStack> nonnulllist = NonNullList.withSize(27, ItemStack.EMPTY);
                ItemStackHelper.loadAllItems(compoundnbt, nonnulllist);
                int i = 0;
                int j = 0;

                for(ItemStack itemstack : nonnulllist) {
                    if (!itemstack.isEmpty()) {
                        ++j;
                        if (i <= 4) {
                            ++i;
                            IFormattableTextComponent iformattabletextcomponent = itemstack.getDisplayName().deepCopy();

                            String s = iformattabletextcomponent.getString();
                            s = s.replace(" Bucket", "");
                            s = s.replace(" Bottle", "");
                            s = s.replace("Bucket of ", "");
                            IFormattableTextComponent str = new StringTextComponent(s);

                            str.appendString(" x").appendString(String.valueOf(itemstack.getCount()));
                            tooltip.add(str);
                        }
                    }
                }
                if (j - i > 0) {
                    tooltip.add((new TranslationTextComponent("container.shulkerBox.more", j - i)).mergeStyle(TextFormatting.ITALIC));
                }
            }
        }
        //mob jar
        CompoundNBT com = stack.getChildTag("CachedJarMobValues");
        if (com != null){
            if(com.contains("Name")){
                tooltip.add(new StringTextComponent(com.getString("Name")));
            }
        }
    }


    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        ItemStack stack = context.getItem();
        CompoundNBT com = stack.getTag();
        if(!context.getPlayer().isSneaking() && com!=null && com.contains("JarMob")){
            World world = context.getWorld();
            Entity entity  = EntityType.loadEntityAndExecute(com.getCompound("JarMob"), world, o -> o);
            if(entity!=null) {
                if(!world.isRemote) {
                    Vector3d v = context.getHitVec();
                    entity.setPositionAndRotation(v.getX(), v.getY(), v.getZ(), context.getPlacementYaw(), 0);
                    world.addEntity(entity);
                }
                boolean flag = this.getItem() == Registry.JAR_ITEM;
                if(!context.getPlayer().isCreative()) {
                   ItemStack returnItem = new ItemStack(flag ? Registry.EMPTY_JAR_ITEM : Registry.EMPTY_JAR_ITEM_TINTED);
                   if(stack.hasDisplayName())returnItem.setDisplayName(stack.getDisplayName());
                   context.getPlayer().setHeldItem(context.getHand(), returnItem);
                }

            }
            return ActionResultType.SUCCESS;

        }
        return super.onItemUse(context);
    }

    @Override
    public ActionResultType tryPlace(BlockItemUseContext context) {
        ActionResultType placeresult = super.tryPlace(context);
        if(placeresult.isSuccessOrConsume()) {
            World world = context.getWorld();
            BlockPos pos = context.getPos();
            TileEntity te = world.getTileEntity(pos);
            if(te instanceof JarBlockTile){
                JarBlockTile mobjar = ((JarBlockTile)te);
                CompoundNBT compound = context.getItem().getTag();
                if(compound!=null&&compound.contains("JarMob")&&compound.contains("CachedJarMobValues")) {
                    CompoundNBT com2 = compound.getCompound("CachedJarMobValues");
                    CompoundNBT com = compound.getCompound("JarMob");

                    mobjar.entityData = com;
                    mobjar.yOffset = com2.getFloat("YOffset");
                    mobjar.scale = com2.getFloat("Scale");
                    //TODO: rewrite this check
                    if (!world.isRemote && (com.getString("id").equals("minecraft:endermite")||com.getString("id").equals("iceandfire:pixie"))){
                        BlockState state = world.getBlockState(pos);
                        if(state.get(JarBlock.LIGHT_LEVEL)<5){
                            world.setBlockState(pos,state.with(JarBlock.LIGHT_LEVEL, 5));
                        }
                    }
                    mobjar.markDirty();
                    //mobjar.updateMob();

                }
            }
        }
        return placeresult;
    }

}
