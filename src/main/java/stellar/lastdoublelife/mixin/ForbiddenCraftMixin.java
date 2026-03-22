package stellar.lastdoublelife.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stellar.lastdoublelife.manager.GameManager;

@Mixin(targets = "net.minecraft.world.inventory.CraftingResultSlot")
public class ForbiddenCraftMixin {

    /**
     * Inject at the start of onTake (called after the item has already been
     * moved to the player's cursor/inventory). If the item is forbidden:
     *  - Remove it from cursor or inventory so the player never keeps it.
     *  - Cancel the method so ingredients are NOT consumed (no penalty).
     */
    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void preventForbiddenCraft(Player player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!GameManager.isForbidden(stack)) return;

        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).toString();

        // Remove from cursor (regular click) or inventory (shift-click)
        AbstractContainerMenu menu = sp.containerMenu;
        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty() && carried.getItem() == stack.getItem()) {
            menu.setCarried(ItemStack.EMPTY);
        } else {
            var inv = sp.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack slot = inv.getItem(i);
                if (!slot.isEmpty() && slot.getItem() == stack.getItem()) {
                    inv.setItem(i, ItemStack.EMPTY);
                    break;
                }
            }
        }

        sp.sendSystemMessage(Component.literal(
                "[LDL] " + id + " is forbidden and cannot be crafted.")
                .withStyle(ChatFormatting.RED));

        // Cancel ingredient consumption — player keeps their materials
        ci.cancel();
    }
}
