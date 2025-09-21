package net.mehvahdjukaar.supplementaries.client.renderers;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.supplementaries.reg.ClientRegistry;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public abstract class SphereRenderType extends RenderType {

    public static final Function<ResourceLocation, RenderType> RENDER_TYPE = Util.memoize((resourceLocation) -> {
        CompositeState compositeState = CompositeState.builder()
                .setShaderState(new ShaderStateShard(ClientRegistry.SPHERE_SHADER))
                .setTextureState(new TextureStateShard(resourceLocation, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
        return create("spherify", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, true, false, compositeState);
    });

    public SphereRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}