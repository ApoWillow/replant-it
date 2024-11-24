package com.apowillow;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoReplantModClient implements ClientModInitializer {

    private static boolean shouldPlace = false;

    private static BlockPos position = null;

    private static int beforeHand = -1;

    @Override
    public void onInitializeClient() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (shouldPlace && beforeHand > -1) {
                BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(position.down()), Direction.UP, position.down(), false);
                MinecraftClient.getInstance().interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
                MinecraftClient.getInstance().interactionManager.clickSlot(new InventoryScreen(client.player).getScreenHandler().syncId, convertSlotIds(beforeHand), client.player.getInventory().selectedSlot, SlotActionType.SWAP, client.player);
            }
            shouldPlace = false;
        });
        ClientPlayerBlockBreakEvents.AFTER.register((clientWorld, clientPlayerEntity, blockPos, blockState) -> {
            if (blockState.getBlock() instanceof CropBlock cropBlock) {
                ItemStack seedStack = findSeedInInventory(clientPlayerEntity, cropBlock);
                if (!seedStack.isEmpty() && !clientPlayerEntity.getStackInHand(Hand.MAIN_HAND).isOf(Items.AIR)) {
                    System.out.println("Seedstack works" + seedStack);
                    beforeHand = clientPlayerEntity.getInventory().getSlotWithStack(seedStack);
                    // Simulate right-click to replant
                    MinecraftClient.getInstance().interactionManager.clickSlot(new InventoryScreen(clientPlayerEntity).getScreenHandler().syncId, convertSlotIds(clientPlayerEntity.getInventory().getSlotWithStack(seedStack)), clientPlayerEntity.getInventory().selectedSlot, SlotActionType.SWAP, clientPlayerEntity);
                    //MinecraftClient.getInstance().interactionManager.interactBlock(clientPlayerEntity, Hand.MAIN_HAND, hitResult);
                    shouldPlace = true;
                    position = blockPos;
                }
            }
        });
    }

    public static int convertSlotIds(int slot) {
        //For some stupid reason, the hotbar slots are different
        //https://wiki.vg/File:Inventory-slots.png
        if (PlayerInventory.isValidHotbarIndex(slot)) {
            slot += 36;
        } else if (slot == 40) {
            slot = 45;
        }

        return slot;
    }

	/*
	private void handleBlockBreak(World world, BlockPos pos, BlockState state, ClientPlayerEntity player) {
		if (state.getBlock() instanceof CropBlock cropBlock) {
			replantCrop(world, pos, cropBlock, player);
		}
	}
	 */
	/*
	private void replantCrop(World world, BlockPos pos, CropBlock cropBlock, ClientPlayerEntity player) {
		ItemStack seedStack = findSeedInInventory(player, cropBlock);
		if (!seedStack.isEmpty()) {
			// Simulate right-click to replant
			BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
			ActionResult result = player.interactAt(player, Vec3d.of(pos), Hand.MAIN_HAND);
		}
	}
	 */

    private ItemStack findSeedInInventory(ClientPlayerEntity player, CropBlock cropBlock) {
        Item seedItem = getSeedForCropBlock(cropBlock);

        // Search the player's inventory for the seed
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() == seedItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // Get the seed item for a crop block
    private Item getSeedForCropBlock(CropBlock cropBlock) {
        try {
            return cropBlock.getPickStack(null, null, null).getItem();
        } catch (NullPointerException e) {
            return Items.AIR;
        }
    }


    // To prevent the server's anti cheat from activating.

    private long lastReplantTime = 0;

    private boolean canReplant() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastReplantTime > 200) { // 200ms cooldown
            lastReplantTime = currentTime;
            return true;
        }
        return false;
    }

}