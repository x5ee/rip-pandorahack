package com.pandora.client.module.modules.combat;

import com.pandora.api.settings.Setting;
import com.pandora.api.util.misc.MessageBus;
import com.pandora.api.util.players.friends.Friends;
import com.pandora.api.util.world.BlockUtils;
import com.pandora.api.util.world.EntityUtil;
import com.pandora.client.module.Module;
import com.pandora.client.module.ModuleManager;
import com.pandora.client.module.modules.gui.ColorMain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockWeb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class AutoWeb extends Module {
   Setting.Mode trapType;
   Setting.Boolean chatMsg;
   Setting.Boolean rotate;
   Setting.Boolean disableNone;
   Setting.Integer enemyRange;
   Setting.Integer tickDelay;
   Setting.Integer blocksPerTick;
   private boolean noWeb = false;
   private boolean isSneaking = false;
   private boolean firstRun = false;
   private int blocksPlaced;
   private int delayTimeTicks = 0;
   private int offsetSteps = 0;
   private int oldSlot = -1;
   private EntityPlayer closestTarget;

   public AutoWeb() {
      super("AutoWeb", Module.Category.Combat);
   }

   public void setup() {
      ArrayList<String> trapTypes = new ArrayList();
      trapTypes.add("Single");
      trapTypes.add("Double");
      this.trapType = this.registerMode("Mode", "Mode", trapTypes, "Double");
      this.disableNone = this.registerBoolean("Disable No Web", "DisableNoWb", true);
      this.rotate = this.registerBoolean("Rotate", "Rotate", true);
      this.tickDelay = this.registerInteger("Tick Delay", "TickDelay", 5, 0, 10);
      this.blocksPerTick = this.registerInteger("Blocks Per Tick", "BlocksPerTick", 4, 0, 8);
      this.enemyRange = this.registerInteger("Range", "Range", 4, 0, 6);
      this.chatMsg = this.registerBoolean("Chat Msgs", "ChatMsgs", true);
   }

   public void onEnable() {
      if (mc.field_71439_g == null) {
         this.disable();
      } else {
         if (this.chatMsg.getValue()) {
            MessageBus.sendClientPrefixMessage(ColorMain.getEnabledColor() + "AutoWeb turned ON!");
         }

         this.oldSlot = mc.field_71439_g.field_71071_by.field_70461_c;
         if (this.findWebSlot() != -1) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.findWebSlot();
         }

      }
   }

   public void onDisable() {
      if (mc.field_71439_g != null) {
         if (this.chatMsg.getValue()) {
            if (this.noWeb) {
               MessageBus.sendClientPrefixMessage(ColorMain.getDisabledColor() + "No web detected... AutoWeb turned OFF!");
            } else {
               MessageBus.sendClientPrefixMessage(ColorMain.getDisabledColor() + "AutoWeb turned OFF!");
            }
         }

         if (this.isSneaking) {
            mc.field_71439_g.field_71174_a.func_147297_a(new CPacketEntityAction(mc.field_71439_g, Action.STOP_SNEAKING));
            this.isSneaking = false;
         }

         if (this.oldSlot != mc.field_71439_g.field_71071_by.field_70461_c && this.oldSlot != -1) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.oldSlot;
            this.oldSlot = -1;
         }

         this.noWeb = false;
         this.firstRun = true;
         AutoCrystal.stopAC = false;
      }
   }

   public void onUpdate() {
      if (mc.field_71439_g == null) {
         this.disable();
      } else if (this.disableNone.getValue() && this.noWeb) {
         this.disable();
      } else {
         this.findClosestTarget();
         if (this.closestTarget != null) {
            if (this.firstRun) {
               this.firstRun = false;
               if (this.findWebSlot() == -1) {
                  this.noWeb = true;
               }
            } else {
               if (this.delayTimeTicks < this.tickDelay.getValue()) {
                  ++this.delayTimeTicks;
                  return;
               }

               this.delayTimeTicks = 0;
            }

            this.blocksPlaced = 0;

            while(this.blocksPlaced <= this.blocksPerTick.getValue()) {
               List<Vec3d> placeTargets = new ArrayList();
               int maxSteps;
               if (this.trapType.getValue().equalsIgnoreCase("Single")) {
                  Collections.addAll(placeTargets, AutoWeb.Offsets.SINGLE);
                  maxSteps = AutoWeb.Offsets.SINGLE.length;
               } else {
                  Collections.addAll(placeTargets, AutoWeb.Offsets.DOUBLE);
                  maxSteps = AutoWeb.Offsets.DOUBLE.length;
               }

               if (this.offsetSteps >= maxSteps) {
                  this.offsetSteps = 0;
                  break;
               }

               BlockPos offsetPos = new BlockPos((Vec3d)placeTargets.get(this.offsetSteps));
               BlockPos targetPos = (new BlockPos(this.closestTarget.func_174791_d())).func_177982_a(offsetPos.func_177958_n(), offsetPos.func_177956_o(), offsetPos.func_177952_p());
               boolean tryPlacing = true;
               if (!mc.field_71441_e.func_180495_p(targetPos).func_185904_a().func_76222_j()) {
                  tryPlacing = false;
               }

               if (tryPlacing && this.placeBlock(targetPos, this.enemyRange.getValue())) {
                  ++this.blocksPlaced;
               }

               ++this.offsetSteps;
               if (this.isSneaking) {
                  mc.field_71439_g.field_71174_a.func_147297_a(new CPacketEntityAction(mc.field_71439_g, Action.STOP_SNEAKING));
                  this.isSneaking = false;
               }
            }

         }
      }
   }

   private int findWebSlot() {
      int slot = -1;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
         if (stack != ItemStack.field_190927_a && stack.func_77973_b() instanceof ItemBlock) {
            Block block = ((ItemBlock)stack.func_77973_b()).func_179223_d();
            if (block instanceof BlockWeb) {
               slot = i;
               break;
            }
         }
      }

      return slot;
   }

   private boolean placeBlock(BlockPos pos, int range) {
      Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
      if (!(block instanceof BlockAir) && !(block instanceof BlockLiquid)) {
         return false;
      } else {
         EnumFacing side = BlockUtils.getPlaceableSide(pos);
         if (side == null) {
            return false;
         } else {
            BlockPos neighbour = pos.func_177972_a(side);
            EnumFacing opposite = side.func_176734_d();
            if (!BlockUtils.canBeClicked(neighbour)) {
               return false;
            } else {
               Vec3d hitVec = (new Vec3d(neighbour)).func_72441_c(0.5D, 0.5D, 0.5D).func_178787_e((new Vec3d(opposite.func_176730_m())).func_186678_a(0.5D));
               Block neighbourBlock = mc.field_71441_e.func_180495_p(neighbour).func_177230_c();
               if (mc.field_71439_g.func_174791_d().func_72438_d(hitVec) > (double)range) {
                  return false;
               } else {
                  int webbSlot = this.findWebSlot();
                  if (mc.field_71439_g.field_71071_by.field_70461_c != webbSlot) {
                     mc.field_71439_g.field_71071_by.field_70461_c = webbSlot;
                  }

                  if (!this.isSneaking && BlockUtils.blackList.contains(neighbourBlock) || BlockUtils.shulkerList.contains(neighbourBlock)) {
                     mc.field_71439_g.field_71174_a.func_147297_a(new CPacketEntityAction(mc.field_71439_g, Action.START_SNEAKING));
                     this.isSneaking = true;
                  }

                  if (webbSlot == -1) {
                     this.noWeb = true;
                     return false;
                  } else {
                     boolean stoppedAC = false;
                     if (ModuleManager.isModuleEnabled("AutoCrystalGS")) {
                        AutoCrystal.stopAC = true;
                        stoppedAC = true;
                     }

                     if (this.rotate.getValue()) {
                        BlockUtils.faceVectorPacketInstant(hitVec);
                     }

                     mc.field_71442_b.func_187099_a(mc.field_71439_g, mc.field_71441_e, neighbour, opposite, hitVec, EnumHand.MAIN_HAND);
                     mc.field_71439_g.func_184609_a(EnumHand.MAIN_HAND);
                     mc.field_71467_ac = 4;
                     if (stoppedAC) {
                        AutoCrystal.stopAC = false;
                        stoppedAC = false;
                     }

                     return true;
                  }
               }
            }
         }
      }
   }

   private void findClosestTarget() {
      List<EntityPlayer> playerList = mc.field_71441_e.field_73010_i;
      this.closestTarget = null;
      Iterator var2 = playerList.iterator();

      while(var2.hasNext()) {
         EntityPlayer entityPlayer = (EntityPlayer)var2.next();
         if (entityPlayer != mc.field_71439_g && !Friends.isFriend(entityPlayer.func_70005_c_()) && EntityUtil.isLiving(entityPlayer)) {
            if (this.closestTarget == null) {
               this.closestTarget = entityPlayer;
            } else if (mc.field_71439_g.func_70032_d(entityPlayer) < mc.field_71439_g.func_70032_d(this.closestTarget)) {
               this.closestTarget = entityPlayer;
            }
         }
      }

   }

   private static class Offsets {
      private static final Vec3d[] SINGLE = new Vec3d[]{new Vec3d(0.0D, 0.0D, 0.0D)};
      private static final Vec3d[] DOUBLE = new Vec3d[]{new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 1.0D, 0.0D)};
   }
}
