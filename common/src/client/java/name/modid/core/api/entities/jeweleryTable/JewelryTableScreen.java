package name.modid.core.api.entities.jeweleryTable;

import java.util.ArrayList;
import java.util.List;

import name.modid.Gemstones;
import name.modid.core.api.modifiers.helpers.GemstoneSlotHelper;
import name.modid.core.api.tooltips.TooltipHelper.InlineIcons;
import name.modid.core.api.tooltips.TooltipText;
import name.modid.core.content.items.registries.ItemsRegistry;
import name.modid.core.content.screen.JewelryTableScreenHandler;
import name.modid.core.content.screen.JewelryTableScreenHandler.SocketAction;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class JewelryTableScreen extends HandledScreen<JewelryTableScreenHandler> {
  public static final Identifier GUI_TEXTURE = Identifier.of(Gemstones.MOD_ID,
      "textures/gui/jewelry_table/gui.png");

  private static final int BACKGROUND_WIDTH = 176;
  private static final int BACKGROUND_HEIGHT = 166;

  private static final int SLOT_SIZE = 18;
  private static final int SOCKET_SPRITES_V = 208;
  private static final int SLOT_FRAME_U = 0;
  private static final int PLUS_BUTTON_U = 18;
  private static final int LOCK_BUTTON_U = 36;
  private static final int INACTIVE_SOCKET_U = 54;
  private static final int TOOL_SPRITES_V = 226;
  private static final int TOOL_SILHOUETTES_U = 18;

  private static final int TOOL_PANEL_X = -21;
  private static final int TOOL_PANEL_U = 176;
  private static final int TOOL_PANEL_WIDTH = 25;
  private static final int TOOL_PANEL_HEIGHT = 81;

  private static final int TOGGLE_CLOSED_X = -28;
  private static final int TOGGLE_OPEN_X = -49;
  private static final int TOGGLE_Y = 3;
  private static final int TOGGLE_U = 176;
  private static final int TOGGLE_OPEN_V = 84;
  private static final int TOGGLE_CLOSED_V = 110;
  private static final int TOGGLE_WIDTH = 32;
  private static final int TOGGLE_HEIGHT = 26;
  // The rest of the sprite goes under the window or merges with the panel
  private static final int TOGGLE_VISIBLE_WIDTH = 28;
  private static final int TOGGLE_ITEM_X = 7;
  private static final int TOGGLE_ITEM_Y = 5;

  private static final int TITLE_Y = 7;
  private static final int TITLE_COLOR = 0xF0E0C0;

  private static final int SOCKETS_Y = 26;
  private static final int SOCKETS_PER_ROW = 8;

  private static final int AFFORDABLE_COLOR = 0x80FF20;
  private static final int TOO_EXPENSIVE_COLOR = 0xFF6060;
  private static final int TOOLTIP_MAX_WIDTH = 340;

  // Survives reopening the table
  private static boolean toolPanelOpen = true;

  private final ItemStack toggleIcon = new ItemStack(ItemsRegistry.jewelryHammer());

  public JewelryTableScreen(JewelryTableScreenHandler handler, PlayerInventory inventory, Text title) {
    super(handler, inventory, title);

    this.backgroundWidth = BACKGROUND_WIDTH;
    this.backgroundHeight = BACKGROUND_HEIGHT;
    handler.setToolPanelOpen(toolPanelOpen);
  }

  @Override
  protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
    // Like a creative tab: an inactive one lies behind the window, an active one on top of it
    if (!toolPanelOpen) {
      drawToggle(context);
    }

    context.drawTexture(GUI_TEXTURE, this.x, this.y, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);

    if (toolPanelOpen) {
      drawToolPanel(context);
      drawToggle(context);
    }

    Slot baseSlot = this.handler.getSlot(JewelryTableScreenHandler.SLOT_BASE);
    drawSocketSprite(context, SLOT_FRAME_U, this.x + baseSlot.x - 1, this.y + baseSlot.y - 1);
    drawSockets(context, mouseX, mouseY);
  }

  private void drawToolPanel(DrawContext context) {
    context.drawTexture(GUI_TEXTURE, this.x + TOOL_PANEL_X, this.y, TOOL_PANEL_U, 0, TOOL_PANEL_WIDTH,
        TOOL_PANEL_HEIGHT);

    for (int i = 0; i < JewelryTableScreenHandler.TOOL_SLOTS.length; i++) {
      Slot slot = this.handler.getSlot(JewelryTableScreenHandler.TOOL_SLOTS[i]);
      int u = slot.hasStack() ? SLOT_FRAME_U : TOOL_SILHOUETTES_U + i * SLOT_SIZE;
      context.drawTexture(GUI_TEXTURE, this.x + slot.x - 1, this.y + slot.y - 1, u, TOOL_SPRITES_V, SLOT_SIZE,
          SLOT_SIZE);
    }
  }

  private void drawToggle(DrawContext context) {
    int x = getToggleX();
    int y = this.y + TOGGLE_Y;
    int v = toolPanelOpen ? TOGGLE_OPEN_V : TOGGLE_CLOSED_V;

    context.drawTexture(GUI_TEXTURE, x, y, TOGGLE_U, v, TOGGLE_WIDTH, TOGGLE_HEIGHT);
    context.drawItem(this.toggleIcon, x + TOGGLE_ITEM_X, y + TOGGLE_ITEM_Y);
  }

  private void drawSockets(DrawContext context, int mouseX, int mouseY) {
    int hovered = getSocketAt(mouseX, mouseY);
    boolean hasItem = this.handler.getSocketCount() > 0;

    for (int i = 0; i < GemstoneSlotHelper.getMaxSlots(); i++) {
      int x = getSocketX(i);
      int y = getSocketY(i);
      if (!hasItem) {
        drawSocketSprite(context, INACTIVE_SOCKET_U, x, y);
        continue;
      }

      SocketAction action = this.handler.getSocketAction(i);

      drawSocketSprite(context, switch (action) {
        case INSERT -> PLUS_BUTTON_U;
        case UNLOCK -> LOCK_BUTTON_U;
        default -> SLOT_FRAME_U;
      }, x, y);

      if (action == SocketAction.EXTRACT) {
        context.drawItem(this.handler.getSocketedGem(i), x + 1, y + 1);
      }
      if (i == hovered) {
        drawSlotHighlight(context, x + 1, y + 1, 0);
      }
    }
  }

  private void drawSocketSprite(DrawContext context, int u, int x, int y) {
    context.drawTexture(GUI_TEXTURE, x, y, u, SOCKET_SPRITES_V, SLOT_SIZE, SLOT_SIZE);
  }

  private int getToggleX() {
    return this.x + (toolPanelOpen ? TOGGLE_OPEN_X : TOGGLE_CLOSED_X);
  }

  private int getSocketX(int index) {
    int row = index / SOCKETS_PER_ROW;
    int inRow = Math.min(SOCKETS_PER_ROW, GemstoneSlotHelper.getMaxSlots() - row * SOCKETS_PER_ROW);
    return this.x + (BACKGROUND_WIDTH - inRow * SLOT_SIZE) / 2 + index % SOCKETS_PER_ROW * SLOT_SIZE;
  }

  // Extra rows grow upwards, the last row stays right above the item
  private int getSocketY(int index) {
    int rows = (GemstoneSlotHelper.getMaxSlots() + SOCKETS_PER_ROW - 1) / SOCKETS_PER_ROW;
    return this.y + SOCKETS_Y - (rows - 1 - index / SOCKETS_PER_ROW) * SLOT_SIZE;
  }

  private int getSocketAt(double mouseX, double mouseY) {
    for (int i = 0; i < this.handler.getSocketCount(); i++) {
      if (isOver(mouseX, mouseY, getSocketX(i), getSocketY(i), SLOT_SIZE, SLOT_SIZE)) {
        return i;
      }
    }
    return -1;
  }

  private boolean isOverToggle(double mouseX, double mouseY) {
    return isOver(mouseX, mouseY, getToggleX(), this.y + TOGGLE_Y, TOGGLE_VISIBLE_WIDTH, TOGGLE_HEIGHT);
  }

  private boolean isOverToolPanel(double mouseX, double mouseY) {
    return toolPanelOpen
        && isOver(mouseX, mouseY, this.x + TOOL_PANEL_X, this.y, TOOL_PANEL_WIDTH, TOOL_PANEL_HEIGHT);
  }

  private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
    return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
  }

  /** The part of the screen sticking out to the left of the main window */
  public ScreenRect getSideBounds() {
    int left = getToggleX();
    return new ScreenRect(left, this.y, this.x - left, toolPanelOpen ? TOOL_PANEL_HEIGHT : TOGGLE_Y + TOGGLE_HEIGHT);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == 0) {
      if (isOverToggle(mouseX, mouseY)) {
        toolPanelOpen = !toolPanelOpen;
        this.handler.setToolPanelOpen(toolPanelOpen);
        this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        return true;
      }

      int socket = getSocketAt(mouseX, mouseY);
      if (socket >= 0) {
        if (this.handler.canUseSocket(this.client.player, socket)) {
          this.client.interactionManager.clickButton(this.handler.syncId, socket);
        }
        return true;
      }
    }

    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
    return super.isClickOutsideBounds(mouseX, mouseY, left, top, button)
        && !isOverToggle(mouseX, mouseY)
        && !isOverToolPanel(mouseX, mouseY);
  }

  @Override
  protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
    int titleX = (BACKGROUND_WIDTH - this.textRenderer.getWidth(this.title)) / 2;
    context.drawText(this.textRenderer, this.title, titleX, TITLE_Y, TITLE_COLOR, true);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    drawMouseoverTooltip(context, mouseX, mouseY);

    if (isOverToggle(mouseX, mouseY)) {
      context.drawTooltip(this.textRenderer, Text.translatable("container.gemstones.jewelry_table.tools"), mouseX,
          mouseY);
      return;
    }

    int socket = getSocketAt(mouseX, mouseY);
    if (socket >= 0) {
      drawSocketTooltip(context, socket, mouseX, mouseY);
    }
  }

  private void drawSocketTooltip(DrawContext context, int socket, int mouseX, int mouseY) {
    List<Text> lines = switch (this.handler.getSocketAction(socket)) {
      case INSERT -> getInsertTooltip();
      case EXTRACT -> getExtractTooltip(socket);
      case UNLOCK -> getUnlockTooltip();
      case NONE -> List.of();
    };

    List<OrderedText> wrapped = new ArrayList<>();
    for (Text line : lines) {
      if (line.getString().isEmpty()) {
        wrapped.add(OrderedText.EMPTY);
      } else {
        wrapped.addAll(this.textRenderer.wrapLines(line, TOOLTIP_MAX_WIDTH));
      }
    }

    context.drawOrderedTooltip(this.textRenderer, wrapped, mouseX, mouseY);
  }

  private List<Text> getInsertTooltip() {
    ItemStack gem = this.handler.getCursorStack();
    List<Text> lines = new ArrayList<>(List.of(
        title("insert"),
        Text.translatable("container.gemstones.jewelry_table.insert.info",
            TooltipText.icon(InlineIcons.MOUSE_DRAG),
            action("insert"))));

    boolean holdsGem = this.handler.getInsertLevelCost(gem) > 0;
    Text levels = holdsGem
        ? levelCost(this.handler.getInsertLevelCost(gem))
        : TooltipText.line(TooltipText.icon(InlineIcons.EXPERIENCE),
            Text.literal(" "),
            Text.translatable("item.gemstones.jewelry_pliers.info_levels")
                .styled(style -> style.withColor(AFFORDABLE_COLOR)));
    addRequired(lines, levels, tool(JewelryTableScreenHandler.SLOT_HAMMER, ItemsRegistry.jewelryHammer().getName()));

    if (holdsGem && !this.handler.canInsertGem(gem)) {
      lines.add(Text.empty());
      lines.add(Text.translatable("container.gemstones.jewelry_table.insert.unsuitable").formatted(Formatting.RED));
    }

    return lines;
  }

  private List<Text> getExtractTooltip(int socket) {
    List<Text> lines = new ArrayList<>(List.of(
        title("extract"),
        Text.translatable("container.gemstones.jewelry_table.extract.info",
            TooltipText.icon(InlineIcons.MOUSE_LMB),
            action("extract"),
            this.handler.getSocketedGem(socket).toHoverableText())));

    addRequired(lines, levelCost(this.handler.getExtractLevelCost(socket)),
        tool(JewelryTableScreenHandler.SLOT_PLIERS, ItemsRegistry.jewelryPliers().getName()));

    return lines;
  }

  private List<Text> getUnlockTooltip() {
    List<Text> lines = new ArrayList<>(List.of(
        title("unlock"),
        Text.translatable("container.gemstones.jewelry_table.unlock.info",
            TooltipText.icon(InlineIcons.MOUSE_LMB),
            action("unlock"))));

    addRequired(lines, tool(JewelryTableScreenHandler.SLOT_CRYSTAL, ItemsRegistry.expansionCrystal().getName()),
        tool(JewelryTableScreenHandler.SLOT_CHISEL, Text.translatable("container.gemstones.jewelry_table.chisel")));

    return lines;
  }

  private static Text title(String action) {
    return Text.translatable("container.gemstones.jewelry_table." + action + ".title").formatted(Formatting.YELLOW);
  }

  private static Text action(String action) {
    return Text.translatable("container.gemstones.jewelry_table." + action + ".action").formatted(Formatting.GOLD);
  }

  private static void addRequired(List<Text> lines, Text... requirements) {
    lines.add(Text.empty());
    lines.add(Text.translatable("container.gemstones.jewelry_table.required").formatted(Formatting.GRAY));
    for (Text requirement : requirements) {
      lines.add(TooltipText.line(
          Text.translatable("tooltip.gemstones.category_dot").formatted(Formatting.DARK_GRAY),
          requirement));
    }
  }

  private Text levelCost(int cost) {
    int color = this.handler.canAfford(this.client.player, cost) ? AFFORDABLE_COLOR : TOO_EXPENSIVE_COLOR;
    MutableText levels = Text.translatable(
        cost == 1 ? "container.gemstones.jewelry_table.level" : "container.gemstones.jewelry_table.levels")
        .styled(style -> style.withColor(color));
    return TooltipText.line(
        Text.literal(cost + " ").styled(style -> style.withColor(color)),
        TooltipText.icon(InlineIcons.EXPERIENCE),
        Text.literal(" "),
        levels);
  }

  private Text tool(int slot, Text missingName) {
    ItemStack stack = this.handler.getInventory().getStack(slot);
    return stack.isEmpty() ? missingName.copy().formatted(Formatting.RED) : stack.toHoverableText();
  }
}
