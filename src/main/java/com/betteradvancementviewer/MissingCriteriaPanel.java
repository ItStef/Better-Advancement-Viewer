package com.betteradvancementviewer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public final class MissingCriteriaPanel {
	private static final int MARGIN = 8;
	private static final int ROW_HEIGHT = 18;
	private static final int COLUMN_GAP = 8;
	private static final int MAX_SHOWN = 20;
	private static final int SCROLLBAR_HEIGHT = 4;
	private static final int SCROLLBAR_SPACE = SCROLLBAR_HEIGHT + 4;
	private static final int SEARCH_HEIGHT = 12;
	private static final int SEARCH_SPACE = SEARCH_HEIGHT + 4;
	private static final int SEARCH_MIN_WIDTH = 100;

	private record Entry(Component label, ItemStack icon) {}

	private MissingCriteriaPanel() {}

	public static void render(GuiGraphicsExtractor graphics, AdvancementProgress progress, int leftPos, int topPos, boolean pinned) {
		PinState.clearHitAreas();
		if (progress == null || progress.isDone() || progress.getProgressText() == null) return;

		String query = pinned ? PinState.query.toLowerCase(Locale.ROOT) : "";
		boolean searching = !query.isEmpty();
		List<Entry> missing = collect(progress.getRemainingCriteria(), false, query);
		List<Entry> done = collect(progress.getCompletedCriteria(), true, query);
		boolean showAll = pinned && (PinState.showAll || searching);

		int shownMissing = showAll ? missing.size() : Math.min(MAX_SHOWN, missing.size());
		List<Entry> entries = new ArrayList<>(missing.subList(0, shownMissing));
		if (showAll) entries.addAll(done);
		if (entries.isEmpty()) entries.add(new Entry(Component.literal("No matches").withStyle(ChatFormatting.GRAY), ItemStack.EMPTY));

		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		int screenWidth = minecraft.getWindow().getGuiScaledWidth();
		int screenHeight = minecraft.getWindow().getGuiScaledHeight();

		MutableComponent header = Component.literal("Still missing (" + missing.size() + ")").withStyle(ChatFormatting.GOLD);
		if (!done.isEmpty()) header.append(Component.literal("  " + done.size() + " done").withStyle(ChatFormatting.GRAY));
		int headerHeight = font.lineHeight + 4;
		int searchSpace = pinned ? SEARCH_SPACE : 0;
		Component footer = searching ? null : footerText(missing.size() - shownMissing, done.size(), pinned, showAll);
		int footerHeight = footer != null ? font.lineHeight + 2 : 0;

		int reserved = searchSpace + headerHeight + footerHeight + (showAll ? SCROLLBAR_SPACE : 0);
		int maxRows = Math.max(1, (screenHeight - 2 * MARGIN - reserved) / ROW_HEIGHT);
		int columns = (entries.size() + maxRows - 1) / maxRows;
		int rows = (entries.size() + columns - 1) / columns;

		int labelWidth = 0;
		for (Entry entry : entries) labelWidth = Math.max(labelWidth, font.width(entry.label()));
		int columnWidth = ROW_HEIGHT + labelWidth;
		int contentWidth = columns * columnWidth + (columns - 1) * COLUMN_GAP;

		int minWidth = Math.max(font.width(header), footer != null ? font.width(footer) : 0);
		if (pinned) minWidth = Math.max(minWidth, SEARCH_MIN_WIDTH);
		int viewWidth = Math.min(Math.max(contentWidth, minWidth), screenWidth - 2 * MARGIN);
		int maxScroll = Math.max(0, contentWidth - viewWidth);
		int scrollbarSpace = maxScroll > 0 ? SCROLLBAR_SPACE : 0;

		int height = searchSpace + headerHeight + rows * ROW_HEIGHT + scrollbarSpace + footerHeight;
		int x = Math.max(MARGIN, Math.min(leftPos + AdvancementsScreen.WINDOW_WIDTH + MARGIN, screenWidth - viewWidth - MARGIN));
		int y = Math.max(MARGIN, Math.min(topPos, screenHeight - height - MARGIN));
		int headerY = y + searchSpace;
		int contentTop = headerY + headerHeight;
		int contentBottom = contentTop + rows * ROW_HEIGHT;

		int thumbWidth = maxScroll > 0 ? Math.max(16, viewWidth * viewWidth / contentWidth) : 0;
		PinState.setScrollbar(new PinState.Rect(x, contentBottom + 2, viewWidth, SCROLLBAR_HEIGHT), thumbWidth, maxScroll);
		int scroll = maxScroll > 0 ? PinState.scrollX : 0;

		// extractHover runs with the pose shifted into the window; draw in screen space instead
		graphics.pose().pushMatrix().identity();
		TooltipRenderUtil.extractTooltipBackground(graphics, x, y, viewWidth, height, null);
		if (pinned) drawSearchBox(graphics, font, x, y, viewWidth);
		graphics.text(font, header, x, headerY, 0xFFFFFFFF, true);

		graphics.enableScissor(x, contentTop, x + viewWidth, contentBottom);
		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			int entryX = x + (i / rows) * (columnWidth + COLUMN_GAP) - scroll;
			if (entryX + columnWidth < x || entryX > x + viewWidth) continue;
			int entryY = contentTop + (i % rows) * ROW_HEIGHT;
			if (!entry.icon().isEmpty()) graphics.fakeItem(entry.icon(), entryX, entryY + 1);
			graphics.text(font, entry.label(), entryX + ROW_HEIGHT, entryY + 5, 0xFFFFFFFF, true);
		}
		graphics.disableScissor();

		if (maxScroll > 0) {
			int barY = contentBottom + 2;
			int thumbX = PinState.thumbX();
			graphics.fill(x, barY, x + viewWidth, barY + SCROLLBAR_HEIGHT, 0xFF202020);
			graphics.fill(thumbX, barY, thumbX + thumbWidth, barY + SCROLLBAR_HEIGHT, PinState.isDragging() ? 0xFFFFFFFF : 0xFFA0A0A0);
		}

		if (footer != null) {
			int footerY = contentBottom + scrollbarSpace + 2;
			graphics.text(font, footer, x, footerY, 0xFFFFFFFF, true);
			if (pinned) PinState.setFooter(new PinState.Rect(x, footerY, font.width(footer), font.lineHeight));
		}
		if (pinned) PinState.setPanel(new PinState.Rect(x, y, viewWidth, height));
		graphics.pose().popMatrix();
	}

	private static void drawSearchBox(GuiGraphicsExtractor graphics, Font font, int x, int y, int width) {
		boolean focused = PinState.searchFocused;
		graphics.fill(x, y, x + width, y + SEARCH_HEIGHT, focused ? 0xFFFFFFFF : 0xFFA0A0A0);
		graphics.fill(x + 1, y + 1, x + width - 1, y + SEARCH_HEIGHT - 1, 0xFF000000);

		if (PinState.query.isEmpty() && !focused) {
			graphics.text(font, "Search...", x + 3, y + 2, 0xFF808080, false);
		} else {
			boolean cursor = focused && System.currentTimeMillis() / 500 % 2 == 0;
			String shown = font.plainSubstrByWidth(PinState.query, width - 12, true);
			graphics.text(font, shown + (cursor ? "_" : ""), x + 3, y + 2, 0xFFFFFFFF, false);
		}
		PinState.setSearchBox(new PinState.Rect(x, y, width, SEARCH_HEIGHT));
	}

	private static Component footerText(int hiddenMissing, int doneCount, boolean pinned, boolean showAll) {
		if (showAll) return Component.literal("Show less").withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
		if (hiddenMissing == 0 && doneCount == 0) return null;

		List<String> parts = new ArrayList<>();
		if (hiddenMissing > 0) parts.add("+" + hiddenMissing + " more");
		if (doneCount > 0) parts.add(doneCount + " done");
		String summary = String.join(", ", parts);

		if (pinned) return Component.literal("Show all (" + summary + ")").withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
		return Component.literal(summary + " - right-click to pin").withStyle(ChatFormatting.GRAY);
	}

	private static List<Entry> collect(Iterable<String> criteria, boolean done, String query) {
		List<Entry> entries = new ArrayList<>();
		for (String criterion : criteria) {
			Entry entry = resolve(criterion);
			if (!query.isEmpty() && !entry.label().getString().toLowerCase(Locale.ROOT).contains(query)) continue;
			entries.add(done ? new Entry(Component.empty().append(entry.label()).withStyle(ChatFormatting.GRAY), entry.icon()) : entry);
		}
		entries.sort(Comparator.comparing(e -> e.label().getString(), String.CASE_INSENSITIVE_ORDER));
		return entries;
	}

	// "apple", "minecraft:zombie", "minecraft:plains" -> readable label + icon
	private static Entry resolve(String criterion) {
		Identifier id = Identifier.tryParse(criterion);
		if (id == null) return new Entry(Component.literal(criterion), ItemStack.EMPTY);

		if (BuiltInRegistries.ITEM.containsKey(id)) {
			ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
			return new Entry(stack.getHoverName(), stack);
		}

		if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
			ItemStack egg = SpawnEggItem.byId(type).map(ItemStack::new).orElse(ItemStack.EMPTY);
			return new Entry(type.getDescription(), egg);
		}

		String biomeKey = id.toLanguageKey("biome");
		if (Language.getInstance().has(biomeKey)) return new Entry(Component.translatable(biomeKey), ItemStack.EMPTY);

		return new Entry(Component.literal(prettify(id.getPath())), ItemStack.EMPTY);
	}

	// "snowy_taiga" -> "Snowy Taiga"
	private static String prettify(String path) {
		StringBuilder out = new StringBuilder();
		for (String word : path.split("_")) {
			if (word.isEmpty()) continue;
			if (!out.isEmpty()) out.append(' ');
			out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return out.toString();
	}
}