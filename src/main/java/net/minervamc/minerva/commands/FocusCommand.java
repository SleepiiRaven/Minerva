package net.minervamc.minerva.commands;

import net.kyori.adventure.text.Component;
import net.minervamc.minerva.api.command.Command;
import net.minervamc.minerva.api.command.CommandContext;
import net.minervamc.minerva.api.command.ICommand;
import net.minervamc.minerva.api.text.Text;
import net.minervamc.minerva.utils.SpellCastUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@SuppressWarnings("unused")
public class FocusCommand extends Command {
    public FocusCommand() {
        super("focus",
                "Focus to cast a spell.",
                "/focus",
                List.of());
    }

    @ICommand(allArgs = true)
    public void allArgs(CommandContext context) {
        Player player = context.getSenderAsPlayer();
        if(player == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if(item.getType().isEmpty()) return;
        SpellCastUtils.toggleFocus(item);

        Component message = SpellCastUtils.isSpellCastFocused(item)
                ? Text.formatLegacy("&aToggled on focus")
                : Text.formatLegacy("&cToggled off focus");
        player.sendMessage(message);
    }

}
