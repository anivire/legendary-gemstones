package name.modid.core.mixins.modifiers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import name.modid.core.content.entities.SparkProjectileEntity;
import name.modid.core.content.events.CustomEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;

@Mixin(PersistentProjectileEntity.class)
public class OnHitProjectile {
  @Inject(method = "onEntityHit", at = @At("HEAD"))
  protected void onEntityHit(EntityHitResult entityHitResult, CallbackInfo ci) {
    PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;

    if (projectile instanceof SparkProjectileEntity
        || !(projectile.getOwner() instanceof ServerPlayerEntity player)) {
      return;
    }

    CustomEvents.ON_PROJECTILE_DAMAGE.invoker().onProjectileDamage(projectile, player, entityHitResult);
  }

  // onHit is called only after the projectile has actually damaged a living
  // target, so shields, i-frames and dodging endermen no longer trigger on-hit
  // effects
  @Inject(method = "onHit", at = @At("HEAD"))
  protected void onLivingHit(LivingEntity target, CallbackInfo ci) {
    PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;

    if (projectile instanceof SparkProjectileEntity
        || !(projectile.getOwner() instanceof ServerPlayerEntity player)) {
      return;
    }

    CustomEvents.ON_HIT_PROJECTILE.invoker().onHitProjectile(projectile, player, new EntityHitResult(target));
  }

  @Inject(method = "onBlockHit", at = @At("HEAD"))
  protected void onBlockHit(BlockHitResult blockHitResult, CallbackInfo ci) {
    PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;

    if (projectile instanceof SparkProjectileEntity
        || !(projectile.getOwner() instanceof ServerPlayerEntity player)) {
      return;
    }

    CustomEvents.ON_HIT_PROJECTILE.invoker().onHitProjectile(projectile, player, blockHitResult);
  }
}
