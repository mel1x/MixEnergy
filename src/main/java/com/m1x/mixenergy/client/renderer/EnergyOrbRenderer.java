package com.m1x.mixenergy.client.renderer;

import com.m1x.mixenergy.common.entity.EnergyOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class EnergyOrbRenderer extends EntityRenderer<EnergyOrbEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("mixenergy", "textures/entity/energy_orb.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);
    private static final float SIZE = 0.1f;

    public EnergyOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EnergyOrbEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, 
                      MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Make the orb bob up and down
        float bob = (float) Math.sin(entity.tickCount * 0.05) * 0.1f;
        poseStack.translate(0, 0.1f + bob, 0);
        
        // Always face the player (camera)
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        
        // Scale to size
        poseStack.scale(SIZE, SIZE, SIZE);

        VertexConsumer vertexConsumer = buffer.getBuffer(RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f pose4f = pose.pose();
        Matrix3f normal3f = pose.normal();
        
        // Use full bright lighting (15728880 = 0xF000F0)
        int fullBrightness = 16828880;
        
        // Make the orb pulsate slightly
        float pulsate = 0.8f + (Mth.sin(entity.tickCount * 0.1f) + 1f) * 0.1f;
        
        // Render a billboard quad with full brightness
        vertex(vertexConsumer, pose4f, normal3f, -1, -1, 0, 0, 1, fullBrightness, pulsate);
        vertex(vertexConsumer, pose4f, normal3f, 1, -1, 1, 0, 1, fullBrightness, pulsate);
        vertex(vertexConsumer, pose4f, normal3f, 1, 1, 1, 1, 1, fullBrightness, pulsate);
        vertex(vertexConsumer, pose4f, normal3f, -1, 1, 0, 1, 1, fullBrightness, pulsate);
        
        // Render a second layer to enhance glow effect (slightly larger)
        float scale = 1.2f;
        float alpha = 0.6f * pulsate;
        vertex(vertexConsumer, pose4f, normal3f, -scale, -scale, 0, 0, alpha, fullBrightness, 1.0f);
        vertex(vertexConsumer, pose4f, normal3f, scale, -scale, 1, 0, alpha, fullBrightness, 1.0f);
        vertex(vertexConsumer, pose4f, normal3f, scale, scale, 1, 1, alpha, fullBrightness, 1.0f);
        vertex(vertexConsumer, pose4f, normal3f, -scale, scale, 0, 1, alpha, fullBrightness, 1.0f);
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer vertexConsumer, Matrix4f pose, Matrix3f normal, 
                              float x, float y, float u, float v, float alpha, int light, float intensity) {
        vertexConsumer.vertex(pose, x, y, 0)
                .color(intensity, intensity, intensity, alpha)  // Increase intensity for all RGB channels
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0, 0, 1)
                .endVertex();
    }

    // Overload for compatibility with existing calls
    private static void vertex(VertexConsumer vertexConsumer, Matrix4f pose, Matrix3f normal, 
                              float x, float y, float u, float v, float alpha, int light) {
        vertex(vertexConsumer, pose, normal, x, y, u, v, alpha, light, 1.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(EnergyOrbEntity entity) {
        return TEXTURE;
    }
} 