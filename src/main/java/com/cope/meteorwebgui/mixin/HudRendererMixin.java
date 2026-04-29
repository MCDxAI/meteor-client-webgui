package com.cope.meteorwebgui.mixin;

import com.cope.meteorwebgui.hud.HudPreviewCapture;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HudRenderer.class)
public class HudRendererMixin {

    @Inject(method = "text(Ljava/lang/String;DDLmeteordevelopment/meteorclient/utils/render/color/Color;Z)D", at = @At("HEAD"))
    private void meteorwebgui$captureText(String text, double x, double y, Color color, boolean shadow, CallbackInfoReturnable<Double> cir) {
        HudPreviewCapture.recordText(text, x, y, color, shadow, 1.0);
    }

    @Inject(method = "text(Ljava/lang/String;DDLmeteordevelopment/meteorclient/utils/render/color/Color;ZD)D", at = @At("HEAD"))
    private void meteorwebgui$captureScaledText(String text, double x, double y, Color color, boolean shadow, double scale, CallbackInfoReturnable<Double> cir) {
        HudPreviewCapture.recordText(text, x, y, color, shadow, scale);
    }

    @Inject(method = "line(DDDDLmeteordevelopment/meteorclient/utils/render/color/Color;)V", at = @At("HEAD"))
    private void meteorwebgui$markLine(double x1, double y1, double x2, double y2, Color color, CallbackInfo ci) {
        HudPreviewCapture.markNonText();
    }

    @Inject(method = "quad(DDDDLmeteordevelopment/meteorclient/utils/render/color/Color;)V", at = @At("HEAD"))
    private void meteorwebgui$markQuad(double x, double y, double width, double height, Color color, CallbackInfo ci) {
        HudPreviewCapture.markNonText();
    }

    @Inject(method = "quad(DDDDLmeteordevelopment/meteorclient/utils/render/color/Color;Lmeteordevelopment/meteorclient/utils/render/color/Color;Lmeteordevelopment/meteorclient/utils/render/color/Color;Lmeteordevelopment/meteorclient/utils/render/color/Color;)V", at = @At("HEAD"))
    private void meteorwebgui$markGradient(double x, double y, double width, double height, Color c1, Color c2, Color c3, Color c4, CallbackInfo ci) {
        HudPreviewCapture.markNonText();
    }

    @Inject(method = "triangle(DDDDDDLmeteordevelopment/meteorclient/utils/render/color/Color;)V", at = @At("HEAD"))
    private void meteorwebgui$markTriangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color, CallbackInfo ci) {
        HudPreviewCapture.markNonText();
    }

    @Inject(method = "texture(Lnet/minecraft/resources/Identifier;DDDDLmeteordevelopment/meteorclient/utils/render/color/Color;)V", at = @At("HEAD"))
    private void meteorwebgui$markTexture(Identifier texture, double x, double y, double width, double height, Color color, CallbackInfo ci) {
        HudPreviewCapture.markNonText();
    }

    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;IIFZLjava/lang/String;)V", at = @At("HEAD"))
    private void meteorwebgui$markItem(ItemStack stack, int x, int y, float scale, boolean overlay, String countLabel, CallbackInfo ci) {
        HudPreviewCapture.markNonText();
    }

    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;IIFZ)V", at = @At("HEAD"))
    private void meteorwebgui$markItemSimple(ItemStack stack, int x, int y, float scale, boolean overlay, CallbackInfo ci) {
        HudPreviewCapture.markNonText();
    }

    @Inject(method = "entity(Lnet/minecraft/world/entity/LivingEntity;IIIIFF)V", at = @At("HEAD"))
    private void meteorwebgui$markEntity(LivingEntity entity, int x, int y, int sizeX, int sizeY, float pitch, float yaw, CallbackInfo ci) {
        HudPreviewCapture.markNonText();
    }
}
