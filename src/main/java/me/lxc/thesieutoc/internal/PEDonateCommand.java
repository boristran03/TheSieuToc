package me.lxc.thesieutoc.internal;

import com.google.gson.JsonObject;
import me.lxc.thesieutoc.TheSieuToc;
import me.lxc.thesieutoc.tasks.CardCheckTask;
import net.thesieutoc.TheSieuTocAPI;
import net.thesieutoc.data.CardAmount;
import net.thesieutoc.data.CardInfo;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.util.FormBuilder;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static me.lxc.thesieutoc.handlers.InputCardHandler.LocalCardInfo;

public class PEDonateCommand implements CommandExecutor {

    final TheSieuToc main = TheSieuToc.getInstance();

    public void getNapTheForm(Player player) {
        FloodgateApi api = FloodgateApi.getInstance();
        if (!api.isFloodgatePlayer(player.getUniqueId())) {
            player.sendMessage("Bạn không phải người chơi PE");
            return;
        }
        List<String> types = main.getSettings().cardEnable;
        List<Integer> amounts = main.getSettings().amountList;
        String[] cardType = Arrays.copyOf(types.toArray(), types.size(), String[].class);
        List<String> strings = amounts.stream().map(Object::toString)
                .collect(Collectors.toUnmodifiableList());
        String[] cardAmount = Arrays.copyOf(strings.toArray(), strings.size(), String[].class);
        FormBuilder form = CustomForm.builder()
                .title("Nạp Thẻ")
                .dropdown("Chọn loại thẻ", cardType)
                .dropdown("Chọn mệnh giá thẻ", cardAmount)
                .input("Số seri", "Nhập số seri tại đây")
                .input("Mã thẻ", "Nhập mã thẻ tại đây")
                .responseHandler((formHandler, rawData) -> formHandler(player, formHandler, rawData));
        api.sendForm(player.getUniqueId(), form);
    }

    private void formHandler(Player player, CustomForm form, String rawData) {
        CustomFormResponse response = form.parseResponse(rawData);
        if (!response.isCorrect()) {
            player.sendMessage("§cĐã xãy ra lỗi, vui lòng nạp lại!");
            return;
        }
        Settings settings = TheSieuToc.getInstance().getSettings();
        int cardType = response.getDropdown(0);
        int cardAmount = response.getDropdown(1) + 1;
        //the original CardAmount class start the id from 1
        String serial = response.getInput(2);
        String pin = response.getInput(3);

        if (StringUtils.isBlank(serial) || StringUtils.isBlank(pin)) {
            player.sendMessage("Số bạn nhập không hợp lệ, vui lòng thử lại");
            return;
        }
        if (!serial.matches(main.getRegex()) || !pin.matches(main.getRegex())) {
            player.sendMessage("Số bạn nhập không hợp lệ, vui lòng thử lại");
            return;
        }
        Messages msg = TheSieuToc.getInstance().getMessages();
        List<String> types = main.getSettings().cardEnable;
        LocalCardInfo info = new LocalCardInfo(types.get(cardType), CardAmount.getAmountFromID(cardAmount), serial, pin);
        JsonObject sendCard = TheSieuTocAPI.sendCard(settings.iTheSieuTocKey, settings.iTheSieuTocSecret, info.type, info.amount, info.serial, info.pin);
        main.submitCardDebug(player, info);
        TheSieuToc.pluginDebug.debug("Response: " + (sendCard != null ? sendCard.toString() : "NULL"));
        assert sendCard != null;
        if (!sendCard.get("status").getAsString().equals("00")) {
            player.sendMessage(msg.fail);
            player.sendMessage(sendCard.get("msg").getAsString());
            return;
        }
        String transactionID = sendCard.get("transaction_id").getAsString();
        CardInfo tstInfo = new CardInfo(transactionID, info.type, info.amount, info.serial, info.pin);

        Bukkit.getScheduler().runTaskLaterAsynchronously(TheSieuToc.getInstance(), () -> {
            if (CardCheckTask.getInstance().checkOne(player, tstInfo, null)) {
                List<CardInfo> queue = TheSieuToc.getInstance().queue.get(player);
                if (queue == null) {
                    queue = new ArrayList<>();
                }
                queue.add(tstInfo);
                if (TheSieuToc.getInstance().queue.containsKey(player))
                    TheSieuToc.getInstance().queue.replace(player, queue);
                else TheSieuToc.getInstance().queue.put(player, queue);
            }
        }, 20L);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)) return true;
        Player player = (Player) commandSender;
        getNapTheForm(player);
        return true;
    }
}

//enum TypeID {
//    VIETTEL(0), VINAPHONE(1), MOBIFONE(2), VIETNAMOBILE(3), VCOIN(4), ZING(5), GATE(6);
//
//    private final int id;
//
//    TypeID(int id) {
//        this.id = id;
//    }
//
//    public static String getType(int id) {
//        String result = "";
//        for (TypeID value : values()) {
//            if(value.getID() == id)
//                result = value.toString();
//        }
//        return result;
//    }
//
//    public int getID() {
//        return id;
//    }
//}
