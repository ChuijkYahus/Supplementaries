package net.mehvahdjukaar.supplementaries.mixins.neoforge.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.mehvahdjukaar.supplementaries.reg.ModFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(DefaultFluidRenderer.class)
public abstract class CompatSodiumFluidRendererMixin {

    @Shadow
    @Final
    private QuadLightData quadLightData;

    @WrapOperation(method = "fluidHeight", remap = false,
            at = @At(value = "INVOKE",
                    remap = true,
                    target = "Lnet/minecraft/world/level/material/Fluid;isSame(Lnet/minecraft/world/level/material/Fluid;)Z"))
    public boolean supplementaries$modifyLumiseneHeight(Fluid instance, Fluid above, Operation<Boolean> original) {
        return original.call(instance, above) || above.isSame(ModFluids.LUMISENE_FLUID.get());
    }

    @Inject(method = "updateQuad",
            at = @At(value = "INVOKE",
                    remap = true,
                    shift = At.Shift.AFTER,
                    target = "Lnet/caffeinemc/mods/sodium/client/model/color/ColorProvider;getColors(Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos$MutableBlockPos;Ljava/lang/Object;Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;[I)V"),
            remap = false)
    public void supplementaries$modifyLumiseneEmissivity(ModelQuadViewMutable quad, LevelSlice level, BlockPos pos, LightPipeline lighter, Direction dir, ModelQuadFacing facing, float brightness,
                                                         ColorProvider<FluidState> colorProvider, FluidState fluidState, CallbackInfo ci) {

        if (fluidState.is(ModFluids.LUMISENE_FLUID.get())) {
            QuadLightData light = this.quadLightData;

            int minLight = ModFluids.LUMISENE_FAKE_LIGHT_EMISSION - 3;
            for (int j = 0; j < light.lm.length; j++) {
                int l = light.lm[j];
                int bl = LightTexture.block(l);
                int sl = LightTexture.sky(l);
                if (bl < minLight) {
                    bl = minLight;
                }
                // this removes smooth lighting from lights lower than me
                light.lm[j] = LightTexture.pack(bl, sl);

                // no shading on emissive stuff!
                //TODO: this cant be correct! without however stuff is shader when against blocks
                light.br[j] = 1.0F;
            }
        }
    }



    /*
    @Inject(method = "calculateAverageHeight",
            at = @At("HEAD"), cancellable = true)
    public void supplementaries$modifyLumiseneHeight(BlockAndTintGetter level, Fluid fluid, float g, float h, float i, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        ModFluidsImpl.messWithAvH(level, fluid, g, h, i, pos, cir);
    }

    @ModifyExpressionValue(method = "tesselate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
    public int supplementaries$modifyLumiseneLight(int original, @Local  Fluid fluid) {
       return ModFluidsImpl.messWithFluidLight(original, fluid);
    }*/

    @ModifyArg(method = "render",
            remap = false,
            at = @At(value = "INVOKE",
                    remap = false,
                    target = "Lnet/caffeinemc/mods/sodium/client/model/light/LightPipelineProvider;getLighter(Lnet/caffeinemc/mods/sodium/client/model/light/LightMode;)Lnet/caffeinemc/mods/sodium/client/model/light/LightPipeline;"))
    public LightMode supplementaries$modifyLumiseneLight(LightMode lightMode, @Local Fluid fluid) {
        if (fluid == ModFluids.LUMISENE_FLUID.get()) {
            return Minecraft.getInstance().options.ambientOcclusion().get() ? LightMode.SMOOTH : LightMode.FLAT;
        }
        return lightMode;
    }

}
