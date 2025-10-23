package net.mehvahdjukaar.supplementaries.common.entities;

import net.mehvahdjukaar.moonlight.api.entity.ImprovedProjectileEntity;
import net.mehvahdjukaar.supplementaries.common.block.blocks.AwningBlock;
import net.mehvahdjukaar.supplementaries.common.block.fire_behaviors.ProjectileStats;
import net.mehvahdjukaar.supplementaries.common.entities.data.SlimedData;
import net.mehvahdjukaar.supplementaries.configs.CommonConfigs;
import net.mehvahdjukaar.supplementaries.reg.ModEntities;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.mehvahdjukaar.supplementaries.reg.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class SlimeBallEntity extends ImprovedProjectileEntity {

    private int bounces = 0;

    public SlimeBallEntity(Level world, double x, double y, double z) {
        super(ModEntities.THROWABLE_SLIMEBALL.get(), x, y, z, world);
        this.maxAge = 400;
    }

    public SlimeBallEntity(LivingEntity thrower) {
        super(ModEntities.THROWABLE_SLIMEBALL.get(), thrower, thrower.level());
        this.maxAge = 400;
    }

    public SlimeBallEntity(EntityType<SlimeBallEntity> type, Level level) {
        super(type, level);
        this.maxAge = 400;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("bounces", this.bounces);
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        this.bounces = compound.getInt("bounces");
    }

    @Override
    protected Component getTypeName() {
        return this.getItem().getDisplayName();
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        Direction hitDirection = result.getDirection();
        Vector3f surfaceNormal = hitDirection.step();
        BlockState hitState = level().getBlockState(result.getBlockPos());
        if(hitDirection == Direction.UP && hitState.getBlock() instanceof AwningBlock){
            surfaceNormal = AwningBlock.getNormalVector(hitState);
        }

        Vec3 velocity = this.getDeltaMovement();
        Vec3 newVel = new Vec3(velocity.toVector3f().reflect(surfaceNormal));

        bounce(newVel);
    }

    private void bounce(Vec3 newVel) {
        bounces++;
        Vec3 velocity = this.getDeltaMovement();


        float conservedEnergy = 0.75f;
        newVel = newVel.scale(conservedEnergy);
        this.setDeltaMovement(newVel);
        //adds distance that was eaten up by collision
        double missingDistance = velocity.subtract(this.position().subtract(new Vec3(xo, yo, zo))).length();
        Vec3 missingVel = newVel.normalize().scale(missingDistance);
        this.move(MoverType.SELF, missingVel);
        //this.setPos(this.position().add(missingVel));
        //this.setPos(this.position().add(surfaceNormal.x * 0.1f, surfaceNormal.y * 0.1f, surfaceNormal.z * 0.1f));

        if (!level().isClientSide) {
            this.hasImpulse = true;
            addParticleEffects();
            this.playSound(ModSounds.SLIMEBALL_LAND.get(), 1.5f, 1);
            if (bounces > 3) {
                this.discard();
            }
        }
    }

    private void addParticleEffects() {
        this.level().broadcastEntityEvent(this, (byte) 3);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ParticleOptions particleOptions = new ItemParticleOption(ParticleTypes.ITEM, this.getItem());

            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(particleOptions, this.getX(), this.getY(), this.getZ(),
                        0.0, 0.0, 0.0);
            }
        }

    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if(entity instanceof LivingEntity le && le.isBlocking()){
            Vec3 hit = result.getLocation();
            Vec3 entityView = le.getViewVector(1.0F);
            Vec3 normal = hit.vectorTo(le.position()).normalize();
            normal = new Vec3(normal.x, 0.0, normal.z);
            if (normal.dot(entityView) < 0.0) {

                bounce(this.getDeltaMovement().scale(-1));
                return;
            }
        }
        if (entity instanceof LivingEntity le  && le.attackable()) {
            //sets on both but also sends packet just because lmao
            SlimedData slimedData = ModRegistry.SLIMED_DATA.getOrCreate(le);
            slimedData.setSlimedTicks(le, CommonConfigs.Tweaks.SLIME_DURATION.get());
        }
        else if (entity instanceof EndCrystal) {
            entity.hurt(this.damageSources().thrown(this, this.getOwner()), 0);
        }else {
            //somehow allows entity event to be received before entity is broken
            this.hasImpulse = true;
            addParticleEffects();
        }
        this.discard();
    }

    @Override
    public float getDefaultShootVelocity() {
        return ProjectileStats.SLIMEBALL_SPEED;
    }

    @Override
    public boolean canHarmOwner() {
        return true;
    }
}
