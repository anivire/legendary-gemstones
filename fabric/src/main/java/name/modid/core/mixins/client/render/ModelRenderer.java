package name.modid.core.mixins.client.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import name.modid.core.api.modifiers.config.utils.ModifierUtils;
import name.modid.core.content.registries.AttributesRegistry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.item.ItemStack;

@Mixin(AbstractClientPlayerEntity.class)
public class ModelRenderer {
  @ModifyExpressionValue(method = "getFovMultiplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getItemUseTime()I"))
  private int scaleBowZoomTime(int useTicks) {
    ItemStack stack = ((AbstractClientPlayerEntity) (Object) this).getActiveItem();
    AttributeModifiersComponent itemAttributeModifiers = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS,
        AttributeModifiersComponent.DEFAULT);

    float drawSpeedMultiplier = ModifierUtils.getAttributeMultiplier(
        itemAttributeModifiers,
        AttributesRegistry.PULL_SPEED_ATTRIBUTE);

    return Math.round(useTicks * drawSpeedMultiplier);
  }
}
