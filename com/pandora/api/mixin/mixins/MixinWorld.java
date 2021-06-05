package com.pandora.api.mixin.mixins;

import com.pandora.client.module.ModuleManager;
import com.pandora.client.module.modules.render.NoRender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({World.class})
public class MixinWorld {
   @Inject(
      method = {"checkLightFor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void updateLightmapHook(EnumSkyBlock lightType, BlockPos pos, CallbackInfoReturnable<Boolean> info) {
      if (ModuleManager.isModuleEnabled("NoRender") && ((NoRender)ModuleManager.getModuleByName("NoRender")).noSkylight.getValue() && lightType == EnumSkyBlock.SKY) {
         info.setReturnValue(true);
         info.cancel();
      }

   }
}
