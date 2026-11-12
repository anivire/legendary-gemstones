package name.modid.core.content.blocks.entity;

import org.jetbrains.annotations.Nullable;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import name.modid.core.api.modifiers.helpers.GemstoneSlotHelper;
import name.modid.core.content.blocks.entity.core.BlockEntitiesRegistry;
import name.modid.core.content.blocks.entity.core.ImplementedInventory;
import name.modid.core.content.items.ExpansionCrystalItem;
import name.modid.core.content.items.tools.ChiselItem;
import name.modid.core.content.items.tools.JewelryHammerItem;
import name.modid.core.content.items.tools.JewelryPliersItem;
import name.modid.core.content.screen.JewelryTableScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class JewelryTableBlockEntity extends BlockEntity
    implements ImplementedInventory, ExtendedMenuProvider {
  private static final int[] AUTOMATION_INPUT_SLOTS = {
      JewelryTableScreenHandler.SLOT_CHISEL,
      JewelryTableScreenHandler.SLOT_BASE,
      JewelryTableScreenHandler.SLOT_HAMMER,
      JewelryTableScreenHandler.SLOT_PLIERS,
      JewelryTableScreenHandler.SLOT_CRYSTAL
  };

  private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(
      JewelryTableScreenHandler.TABLE_INVENTORY_SIZE, ItemStack.EMPTY);

  public JewelryTableBlockEntity(BlockPos pos, BlockState state) {
    super(BlockEntitiesRegistry.JEWELRY_TABLE_BLOCK_ENTITY.get(), pos, state);
  }

  @Override
  public DefaultedList<ItemStack> getItems() {
    return inventory;
  }

  @Override
  public int[] getAvailableSlots(Direction side) {
    return AUTOMATION_INPUT_SLOTS;
  }

  @Override
  public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
    return canAutomationInsert(slot, stack);
  }

  @Override
  public boolean canExtract(int slot, ItemStack stack, Direction side) {
    return true;
  }

  @Override
  public boolean canPlayerUse(PlayerEntity player) {
    return Inventory.canPlayerUse(this, player);
  }

  public static boolean canAutomationInsert(int slot, ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }

    return canAutomationInsertItem(slot, stack.getItem());
  }

  static boolean canAutomationInsertItem(int slot, @Nullable Item item) {
    return switch (slot) {
      case JewelryTableScreenHandler.SLOT_CHISEL -> item instanceof ChiselItem;
      case JewelryTableScreenHandler.SLOT_BASE -> item != null && GemstoneSlotHelper.isItemValid(item);
      case JewelryTableScreenHandler.SLOT_HAMMER -> item instanceof JewelryHammerItem;
      case JewelryTableScreenHandler.SLOT_PLIERS -> item instanceof JewelryPliersItem;
      case JewelryTableScreenHandler.SLOT_CRYSTAL -> item instanceof ExpansionCrystalItem;
      default -> false;
    };
  }

  public ItemStack getDisplayedTool() {
    for (int slot : JewelryTableScreenHandler.TOOL_SLOTS) {
      if (slot != JewelryTableScreenHandler.SLOT_CRYSTAL && !getStack(slot).isEmpty()) {
        return getStack(slot);
      }
    }
    return ItemStack.EMPTY;
  }

  private void moveLegacyStacks() {
    moveLegacyStack(JewelryTableScreenHandler.SLOT_HAMMER);
    moveLegacyStack(JewelryTableScreenHandler.SLOT_CHISEL);
  }

  private void moveLegacyStack(int slot) {
    ItemStack stack = inventory.get(slot);
    if (stack.isEmpty() || canAutomationInsert(slot, stack)) {
      return;
    }

    inventory.set(slot, ItemStack.EMPTY);
    inventory.set(getLegacyStackTarget(stack.getItem()), stack);
  }

  private static int getLegacyStackTarget(Item item) {
    if (item instanceof ChiselItem) {
      return JewelryTableScreenHandler.SLOT_CHISEL;
    }
    if (item instanceof JewelryHammerItem) {
      return JewelryTableScreenHandler.SLOT_HAMMER;
    }
    if (item instanceof JewelryPliersItem) {
      return JewelryTableScreenHandler.SLOT_PLIERS;
    }
    if (item instanceof ExpansionCrystalItem) {
      return JewelryTableScreenHandler.SLOT_CRYSTAL;
    }
    return JewelryTableScreenHandler.SLOT_LEGACY_GEM;
  }

  @Override
  public void markDirty() {
    super.markDirty();

    if (world != null && !world.isClient) {
      BlockState state = getCachedState();
      world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
    }
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    super.writeNbt(nbt, registryLookup);
    Inventories.writeNbt(nbt, inventory, registryLookup);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    super.readNbt(nbt, registryLookup);
    inventory.clear();
    Inventories.readNbt(nbt, inventory, registryLookup);
    moveLegacyStacks();
  }

  @Override
  public void saveExtraData(net.minecraft.network.PacketByteBuf buf) {
    buf.writeBlockPos(this.pos);
  }

  @Override
  public Text getDisplayName() {
    return Text.translatable("container.gemstones.jewelry_table");
  }

  @Override
  public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
    ItemStack legacyGem = removeStack(JewelryTableScreenHandler.SLOT_LEGACY_GEM);
    if (!legacyGem.isEmpty()) {
      playerInventory.offerOrDrop(legacyGem);
    }

    return new JewelryTableScreenHandler(syncId, playerInventory, this.pos);
  }

  @Nullable
  @Override
  public Packet<ClientPlayPacketListener> toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
    return createNbt(registryLookup);
  }
}
