package com.betteradvancementviewer.mixin;

import com.betteradvancementviewer.PinState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen {
	@Shadow private AdvancementTab selectedTab;

	protected AdvancementsScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void bav$resetPin(CallbackInfo ci) {
		PinState.clear();
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void bav$handleClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && PinState.pinned != null) {
			PinState.searchFocused = PinState.overSearchBox(event.x(), event.y());
			if (PinState.searchFocused || PinState.startDrag(event.x(), event.y())) {
				cir.setReturnValue(true);
				return;
			}
			if (PinState.overFooter(event.x(), event.y())) {
				PinState.toggleShowAll();
				cir.setReturnValue(true);
				return;
			}
		}

		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;

		if (PinState.pinned != null) {
			PinState.clear();
			cir.setReturnValue(true);
			return;
		}

		if (selectedTab == null) return;
		AdvancementWidget hovered = ((AdvancementTabAccessor) selectedTab).bav$getHovered();
		if (hovered == null) return;

		PinState.pin(hovered);
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void bav$dragScrollbar(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
		if (!PinState.isDragging()) return;
		PinState.dragTo(event.x());
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"))
	private void bav$stopDrag(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
		PinState.stopDrag();
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void bav$scrollPanel(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
		if (PinState.pinned != null && PinState.scroll(mouseX, mouseY, scrollY)) cir.setReturnValue(true);
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void bav$typeInSearch(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (!PinState.searchFocused) return;
		if (event.key() == GLFW.GLFW_KEY_BACKSPACE) PinState.backspace();
		else if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) PinState.searchFocused = false;
		cir.setReturnValue(true);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (PinState.searchFocused && event.isAllowedChatCharacter()) {
			PinState.type(event.codepointAsString());
			return true;
		}
		return super.charTyped(event);
	}
}