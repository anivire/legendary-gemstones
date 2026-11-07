package name.modid.core.content.events.handlers;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import name.modid.core.api.modifiers.config.GemstoneModifier;
import name.modid.core.api.modifiers.config.ModifierConfig.BeforeBlockBreakConfig;
import name.modid.core.api.modifiers.config.ModifierContext;
import name.modid.core.api.modifiers.config.ModifierContext.ContextBuilder;
import name.modid.core.api.modifiers.config.ModifierManager;
import name.modid.core.api.modifiers.config.utils.ModifierUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class EventOnBeforeBlockBreak {
  private static final ThreadLocal<List<ActiveBreak>> ACTIVE_BREAKS = ThreadLocal.withInitial(ArrayList::new);

  private static final class ActiveBreak {
    private final ServerPlayerEntity player;
    private final BlockPos pos;
    private boolean dropsModified;

    private ActiveBreak(ServerPlayerEntity player, BlockPos pos) {
      this.player = player;
      this.pos = pos;
    }
  }

  public static void beginBreak(ServerPlayerEntity player, BlockPos pos) {
    ACTIVE_BREAKS.get().add(new ActiveBreak(player, pos.toImmutable()));
  }

  public static void endBreak() {
    List<ActiveBreak> breaks = ACTIVE_BREAKS.get();
    if (!breaks.isEmpty()) {
      breaks.remove(breaks.size() - 1);
    }
  }

  public static List<ItemStack> modifyDrops(ServerWorld world, BlockPos pos, BlockState state,
      @Nullable BlockEntity blockEntity, @Nullable Entity entity, List<ItemStack> drops) {
    List<ActiveBreak> breaks = ACTIVE_BREAKS.get();
    if (breaks.isEmpty()) {
      return drops;
    }

    ActiveBreak active = breaks.get(breaks.size() - 1);
    if (active.dropsModified || active.player != entity || !active.pos.equals(pos)) {
      return drops;
    }
    active.dropsModified = true;

    List<GemstoneModifier> modifiers = ModifierUtils.collectGemstoneModifiersFromAllEquipment(
        active.player,
        BeforeBlockBreakConfig.class);

    if (modifiers.isEmpty()) {
      return drops;
    }

    ModifierContext ctx = new ContextBuilder(world)
        .withOwner(active.player)
        .withBlockPos(pos)
        .withBlockState(state)
        .withBlockEntity(blockEntity)
        .withDrops(drops)
        .build();
    ModifierManager.applyModifiers(new ArrayList<>(modifiers), ctx);

    return ctx.getDrops();
  }
}
