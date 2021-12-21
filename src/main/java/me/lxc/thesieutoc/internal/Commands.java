package me.lxc.thesieutoc.internal;

import me.lxc.artxeapi.utils.ArtxeNumber;
import me.lxc.thesieutoc.TheSieuToc;
import me.lxc.thesieutoc.handlers.InputCardHandler;
import me.lxc.thesieutoc.tasks.CardCheckTask;
import me.lxc.thesieutoc.utils.CalculateTop;
import me.lxc.thesieutoc.utils.JsonMessage;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class Commands implements CommandExecutor {

    private static final List<String> TT = Arrays.asList("TOTAL", "DAY", "MONTH", "YEAR");

    static boolean isValidCard(String type) {
        return TheSieuToc.getInstance().getSettings().cardEnable
                .stream().anyMatch(type::equalsIgnoreCase);
    }

    private boolean clearCache(CommandSender sender, Messages msg) {
        if (sender.hasPermission("napthe.admin.cache.clear")) {
            CalculateTop.clearCache();
            sender.sendMessage(msg.cacheCleared);
            return true;
        } else {
            sender.sendMessage(msg.noPermission);
            return false;
        }
    }

    private boolean reload(CommandSender sender, int arg, Messages msg) {
        if (sender.hasPermission("napthe.admin.reload")) {
            if (arg == 1) {
                TheSieuToc.getInstance().reload((short) 0);
                sender.sendMessage(msg.reloaded);
                return true;
            } else {
                sender.sendMessage(msg.tooManyArgs);
                return false;
            }
        } else {
            sender.sendMessage(msg.noPermission);
            return false;
        }
    }

    private boolean give(CommandSender sender, String[] args, Messages msg) {
        if (sender.hasPermission("napthe.admin.give")) {
            switch (args.length) {
                case 1, 2 -> {
                    sender.sendMessage(msg.tooFewArgs);
                    return false;
                }
                case 3 -> {
                    String args_2 = args[2].replaceAll("\\.", "");
                    if (ArtxeNumber.isInteger(args_2)) {
                        String playerName = args[1];
                        int amount = Integer.parseInt(args_2);
                        TheSieuToc.getInstance().getDonorLog().writeLog(playerName, "0", "0", "GIVE", amount, true, "FROM GIVE COMMAND");
                        sender.sendMessage(msg.given.replaceAll("(?ium)[{]Player[}]", playerName).replaceAll("(?ium)[{]Amount[}]", args[2]));
                    } else {
                        sender.sendMessage(msg.notNumber.replaceAll("(?ium)[{]0[}]", args_2));
                        return false;
                    }
                    return true;
                }
                default -> {
                    String argsDF_2 = args[2].replaceAll("\\.", "");
                    if (ArtxeNumber.isInteger(argsDF_2)) {
                        String playerName = args[1];
                        int amount = Integer.parseInt(argsDF_2);
                        String notes = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                        TheSieuToc.getInstance().getDonorLog().writeLog(playerName, "0", "0", "GIVE", amount, true, notes);
                        return true;
                    } else {
                        sender.sendMessage(msg.notNumber.replaceAll("(?ium)[{]0[}]", argsDF_2));
                        return false;
                    }
                }
            }
        } else {
            sender.sendMessage(msg.noPermission);
            return false;
        }
    }

    private boolean check(CommandSender sender, int arg, Messages msg) {
        if (sender.hasPermission("napthe.admin.check")) {
            if (arg == 1) {
                CardCheckTask.getInstance().checkAll();
                sender.sendMessage(msg.checked);
                return true;
            } else {
                sender.sendMessage(msg.tooManyArgs);
                return false;
            }
        } else {
            sender.sendMessage(msg.noPermission);
            return false;
        }
    }

    private boolean top(CommandSender sender, String[] args) {
        Messages msg = TheSieuToc.getInstance().getMessages();
        Bukkit.getScheduler().runTaskAsynchronously(TheSieuToc.getInstance(), () -> {
            switch (args.length) {
                case 1 -> CalculateTop.printTop(sender, CalculateTop.execute("total"), 10);
                case 2 -> {
                    if (ArtxeNumber.isInteger(args[1])) {
                        CalculateTop.printTop(sender, CalculateTop.execute("total"), Integer.parseInt(args[1]));
                    } else {
                        sender.sendMessage(msg.notNumber.replaceAll("(?ium)[{]0[}]", args[1]));
                    }
                }
                case 3 -> {
                    if (!ArtxeNumber.isInteger(args[1])) {
                        sender.sendMessage(msg.notNumber.replaceAll("(?ium)[{]0[}]", args[1]));
                        break;
                    }
                    if (TT.stream().noneMatch(args[2]::equalsIgnoreCase)) {
                        sender.sendMessage(msg.invalidCommand);
                        break;
                    }
                    CalculateTop.printTop(sender, CalculateTop.execute(args[2]), Integer.parseInt(args[1]));
                }
                default -> sender.sendMessage(msg.tooManyArgs);
            }
        });
        return true;
    }

    private void chooseCard(CommandSender sender, boolean isPlayer, boolean hasAPIInfo, Ui ui, Messages msg) {
        if (!hasAPIInfo) sender.sendMessage(msg.missingApiInfo);
        if (!isPlayer) sender.sendMessage(msg.onlyPlayer);
        final Player player = (Player) sender;
        for (String card : TheSieuToc.getInstance().getSettings().cardEnable) {
            String text = ui.cardTypeText.replaceAll("(?ium)[{]Card_Type[}]", card);
            String hover = splitListToLine(ui.cardTypeHover).replaceAll("(?ium)[{]Card_Type[}]", card);
            new JsonMessage().append(text).setHoverAsTooltip(hover).setClickAsExecuteCmd("/donate choose " + card).save().send(player);
        }
    }

    private void chooseAmount(Player player, String type, Ui ui) {
        for (int amount : TheSieuToc.getInstance().getSettings().amountList) {
            String text = ui.cardAmountText.replaceAll("(?ium)[{]Card_Amount[}]", String.valueOf(amount));
            String hover = splitListToLine(ui.cardAmountHover).replaceAll("(?ium)[{]Card_Amount[}]", String.valueOf(amount));
            String executeCommand = "/donate choose " + type + " " + amount;
            new JsonMessage().append(text).setHoverAsTooltip(hover).setClickAsExecuteCmd(executeCommand).save().send(player);
        }
    }

    private String splitListToLine(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            String line = list.get(i);
            sb.append(ChatColor.translateAlternateColorCodes('&', line));
            if (i < list.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] arg) {
        final Messages msg = TheSieuToc.getInstance().getMessages();
        final Ui ui = TheSieuToc.getInstance().getUi();
        final boolean isPlayer = sender instanceof Player;
        final Player player = isPlayer ? (Player) sender : null;
        final boolean hasAPIInfo = TheSieuToc.getInstance().hasAPIInfo;

        switch (arg.length) {
            case 0:
                FloodgateApi api = FloodgateApi.getInstance();
                if (isPlayer) {
                    if (api.isFloodgatePlayer(((Player) sender).getUniqueId())) {
                        sender.sendMessage("§cBạn đang sử dụng phiên bản PE, vui lòng nạp thẻ bằng lệnh §e§n/napthepe");
                        return true;
                    }
                }
                chooseCard(sender, isPlayer, hasAPIInfo, ui, msg);
                return true;
            case 1:
                switch (arg[0].toLowerCase()) {
                    case "give" -> {
                        return give(sender, arg, msg);
                    }
                    case "clear-cache" -> {
                        return clearCache(sender, msg);
                    }
                    case "reload" -> {
                        return reload(sender, arg.length, msg);
                    }
                    case "choose" -> {
                        chooseCard(sender, isPlayer, hasAPIInfo, ui, msg);
                        return true;
                    }
                    case "check" -> {
                        if (hasAPIInfo) {
                            return check(sender, arg.length, msg);
                        } else {
                            sender.sendMessage(msg.missingApiInfo);
                            return false;
                        }
                    }
                    case "top" -> {
                        return top(sender, arg);
                    }
                    default -> {
                        sender.sendMessage(msg.invalidCommand);
                        return false;
                    }
                }
            case 2:
                switch (arg[0].toLowerCase()) {
                    case "give" -> {
                        return give(sender, arg, msg);
                    }
                    case "choose" -> {
                        if (!hasAPIInfo) {
                            sender.sendMessage(msg.missingApiInfo);
                            return false;
                        }
                        if (!isPlayer) {
                            sender.sendMessage(msg.onlyPlayer);
                            return false;
                        }
                        if (isValidCard(arg[1])) {
                            String type = arg[1];
                            chooseAmount(player, type, ui);
                            return true;
                        }
                        sender.sendMessage(msg.invalidCardType);
                        return false;
                    }
                    case "reload" -> {
                        return reload(sender, arg.length, msg);
                    }
                    case "check" -> {
                        if (hasAPIInfo) {
                            return check(sender, arg.length, msg);
                        } else {
                            sender.sendMessage(msg.missingApiInfo);
                            return false;
                        }
                    }
                    case "top" -> {
                        return top(sender, arg);
                    }
                    default -> {
                        sender.sendMessage(msg.invalidCommand);
                        return false;
                    }
                }
            case 3:
                switch (arg[0].toLowerCase()) {
                    case "give" -> {
                        return give(sender, arg, msg);
                    }
                    case "choose" -> {
                        String arg2 = StringUtils.replace(arg[2], ".", "");
                        if (!hasAPIInfo) {
                            sender.sendMessage(msg.missingApiInfo);
                            return false;
                        }
                        if (!isPlayer) {
                            sender.sendMessage(msg.onlyPlayer);
                            return false;
                        }
                        if (!isValidCard(arg[1])) {
                            sender.sendMessage(msg.invalidCardType);
                            return false;
                        }
                        if (!ArtxeNumber.isInteger(arg2)) {
                            sender.sendMessage(msg.notNumber.replaceAll("(?ium)[{]0[}]", arg2));
                            return false;
                        }
                        player.sendMessage("§aNếu bạn muốn thoát, hãy chat §e'cancel' §ađể hủy");
                        InputCardHandler.triggerStepOne(player, arg[1], Integer.parseInt(arg2));
                        return true;
                    }
                    case "reload" -> {
                        return reload(sender, arg.length, msg);
                    }
                    case "check" -> {
                        if (hasAPIInfo) {
                            return check(sender, arg.length, msg);
                        } else {
                            sender.sendMessage(msg.missingApiInfo);
                            return false;
                        }
                    }
                    case "top" -> {
                        return top(sender, arg);
                    }
                    default -> {
                        sender.sendMessage(msg.invalidCommand);
                        return false;
                    }
                }
            default:
                if ("give".equals(arg[0].toLowerCase())) {
                    return give(sender, arg, msg);
                }
                sender.sendMessage(msg.tooManyArgs);
                return false;
        }
    }
}