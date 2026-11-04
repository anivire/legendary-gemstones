package name.modid.core.content.screen;

import name.modid.core.api.components.GemstoneComponent;
import name.modid.core.api.modifiers.helpers.GemstoneSlotHelper;
import name.modid.core.api.modifiers.helpers.ModifierHelper;
import name.modid.core.api.modifiers.types.GemstoneQuality;
import name.modid.core.api.modifiers.types.GemstoneType;
import name.modid.core.content.blocks.entity.JewelryTableBlockEntity;
import name.modid.core.content.items.GemstoneItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class JewelryTableScreenHandler extends ScreenHandler {
  public static final int SLOT_CHISEL = 0;
  public static final int SLOT_BASE = 1;
  public static final int SLOT_HAMMER = 2;
  public static final int SLOT_PLIERS = 3;
  public static final int SLOT_CRYSTAL = 4;
  // Has no screen slot, keeps a gemstone from a table saved before the tool panel
  public static final int SLOT_LEGACY_GEM = 5;
  public static final int TABLE_INVENTORY_SIZE = 6;
  public static final int[] TOOL_SLOTS = { SLOT_CHISEL, SLOT_HAMMER, SLOT_PLIERS, SLOT_CRYSTAL };
  private static final int TABLE_SLOT_COUNT = 5;

  private static final int TOOL_SLOT_X = -14;
  private static final int TOOL_SLOT_Y = 4;
  private static final int BASE_SLOT_X = 80;
  private static final int BASE_SLOT_Y = 50;
  private static final int PLAYER_INVENTORY_Y = 84;
  private static final int PLAYER_HOTBAR_Y = 142;

  private final Inventory inventory;
  private final BlockPos pos;
  private boolean toolPanelOpen = true;

  public JewelryTableScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
    this(syncId, playerInventory, buf.readBlockPos());
  }

  public JewelryTableScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
    this(syncId, playerInventory, playerInventory.player.getWorld().getBlockEntity(pos));
  }

  public JewelryTableScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
    super(ScreenRegistry.JEWELRY_TABLE_SCREEN_HANDLER.get(), syncId);
    this.inventory = (Inventory) blockEntity;
    this.pos = blockEntity.getPos();

    checkSize(this.inventory, TABLE_INVENTORY_SIZE);

    addTableSlot(SLOT_CHISEL, TOOL_SLOT_X, TOOL_SLOT_Y);
    addTableSlot(SLOT_BASE, BASE_SLOT_X, BASE_SLOT_Y);
    addTableSlot(SLOT_HAMMER, TOOL_SLOT_X, TOOL_SLOT_Y + 18);
    addTableSlot(SLOT_PLIERS, TOOL_SLOT_X, TOOL_SLOT_Y + 36);
    addTableSlot(SLOT_CRYSTAL, TOOL_SLOT_X, TOOL_SLOT_Y + 54);

    addPlayerInventory(playerInventory);
    addPlayerHotbar(playerInventory);

    GemstoneSlotHelper.initializeSocketsIfEligible(getBase(), getBase().getItem());
  }

  private void addTableSlot(int index, int x, int y) {
    this.addSlot(new Slot(this.inventory, index, x, y) {
      @Override
      public boolean canInsert(ItemStack stack) {
        return JewelryTableBlockEntity.canAutomationInsert(index, stack);
      }

      @Override
      public int getMaxItemCount() {
        return index == SLOT_BASE ? 1 : super.getMaxItemCount();
      }

      @Override
      public boolean isEnabled() {
        return index == SLOT_BASE || toolPanelOpen;
      }

      @Override
      public void markDirty() {
        super.markDirty();
        GemstoneSlotHelper.initializeSocketsIfEligible(getBase(), getBase().getItem());
      }
    });
  }

  public enum SocketAction {
    INSERT, EXTRACT, UNLOCK, NONE
  }

  public void setToolPanelOpen(boolean open) {
    this.toolPanelOpen = open;
  }

  public ItemStack getBase() {
    return inventory.getStack(SLOT_BASE);
  }

  public boolean hasTableStack(int slot) {
    return !inventory.getStack(slot).isEmpty();
  }

  public int getSocketCount() {
    ItemStack base = getBase();
    return !base.isEmpty() && GemstoneSlotHelper.isItemValid(base.getItem()) ? GemstoneSlotHelper.getMaxSlots() : 0;
  }

  public SocketAction getSocketAction(int index) {
    if (index < 0 || index >= getSocketCount()) {
      return SocketAction.NONE;
    }

    GemstoneComponent[] gems = GemstoneSlotHelper.getGemstones(getBase());
    if (index >= gems.length || gems[index].gemstoneType() == GemstoneType.LOCKED) {
      return SocketAction.UNLOCK;
    }

    if (gems[index].gemstoneType() == GemstoneType.EMPTY) {
      return SocketAction.INSERT;
    }

    return getSocketedGem(index).isEmpty() ? SocketAction.NONE : SocketAction.EXTRACT;
  }

  public ItemStack getSocketedGem(int index) {
    return GemstoneSlotHelper.makeGemItemFromSocket(getBase(), index);
  }

  public boolean canInsertGem(ItemStack gem) {
    if (!(gem.getItem() instanceof GemstoneItem gemItem)) {
      return false;
    }

    ItemStack base = getBase();
    GemstoneType gemType = gemItem.getType();
    GemstoneQuality gemQuality = gemItem.getRarityType();

    // A gemstone without an effect for this item category would sit in the socket
    // doing nothing
    if (ModifierHelper.getGemstoneModifierForItem(gemType, gemQuality, base.getItem()) == null) {
      return false;
    }

    // Forbid inserting two equal gemstone types with mythic quality
    return gemQuality != GemstoneQuality.MYTHIC
        || GemstoneSlotHelper.contains(base, gemType).stream()
            .noneMatch(socketed -> socketed.gemstoneQualityType() == GemstoneQuality.MYTHIC);
  }

  public int getInsertLevelCost(ItemStack gem) {
    return gem.getItem() instanceof GemstoneItem gemItem ? gemItem.getRarityType().getInsertLevelCost() : 0;
  }

  public int getExtractLevelCost(int index) {
    return getSocketedGem(index).getItem() instanceof GemstoneItem gemItem
        ? gemItem.getRarityType().getRemoveLevelCost()
        : 0;
  }

  public boolean canAfford(PlayerEntity player, int cost) {
    return player.getAbilities().creativeMode || player.experienceLevel >= cost;
  }

  public boolean canUseSocket(PlayerEntity player, int index) {
    return switch (getSocketAction(index)) {
      case INSERT -> hasTableStack(SLOT_HAMMER)
          && canInsertGem(getCursorStack())
          && canAfford(player, getInsertLevelCost(getCursorStack()));
      case EXTRACT -> hasTableStack(SLOT_PLIERS) && canAfford(player, getExtractLevelCost(index));
      case UNLOCK -> hasTableStack(SLOT_CHISEL)
          && hasTableStack(SLOT_CRYSTAL)
          && GemstoneSlotHelper.canAddNewSlot(getBase());
      case NONE -> false;
    };
  }

  @Override
  public boolean onButtonClick(PlayerEntity player, int socket) {
    if (!canUseSocket(player, socket)) {
      return false;
    }

    switch (getSocketAction(socket)) {
      case INSERT -> insertGem(player, socket);
      case EXTRACT -> extractGem(player, socket);
      case UNLOCK -> unlockSocket(player);
      case NONE -> {
        return false;
      }
    }

    inventory.markDirty();
    return true;
  }

  private void insertGem(PlayerEntity player, int socket) {
    ItemStack gem = getCursorStack();

    chargeLevels(player, getInsertLevelCost(gem));
    GemstoneSlotHelper.setGemstoneByIndex(getBase(), socket, (GemstoneItem) gem.getItem());
    gem.decrement(1);

    damageTool(player, SLOT_HAMMER);
    playInsertSound(player);
  }

  private void extractGem(PlayerEntity player, int socket) {
    ItemStack gem = getSocketedGem(socket);

    chargeLevels(player, getExtractLevelCost(socket));
    GemstoneSlotHelper.clearGemstoneAtIndex(getBase(), socket);

    if (getCursorStack().isEmpty()) {
      setCursorStack(gem);
    } else {
      player.getInventory().offerOrDrop(gem);
    }

    damageTool(player, SLOT_PLIERS);
    playRemoveSound(player);
  }

  private void unlockSocket(PlayerEntity player) {
    GemstoneSlotHelper.addNewGemSlot(getBase());
    inventory.getStack(SLOT_CRYSTAL).decrement(1);

    damageTool(player, SLOT_CHISEL);
    playExpandSound(player);
  }

  private void chargeLevels(PlayerEntity player, int cost) {
    if (!player.getAbilities().creativeMode && cost > 0) {
      player.addExperienceLevels(-cost);
    }
  }

  private void damageTool(PlayerEntity player, int slot) {
    inventory.getStack(slot).damage(1, player, EquipmentSlot.MAINHAND);
  }

  private void playInsertSound(PlayerEntity player) {
    if (player.getWorld().isClient)
      return;

    var w = player.getWorld();

    w.playSound(null, pos, SoundEvents.BLOCK_VAULT_INSERT_ITEM, SoundCategory.BLOCKS, 0.55f, 1.35f);
    w.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.BLOCKS, 0.55f, 0.75f);
  }

  private void playExpandSound(PlayerEntity player) {
    if (player.getWorld().isClient)
      return;

    var w = player.getWorld();

    w.playSound(null, pos, SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 0.9f, 0.95f);
    w.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.BLOCKS, 0.55f, 0.75f);
  }

  private void playRemoveSound(PlayerEntity player) {
    if (player.getWorld().isClient)
      return;

    var w = player.getWorld();

    w.playSound(null, pos, SoundEvents.ITEM_BUNDLE_REMOVE_ONE, SoundCategory.BLOCKS, 0.75f, 0.95f);
    w.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.BLOCKS, 0.55f, 0.75f);
  }

  @Override
  public boolean canUse(PlayerEntity player) {
    return this.inventory.canPlayerUse(player);
  }

  private void addPlayerInventory(PlayerInventory playerInventory) {
    for (int i = 0; i < 3; ++i) {
      for (int l = 0; l < 9; ++l) {
        this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, PLAYER_INVENTORY_Y + i * 18));
      }
    }
  }

  private void addPlayerHotbar(PlayerInventory playerInventory) {
    for (int i = 0; i < 9; ++i) {
      this.addSlot(new Slot(playerInventory, i, 8 + i * 18, PLAYER_HOTBAR_Y));
    }
  }

  @Override
  public ItemStack quickMove(PlayerEntity player, int invSlot) {
    Slot slot = this.slots.get(invSlot);
    if (slot == null || !slot.hasStack())
      return ItemStack.EMPTY;

    ItemStack original = slot.getStack();
    ItemStack newStack = original.copy();

    if (invSlot < TABLE_SLOT_COUNT) {
      if (!this.insertItem(original, TABLE_SLOT_COUNT, this.slots.size(), true)) {
        return ItemStack.EMPTY;
      }
    } else if (!this.insertItem(original, 0, TABLE_SLOT_COUNT, false)) {
      return ItemStack.EMPTY;
    }

    if (original.isEmpty()) {
      slot.setStack(ItemStack.EMPTY);
    }

    slot.markDirty();

    return newStack;
  }

  public Inventory getInventory() {
    return this.inventory;
  }
}
