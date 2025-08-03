package net.mehvahdjukaar.supplementaries.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.supplementaries.api.IQuiverEntity;
import net.mehvahdjukaar.supplementaries.common.items.QuiverItem;
import net.mehvahdjukaar.supplementaries.common.items.loot.RandomArrowFunction;
import net.mehvahdjukaar.supplementaries.configs.CommonConfigs;
import net.mehvahdjukaar.supplementaries.reg.ModComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin extends Monster {

    protected AbstractSkeletonMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    public void supp$finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        if (this.getType() == EntityType.SKELETON || this.getType() == EntityType.STRAY && CommonConfigs.Tools.QUIVER_ENABLED.get()) {
            double chance = CommonConfigs.Tools.QUIVER_SKELETON_SPAWN.get() * (
                    CommonConfigs.Tools.QUIVER_DEPEND_ON_GLOBAL_DIFFICULTY.get() ? difficulty.getSpecialMultiplier() :0.1);
            if (random.nextFloat() < chance) {
                ((IQuiverEntity) this).supplementaries$setQuiver(
                        RandomArrowFunction.createRandomQuiver(level.getRandom(), difficulty.getSpecialMultiplier()));
            }
        }
    }

    @Inject(method = "performRangedAttack", at = @At(value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/entity/monster/AbstractSkeleton;getArrow(Lnet/minecraft/world/item/ItemStack;FLnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/projectile/AbstractArrow;",
            shift = At.Shift.AFTER))
    public void supp$consumeQuiverArrow(LivingEntity target, float velocity, CallbackInfo ci, @Local(ordinal = 0) ItemStack arrow) {
        if (this instanceof IQuiverEntity quiverEntity) {
            var quiver = quiverEntity.supplementaries$getQuiver();
            //ignore offhand as it has priority over quiver
            if (!quiver.isEmpty() && this.getItemInHand(InteractionHand.OFF_HAND).getItem() != arrow.getItem()) {
                var data = quiver.get(ModComponents.QUIVER_CONTENT.get());
                if (data != null){
                    var mutable = data.toMutable();
                    mutable.getSelected().shrink(1);
                    quiver.set(ModComponents.QUIVER_CONTENT.get(), mutable.toImmutable());
                }


            }
        }
    }


}