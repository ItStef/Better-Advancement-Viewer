package com.betteradvancementviewer.mixin;

import com.betteradvancementviewer.MissingCriteriaPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin {
	@Shadow private int leftPos;
	@Shadow private int topPos;
	@Shadow private AdvancementTab selectedTab;

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void bav$drawMissingCriteria(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (selectedTab == null) return;
		AdvancementWidget hovered = ((AdvancementTabAccessor) selectedTab).bav$getHovered();
		if (hovered == null) return;

		AdvancementsScreen self = (AdvancementsScreen) (Object) this;
		MissingCriteriaPanel.render(graphics, hovered, leftPos, topPos, self.width, self.height);
	}
}
