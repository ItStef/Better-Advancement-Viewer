package com.betteradvancementviewer;

import com.betteradvancementviewer.mixin.AdvancementProgressAccessor;
import com.betteradvancementviewer.mixin.AdvancementWidgetAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// Draws a panel next to the advancements window listing the criteria the hovered advancement still needs.
public final class MissingCriteriaPanel {
	private static final int PADDING = 4;
	private static final int MARGIN = 4;
	private static final int ICON_SIZE = 18;
	private static final int COLUMN_GAP = 8;

	private record Entry(Component label, ItemStack icon) {}

	private MissingCriteriaPanel() {}

	public static void render(GuiGraphicsExtractor graphics, AdvancementWidget widget, int leftPos, int topPos, int screenWidth, int screenHeight) {
		List<Entry> entries = collectMissing(((AdvancementWidgetAccessor) widget).bav$getProgress());
		if (entries.isEmpty()) return;

		Font font = Minecraft.getInstance().font;
		Component header = Component.translatable("better_advancement_viewer.missing", entries.size()).withStyle(ChatFormatting.GOLD);

		boolean hasIcons = entries.stream().anyMatch(e -> !e.icon().isEmpty());
		int iconWidth = hasIcons ? ICON_SIZE : 0;
		int rowHeight = hasIcons ? ICON_SIZE : font.lineHeight + 2;
		int headerHeight = font.lineHeight + 4;

		// Split into as many columns as needed to fit the screen height
		int availableHeight = screenHeight - 2 * MARGIN - 2 * PADDING - headerHeight;
		int maxRows = Math.max(1, availableHeight / rowHeight);
		int columns = (entries.size() + maxRows - 1) / maxRows;
		int rows = (entries.size() + columns - 1) / columns;

		int labelWidth = 0;
		for (Entry entry : entries) labelWidth = Math.max(labelWidth, font.width(entry.label()));
		int columnWidth = iconWidth + labelWidth;

		int panelWidth = Math.max(font.width(header), columns * columnWidth + (columns - 1) * COLUMN_GAP) + 2 * PADDING;
		int panelHeight = headerHeight + rows * rowHeight + 2 * PADDING;

		// Prefer the right side of the advancements window, then the left, then just squeeze it on screen
		int x = leftPos + AdvancementsScreen.WINDOW_WIDTH + MARGIN;
		if (x + panelWidth > screenWidth - MARGIN) {
			int leftX = leftPos - MARGIN - panelWidth;
			x = leftX >= MARGIN ? leftX : Math.max(MARGIN, screenWidth - MARGIN - panelWidth);
		}
		int y = Math.max(MARGIN, Math.min(topPos, screenHeight - MARGIN - panelHeight));

		graphics.nextStratum();
		drawBackground(graphics, x, y, panelWidth, panelHeight);
		graphics.text(font, header, x + PADDING, y + PADDING, 0xFFFFFFFF, true);

		int contentTop = y + PADDING + headerHeight;
		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			int entryX = x + PADDING + (i / rows) * (columnWidth + COLUMN_GAP);
			int entryY = contentTop + (i % rows) * rowHeight;
			if (!entry.icon().isEmpty()) graphics.fakeItem(entry.icon(), entryX, entryY + (rowHeight - 16) / 2);
			graphics.text(font, entry.label(), entryX + iconWidth, entryY + (rowHeight - font.lineHeight) / 2 + 1, 0xFFFFFFFF, true);
		}
	}


	private static List<Entry> collectMissing(AdvancementProgress progress) {
		List<Entry> missing = new ArrayList<>();
		if (progress == null || progress.isDone()) return missing;

		AdvancementRequirements requirements = ((AdvancementProgressAccessor) progress).bav$getRequirements();
		if (requirements == null || requirements.size() <= 1) return missing;

		for (List<String> group : requirements.requirements()) {
			if (group.stream().anyMatch(name -> isDone(progress, name))) continue;

			Entry first = resolve(group.getFirst());
			if (group.size() == 1) {
				missing.add(first);
			} else {
				MutableComponent label = Component.empty().append(first.label());
				for (String alternative : group.subList(1, group.size())) {
					label.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY)).append(resolve(alternative).label());
				}
				missing.add(new Entry(label, first.icon()));
			}
		}
		missing.sort((a, b) -> a.label().getString().compareToIgnoreCase(b.label().getString()));
		return missing;
	}

	private static boolean isDone(AdvancementProgress progress, String criterion) {
		CriterionProgress criterionProgress = progress.getCriterion(criterion);
		return criterionProgress != null && criterionProgress.isDone();
	}

	// Turns a criterion name like "apple", "minecraft:zombie" or "minecraft:plains" into a readable label and icon.
	private static Entry resolve(String criterion) {
		Identifier id = Identifier.tryParse(criterion);
		if (id == null) return new Entry(Component.literal(prettify(criterion)), ItemStack.EMPTY);

		if (BuiltInRegistries.ITEM.containsKey(id)) {
			ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
			return new Entry(stack.getHoverName(), stack);
		}

		if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
			Identifier eggId = id.withSuffix("_spawn_egg");
			ItemStack egg = BuiltInRegistries.ITEM.containsKey(eggId) ? new ItemStack(BuiltInRegistries.ITEM.getValue(eggId)) : ItemStack.EMPTY;
			return new Entry(BuiltInRegistries.ENTITY_TYPE.getValue(id).getDescription(), egg);
		}

		// Biomes are data-driven, so look them up by translation key instead of a registry
		String biomeKey = "biome." + id.getNamespace() + "." + id.getPath();
		if (Language.getInstance().has(biomeKey)) return new Entry(Component.translatable(biomeKey), ItemStack.EMPTY);

		return new Entry(Component.literal(prettify(id.getPath())), ItemStack.EMPTY);
	}

	// "minecraft:some/thing_name" -> "Thing Name" 
	private static String prettify(String raw) {
		String path = raw.substring(Math.max(raw.lastIndexOf(':'), raw.lastIndexOf('/')) + 1);
		StringBuilder out = new StringBuilder();
		for (String word : path.split("_")) {
			if (word.isEmpty()) continue;
			if (!out.isEmpty()) out.append(' ');
			out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
		}
		return out.toString();
	}

	private static void drawBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		graphics.fill(x, y, x + width, y + height, 0xF0100010);
		int border = 0xFF5000A0;
		graphics.fill(x, y, x + width, y + 1, border);
		graphics.fill(x, y + height - 1, x + width, y + height, border);
		graphics.fill(x, y, x + 1, y + height, border);
		graphics.fill(x + width - 1, y, x + width, y + height, border);
	}
}
