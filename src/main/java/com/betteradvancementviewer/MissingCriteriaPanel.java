package com.betteradvancementviewer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

// Draws a panel next to the advancements window listing the criteria the hovered advancement still needs.
public final class MissingCriteriaPanel {
	private static final int MARGIN = 8;
	private static final int ROW_HEIGHT = 18;
	private static final int COLUMN_GAP = 8;

	private record Entry(Component label, ItemStack icon) {}

	private MissingCriteriaPanel() {}

	public static void render(GuiGraphicsExtractor graphics, AdvancementProgress progress, int leftPos, int topPos) {
		// getProgressText() is null for single-requirement advancements, which vanilla already explains
		if (progress == null || progress.isDone() || progress.getProgressText() == null) return;

		List<Entry> entries = new ArrayList<>();
		for (String criterion : progress.getRemainingCriteria()) entries.add(resolve(criterion));
		entries.sort(Comparator.comparing(e -> e.label().getString(), String.CASE_INSENSITIVE_ORDER));

		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		int screenWidth = minecraft.getWindow().getGuiScaledWidth();
		int screenHeight = minecraft.getWindow().getGuiScaledHeight();

		Component header = Component.literal("Still missing (" + entries.size() + ")").withStyle(ChatFormatting.GOLD);
		int headerHeight = font.lineHeight + 4;

		// Split into as many columns as needed to fit the screen height
		int maxRows = Math.max(1, (screenHeight - 2 * MARGIN - headerHeight) / ROW_HEIGHT);
		int columns = (entries.size() + maxRows - 1) / maxRows;
		int rows = (entries.size() + columns - 1) / columns;

		int labelWidth = 0;
		for (Entry entry : entries) labelWidth = Math.max(labelWidth, font.width(entry.label()));
		int columnWidth = ROW_HEIGHT + labelWidth;

		int width = Math.max(font.width(header), columns * columnWidth + (columns - 1) * COLUMN_GAP);
		int height = headerHeight + rows * ROW_HEIGHT;
		int x = Math.min(leftPos + AdvancementsScreen.WINDOW_WIDTH + MARGIN, screenWidth - width - MARGIN);
		int y = Math.min(topPos, screenHeight - height - MARGIN);

		// extractHover runs with the pose shifted into the window's inner area; draw in screen space instead
		graphics.pose().pushMatrix().identity();
		TooltipRenderUtil.extractTooltipBackground(graphics, x, y, width, height, null);
		graphics.text(font, header, x, y, 0xFFFFFFFF, true);

		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			int entryX = x + (i / rows) * (columnWidth + COLUMN_GAP);
			int entryY = y + headerHeight + (i % rows) * ROW_HEIGHT;
			if (!entry.icon().isEmpty()) graphics.fakeItem(entry.icon(), entryX, entryY + 1);
			graphics.text(font, entry.label(), entryX + ROW_HEIGHT, entryY + 5, 0xFFFFFFFF, true);
		}
		graphics.pose().popMatrix();
	}

	// Turns a criterion name like "apple", "minecraft:zombie" or "minecraft:plains" into a readable label and icon.
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

		// Biomes are data-driven, so look them up by translation key instead of a registry
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