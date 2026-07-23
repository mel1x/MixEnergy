package com.m1x.mixenergy.client.renderer;

import com.m1x.mixenergy.common.entity.EnergyOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class EnergyOrbRenderer extends EntityRenderer<EnergyOrbEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("mixenergy", "textures/entity/energy_orb.png");
    private static final RenderType RENDER_TYPE =
            RenderType.itemEntityTranslucentCull(TEXTURE);
    private static final float SIZE = 0.1f;

    public EnergyOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.15f;
        shadowStrength = 0.75f;
    }

    @Override
    protected int getBlockLightLevel(EnergyOrbEntity entity, BlockPos position) {
        return 15;
    }

    @Override
    public void render(
            EnergyOrbEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        poseStack.pushPose();

        float animationTicks = entity.tickCount + partialTicks;
        float bob = Mth.sin(animationTicks * 0.05f) * 0.1f;
        poseStack.translate(0.0, 0.1f + bob, 0.0);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        float energyScale = Mth.clamp(
                0.8f + Mth.sqrt(entity.getEnergyAmount() / EnergyOrbEntity.BASE_ENERGY_AMOUNT) * 0.2f,
                1.0f,
                1.65f
        );
        poseStack.scale(SIZE * energyScale, SIZE * energyScale, SIZE * energyScale);

        VertexConsumer vertexConsumer = buffer.getBuffer(RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        float pulsate = 0.88f + (Mth.sin(animationTicks * 0.1f) + 1.0f) * 0.06f;

        vertex(vertexConsumer, poseMatrix, normalMatrix, -1, -1, 0, 0, 1,
                LightTexture.FULL_BRIGHT, 1.0f);
        vertex(vertexConsumer, poseMatrix, normalMatrix, 1, -1, 1, 0, 1,
                LightTexture.FULL_BRIGHT, 1.0f);
        vertex(vertexConsumer, poseMatrix, normalMatrix, 1, 1, 1, 1, 1,
                LightTexture.FULL_BRIGHT, 1.0f);
        vertex(vertexConsumer, poseMatrix, normalMatrix, -1, 1, 0, 1, 1,
                LightTexture.FULL_BRIGHT, 1.0f);

        float glowScale = 1.2f;
        float glowAlpha = 0.45f * pulsate;
        vertex(vertexConsumer, poseMatrix, normalMatrix, -glowScale, -glowScale, 0, 0,
                glowAlpha, LightTexture.FULL_BRIGHT, 1.0f);
        vertex(vertexConsumer, poseMatrix, normalMatrix, glowScale, -glowScale, 1, 0,
                glowAlpha, LightTexture.FULL_BRIGHT, 1.0f);
        vertex(vertexConsumer, poseMatrix, normalMatrix, glowScale, glowScale, 1, 1,
                glowAlpha, LightTexture.FULL_BRIGHT, 1.0f);
        vertex(vertexConsumer, poseMatrix, normalMatrix, -glowScale, glowScale, 0, 1,
                glowAlpha, LightTexture.FULL_BRIGHT, 1.0f);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f pose,
            Matrix3f normal,
            float x,
            float y,
            float u,
            float v,
            float alpha,
            int light,
            float intensity
    ) {
        consumer.vertex(pose, x, y, 0.0f)
                .color(intensity, intensity, intensity, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0f, 1.0f, 0.0f)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EnergyOrbEntity entity) {
        return TEXTURE;
    }
}
