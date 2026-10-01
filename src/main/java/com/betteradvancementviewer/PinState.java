package com.betteradvancementviewer;

import net.minecraft.client.gui.screens.advancements.AdvancementWidget;

public final class PinState {
	private static final int SCROLL_STEP = 20;

	public record Rect(int x, int y, int width, int height) {
		static final Rect NONE = new Rect(0, 0, 0, 0);

		public boolean contains(double px, double py) {
			return width > 0 && px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	public static AdvancementWidget pinned;
	public static boolean showAll;
	public static int scrollX;
	public static String query = "";
	public static boolean searchFocused;

	// Hit areas from the last frame, in screen coordinates
	private static Rect panel = Rect.NONE;
	private static Rect footer = Rect.NONE;
	private static Rect searchBox = Rect.NONE;
	private static Rect scrollbar = Rect.NONE;
	private static int thumbWidth;
	private static int maxScroll;

	private static boolean dragging;
	private static int dragOffset;

	private PinState() {}

	public static void pin(AdvancementWidget widget) {
		clear();
		pinned = widget;
	}

	public static void clear() {
		pinned = null;
		showAll = false;
		scrollX = 0;
		query = "";
		searchFocused = false;
		dragging = false;
		clearHitAreas();
	}

	public static void toggleShowAll() {
		showAll = !showAll;
		scrollX = 0;
	}

	public static void type(String text) {
		query += text;
		scrollX = 0;
	}

	public static void backspace() {
		if (query.isEmpty()) return;
		query = query.substring(0, query.length() - 1);
		scrollX = 0;
	}

	public static void clearHitAreas() {
		panel = Rect.NONE;
		footer = Rect.NONE;
		searchBox = Rect.NONE;
		scrollbar = Rect.NONE;
		maxScroll = 0;
	}

	public static void setPanel(Rect rect) {
		panel = rect;
	}

	public static void setFooter(Rect rect) {
		footer = rect;
	}

	public static void setSearchBox(Rect rect) {
		searchBox = rect;
	}

	public static void setScrollbar(Rect bar, int thumb, int max) {
		scrollbar = bar;
		thumbWidth = thumb;
		maxScroll = max;
		scrollX = Math.clamp(scrollX, 0, max);
	}

	public static boolean overFooter(double x, double y) {
		return footer.contains(x, y);
	}

	public static boolean overSearchBox(double x, double y) {
		return searchBox.contains(x, y);
	}

	public static int thumbX() {
		if (maxScroll == 0) return scrollbar.x();
		return scrollbar.x() + scrollX * (scrollbar.width() - thumbWidth) / maxScroll;
	}

	public static boolean isDragging() {
		return dragging;
	}

	public static boolean startDrag(double mouseX, double mouseY) {
		if (!scrollbar.contains(mouseX, mouseY)) return false;
		int thumbX = thumbX();
		boolean onThumb = mouseX >= thumbX && mouseX < thumbX + thumbWidth;
		dragOffset = onThumb ? (int) mouseX - thumbX : thumbWidth / 2;
		dragging = true;
		dragTo(mouseX);
		return true;
	}

	public static void dragTo(double mouseX) {
		int track = scrollbar.width() - thumbWidth;
		if (track <= 0) return;
		scrollX = Math.clamp(((int) mouseX - dragOffset - scrollbar.x()) * maxScroll / track, 0, maxScroll);
	}

	public static void stopDrag() {
		dragging = false;
	}

	public static boolean scroll(double mouseX, double mouseY, double amount) {
		if (maxScroll == 0 || !panel.contains(mouseX, mouseY)) return false;
		scrollX = Math.clamp(scrollX - Math.round(amount * SCROLL_STEP), 0, maxScroll);
		return true;
	}
}