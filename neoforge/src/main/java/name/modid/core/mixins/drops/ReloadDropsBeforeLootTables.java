package name.modid.core.mixins.drops;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import name.modid.datapack.drops.DropsDataLoader;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.ReloadableRegistries;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.resource.ResourceManager;

@Mixin(ReloadableRegistries.class)
public class ReloadDropsBeforeLootTables {
  @Inject(method = "reload", at = @At("HEAD"))
  private static void preloadDropsConfigs(
      CombinedDynamicRegistries<ServerDynamicRegistryType> dynamicRegistries,
      ResourceManager resourceManager,
      Executor prepareExecutor,
      CallbackInfoReturnable<CompletableFuture<CombinedDynamicRegistries<ServerDynamicRegistryType>>> cir) {
    DropsDataLoader.preload(resourceManager);
  }
}
