package me.lxc.thesieutoc.event;

import com.google.gson.JsonObject;
import me.lxc.thesieutoc.TheSieuToc;
import me.lxc.thesieutoc.internal.Messages;
import me.lxc.thesieutoc.internal.Settings;
import me.lxc.thesieutoc.internal.Ui;
import me.lxc.thesieutoc.tasks.CardCheckTask;
import net.thesieutoc.TheSieuTocAPI;
import net.thesieutoc.data.CardInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;

import static me.lxc.thesieutoc.handlers.InputCardHandler.*;

public class PlayerChat implements Listener {

    private final TheSieuToc main = TheSieuToc.getInstance();
    final Settings settings = main.getSettings();
    final Messages msg = main.getMessages();
    final Ui ui = main.getUi();
    final String regex = main.getRegex();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        final Player player = e.getPlayer();
        final String text = ChatColor.stripColor(e.getMessage());
        if (stepOne(player) && !stepTwo(player)) {
            e.setCancelled(true);
            if (ui.cancel.stream().noneMatch(text::equalsIgnoreCase) && text.matches(main.getRegex())) {
                player.sendMessage(msg.serial.replaceAll("(?ium)[{]Serial[}]", text));
                unTriggerStep1(player);
                triggerStepTwo(player, text);
            } else {
                unTriggerStep1(player);
                purgePlayer(player);
                player.sendMessage(msg.cancelled);
            }
            return;
        }

        if (!stepOne(player) && stepTwo(player)) {
            e.setCancelled(true);
            if (ui.cancel.stream().noneMatch(text::equalsIgnoreCase) && text.matches(regex)) {
                LocalCardInfo info = lastStep(player, text);
                unTriggerStep2(player);
                player.sendMessage(msg.pin.replaceAll("(?ium)[{]Pin[}]", text));
                main.submitCardDebug(player, info);
                JsonObject sendCard = TheSieuTocAPI.sendCard(settings.iTheSieuTocKey, settings.iTheSieuTocSecret, info.type, info.amount, info.serial, info.pin);
                TheSieuToc.pluginDebug.debug("Response: " + (sendCard != null ? sendCard.toString() : "NULL"));
                assert sendCard != null;
                if (!sendCard.get("status").getAsString().equals("2")) {
                    player.sendMessage(sendCard.get("msg").getAsString());
                    return;
                }
                if (!sendCard.get("status").getAsString().equals("00")) {
                    player.sendMessage(msg.fail);
                    player.sendMessage(sendCard.get("msg").getAsString());
                    return;
                }
                String transactionID = sendCard.get("transaction_id").getAsString();
                CardInfo tstInfo = new CardInfo(transactionID, info.type, info.amount, info.serial, info.pin);

                Bukkit.getScheduler().runTaskLaterAsynchronously(main, () -> {
                    if (CardCheckTask.getInstance().checkOne(player, tstInfo, null)) {
                        List<CardInfo> queue = main.queue.get(player);
                        if (queue == null) {
                            queue = new ArrayList<>();
                        }
                        queue.add(tstInfo);
                        if (main.queue.containsKey(player))
                            main.queue.replace(player, queue);
                        else main.queue.put(player, queue);
                    }
                }, 20L);
            } else {
                unTriggerStep2(player);
                purgePlayer(player);
                player.sendMessage(msg.cancelled);
            }
        }
    }


}
