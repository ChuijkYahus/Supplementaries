package net.mehvahdjukaar.supplementaries.common.block.blocks;

import net.mehvahdjukaar.supplementaries.common.entities.data.SlimedData;
import net.mehvahdjukaar.supplementaries.reg.ModParticles;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SoapBlock extends Block {
    public SoapBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState pState, Level level, BlockPos pos, RandomSource random) {

    }

    @Override
    public boolean triggerEvent(BlockState pState, Level level, BlockPos pos, int pId, int pParam) {
        if (pId == 0) {
            RandomSource r = level.random;
            for (int i = 0; i < 2; i++) {
                level.addParticle(ModParticles.SUDS_PARTICLE.get(), pos.getX() + r.nextFloat(), pos.getY() + 1.1, pos.getZ() + r.nextFloat(),
                        (0.5 - r.nextFloat()) * 0.13f, (r.nextFloat()) * 0.1f, (0.5 - r.nextFloat()) * 0.13f);
            }
            return true;
        }
        return super.triggerEvent(pState, level, pos, pId, pParam);
    }

/*
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        Random rand = entity.level.random;
        if ((!level.isClientSide || entity instanceof LocalPlayer) && !entity.isSteppingCarefully()) {
            //chance
            if (rand.nextFloat() < 0.14) {
                Vec3 m = entity.getDeltaMovement();
                m.subtract(0, m.y, 0);
                if (m.lengthSqr() > 0.0008) {
                    //intensity
                    m = m.normalize().scale(0.10 + rand.nextFloat() * 0.13F);
                    float min = 22;
                    float max = 95;
                    float angle = 20 + rand.nextFloat() * (max - min);
                    angle *= rand.nextBoolean() ? -1 : 1;
                    m = m.yRot((float) (angle * Mth.DEG_TO_RAD));
                    entity.setDeltaMovement(entity.getDeltaMovement().add(m.x, 0.0F, m.z));
                    level.blockEvent(pos, state.getBlock(), 0, 0);
                }
            }
        }
        super.stepOn(level, pos, state, entity);
    }

*/

    @Override
    public void stepOn(Level level, BlockPos pPos, BlockState state, Entity entity) {
        RandomSource rand = level.random;
        double slipChance = 0.14;
        if ((!level.isClientSide || entity instanceof LocalPlayer) && !entity.isSteppingCarefully()) {
            if (rand.nextFloat() < slipChance) {
                var m = entity.getDeltaMovement();
                m.subtract(0, m.y, 0);
                if (m.lengthSqr() > 0.0008) {
                    m = m.normalize().scale(0.10 + rand.nextFloat() * 0.13F);
                    float min = 22;
                    float max = 95;
                    float angle = 20 + rand.nextFloat() * (max - min);
                    angle *= rand.nextBoolean() ? -1 : 1;
                    m = m.yRot(angle * Mth.DEG_TO_RAD);
                    //PACKET HERE
                    entity.setDeltaMovement(entity.getDeltaMovement().add(m.x, 0.0F, m.z));
                    level.blockEvent(pPos, state.getBlock(), 0, 0);
                }
            }
        }
        if (!level.isClientSide && entity instanceof LivingEntity le) {
            SlimedData data = ModRegistry.SLIMED_DATA.getOrCreate(le);
            if (data != null && rand.nextFloat() < slipChance) {
                data.clear((LivingEntity) entity);
                level.blockEvent(pPos, state.getBlock(), 0, 0);
            }
        }
    }
/*
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        //TODO: add this functionality & soap
        if (!level.isClientSide && (entity.xOld != entity.getX() || entity.zOld != entity.getZ())) {

            double d0 = Math.abs(entity.getX() - entity.xOld);
            double d1 = Math.abs(entity.getZ() - entity.zOld);
            if (d0 >= (double) 0.003F || d1 >= (double) 0.003F) {
                Random rand = entity.level.random;
                if (rand.nextFloat()  < 0.14 && !entity.isSteppingCarefully()) {
                    Vec3 m = entity.getDeltaMovement();
                    m = m.normalize().scale(0.12 + rand.nextFloat() * 0.15F);
                    float min = 22;
                    float max = 95;
                    float angle = 20 + rand.nextFloat()*(max-min);
                    angle *= rand.nextBoolean() ? -1 :1;
                    m = m.yRot((float) (angle*Math.PI/180f));
                    entity.setDeltaMovement(entity.getDeltaMovement().add(m.x, 0.0F, m.z));
                    level.blockEvent(pos, state.getBlock(), 0, 0);

                }
            }
        }
        super.stepOn(level, pos, state, entity);
    }
*/
}
