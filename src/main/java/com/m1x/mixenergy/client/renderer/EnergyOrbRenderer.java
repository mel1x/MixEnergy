package com.m1x.mixenergy.client.renderer;

import com.m1x.mixenergy.MixEnergy;
import com.m1x.mixenergy.common.entity.EnergyOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
// LightTexture was removed in 26.2, where renderers read the packed light coordinates
// straight off the render state instead.
//? if <26 {
import net.minecraft.client.renderer.LightTexture;
//?}
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
// ResourceLocation became Identifier and RenderType moved into its own package in 1.21.11.
//? if <1.21.11 {
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
//?} else {
/*import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
*///?}
// Entity rendering stopped writing into a buffer directly in 1.21.9: geometry is now
// submitted to a collector that draws it later in the frame.
//? if <1.21.9 {
import net.minecraft.client.renderer.MultiBufferSource;
//?} else {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?}
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState;
*///?}
//? if >=26 {
/*import net.minecraft.client.renderer.state.level.CameraRenderState;
*///?} elif >=1.21.9 {
/*import net.minecraft.client.renderer.state.CameraRenderState;
*///?}

// EntityRenderer gained a render-state type parameter in 1.21.2: entity data is copied
// into a state object first and rendering only reads that state.
//? if <1.21.2 {
public class EnergyOrbRenderer extends EntityRenderer<EnergyOrbEntity> {
//?} else {
/*public class EnergyOrbRenderer
        extends EntityRenderer<EnergyOrbEntity, EnergyOrbRenderer.OrbRenderState> {
*///?}
    //? if <1.21.11 {
    private static final ResourceLocation TEXTURE =
            MixEnergy.id("textures/entity/energy_orb.png");
    private static final RenderType RENDER_TYPE =
            RenderType.itemEntityTranslucentCull(TEXTURE);
    //?} elif <26 {
    /*private static final Identifier TEXTURE =
            MixEnergy.id("textures/entity/energy_orb.png");
    private static final RenderType RENDER_TYPE =
            RenderTypes.itemEntityTranslucentCull(TEXTURE);
    *///?} else {
    /*private static final Identifier TEXTURE =
            MixEnergy.id("textures/entity/energy_orb.png");
    private static final RenderType RENDER_TYPE =
            RenderTypes.entityTranslucentCullItemTarget(TEXTURE);
    *///?}

    private static final float SIZE = 0.1f;

    /**
     * Packed light coordinates for a fully lit quad. 26.2 removed the LightTexture class
     * that named this value; the packing itself is unchanged.
     */
    //? if <26 {
    private static final int ORB_LIGHT = LightTexture.FULL_BRIGHT;
    //?} else {
    /*private static final int ORB_LIGHT = 0xF000F0;
    *///?}

    public EnergyOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.15f;
        shadowStrength = 0.75f;
    }

    @Override
    protected int getBlockLightLevel(EnergyOrbEntity entity, BlockPos position) {
        return 15;
    }

    //? if <1.21.2 {
    @Override
    public void render(
            EnergyOrbEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        float animationTicks = entity.tickCount + partialTicks;

        poseStack.pushPose();
        applyOrbTransform(
                poseStack,
                entityRenderDispatcher.cameraOrientation(),
                animationTicks,
                entity.getEnergyAmount()
        );
        emitOrbQuads(poseStack.last(), buffer.getBuffer(RENDER_TYPE), animationTicks, ORB_LIGHT);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(EnergyOrbEntity entity) {
        return TEXTURE;
    }
    //?} elif <1.21.9 {
    /*@Override
    public OrbRenderState createRenderState() {
        return new OrbRenderState();
    }

    @Override
    public void extractRenderState(
            EnergyOrbEntity entity,
            OrbRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);
        state.energyAmount = entity.getEnergyAmount();
    }

    @Override
    public void render(
            OrbRenderState state,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        poseStack.pushPose();
        applyOrbTransform(
                poseStack,
                entityRenderDispatcher.cameraOrientation(),
                state.ageInTicks,
                state.energyAmount
        );
        emitOrbQuads(poseStack.last(), buffer.getBuffer(RENDER_TYPE), state.ageInTicks, ORB_LIGHT);
        poseStack.popPose();

        super.render(state, poseStack, buffer, packedLight);
    }
    *///?} else {
    /*@Override
    public OrbRenderState createRenderState() {
        return new OrbRenderState();
    }

    @Override
    public void extractRenderState(
            EnergyOrbEntity entity,
            OrbRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);
        state.energyAmount = entity.getEnergyAmount();
    }

    @Override
    public void submit(
            OrbRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraRenderState
    ) {
        float animationTicks = state.ageInTicks;

        poseStack.pushPose();
        applyOrbTransform(
                poseStack,
                cameraRenderState.orientation,
                animationTicks,
                state.energyAmount
        );
        collector.submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> emitOrbQuads(pose, consumer, animationTicks, ORB_LIGHT)
        );
        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraRenderState);
    }
    *///?}

    //? if >=1.21.2 {
    /*public static class OrbRenderState extends EntityRenderState {
        public float energyAmount = EnergyOrbEntity.BASE_ENERGY_AMOUNT;
    }
    *///?}

    /** Bobs the orb, turns it towards the camera and scales it by the energy it carries. */
    private static void applyOrbTransform(
            PoseStack poseStack,
            Quaternionf cameraOrientation,
            float animationTicks,
            float energyAmount
    ) {
        float bob = Mth.sin(animationTicks * 0.05f) * 0.1f;
        poseStack.translate(0.0, 0.1f + bob, 0.0);
        poseStack.mulPose(cameraOrientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        float energyScale = Mth.clamp(
                0.8f + Mth.sqrt(energyAmount / EnergyOrbEntity.BASE_ENERGY_AMOUNT) * 0.2f,
                1.0f,
                1.65f
        );
        poseStack.scale(SIZE * energyScale, SIZE * energyScale, SIZE * energyScale);
    }

    /** Writes the orb quad and the larger translucent glow quad behind it. */
    private static void emitOrbQuads(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float animationTicks,
            int lightCoords
    ) {
        float pulsate = 0.88f + (Mth.sin(animationTicks * 0.1f) + 1.0f) * 0.06f;

        vertex(consumer, pose, -1, -1, 0, 0, 1, lightCoords, 1.0f);
        vertex(consumer, pose, 1, -1, 1, 0, 1, lightCoords, 1.0f);
        vertex(consumer, pose, 1, 1, 1, 1, 1, lightCoords, 1.0f);
        vertex(consumer, pose, -1, 1, 0, 1, 1, lightCoords, 1.0f);

        float glowScale = 1.2f;
        float glowAlpha = 0.45f * pulsate;
        vertex(consumer, pose, -glowScale, -glowScale, 0, 0,
                glowAlpha, lightCoords, 1.0f);
        vertex(consumer, pose, glowScale, -glowScale, 1, 0,
                glowAlpha, lightCoords, 1.0f);
        vertex(consumer, pose, glowScale, glowScale, 1, 1,
                glowAlpha, lightCoords, 1.0f);
        vertex(consumer, pose, -glowScale, glowScale, 0, 1,
                glowAlpha, lightCoords, 1.0f);
    }


    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float u,
            float v,
            float alpha,
            int light,
            float intensity
    ) {
        // Minecraft 1.20.5 dropped VertexConsumer#normal(Matrix3f, ...) in favour of a
        // normal(PoseStack.Pose, ...) overload, while keeping the rest of the chained
        // builder; 1.21 then replaced the whole chain (and dropped endVertex()) with the
        // addVertex/setColor/setUv/setOverlay/setLight/setNormal style used below.
        //? if <1.20.5 {
        consumer.vertex(pose.pose(), x, y, 0.0f)
                .color(intensity, intensity, intensity, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0f, 1.0f, 0.0f)
                .endVertex();
        //?} elif <1.21 {
        /*consumer.vertex(pose.pose(), x, y, 0.0f)
                .color(intensity, intensity, intensity, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose, 0.0f, 1.0f, 0.0f)
                .endVertex();
        *///?} else {
        /*consumer.addVertex(pose.pose(), x, y, 0.0f)
                .setColor(intensity, intensity, intensity, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
        *///?}
    }
}
