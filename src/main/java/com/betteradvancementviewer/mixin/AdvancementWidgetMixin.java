package com.betteradvancementviewer.mixin;

import com.betteradvancementviewer.MissingCriteriaPanel;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {
	@Shadow private AdvancementProgress progress;

	// Vanilla only calls extractHover on the hovered widget; the last two ints are the screen's leftPos/topPos
	@Inject(method = "extractHover", at = @At("TAIL"))
	private void bav$drawMissingCriteria(GuiGraphicsExtractor graphics, int scrollX, int scrollY, float fade, int leftPos, int topPos, CallbackInfo ci) {
		MissingCriteriaPanel.render(graphics, progress, leftPos, topPos);
	}
}