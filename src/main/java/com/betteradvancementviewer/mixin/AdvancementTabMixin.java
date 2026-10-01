package com.betteradvancementviewer.mixin;

import com.betteradvancementviewer.PinState;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementTab.class)
public abstract class AdvancementTabMixin {
	@Shadow @Final private Map<AdvancementHolder, AdvancementWidget> widgets;
	@Shadow private AdvancementWidget hovered;

	@Inject(method = "tick", at = @At("TAIL"))
	private void bav$keepPinnedHovered(int mouseX, int mouseY, CallbackInfo ci) {
		if (PinState.pinned == null) return;
		hovered = widgets.containsValue(PinState.pinned) ? PinState.pinned : null;
	}
}