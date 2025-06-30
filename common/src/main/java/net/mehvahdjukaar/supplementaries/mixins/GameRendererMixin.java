package net.mehvahdjukaar.supplementaries.mixins;

import net.mehvahdjukaar.supplementaries.client.MobHeadShadersManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    public abstract void loadEffect(ResourceLocation resourceLocation);

    //forge has an event for this but doing it this way is better as we can use instance check
    @Inject(method = "checkEntityPostEffect", at = @At("TAIL"))
    protected void supp$addCustomPostShaders(Entity entity, CallbackInfo ci) {

        ResourceLocation shader = MobHeadShadersManager.INSTANCE.getSpectatorShaders(entity);
        if (shader != null) {
            this.loadEffect(shader);
        }

    }
}
