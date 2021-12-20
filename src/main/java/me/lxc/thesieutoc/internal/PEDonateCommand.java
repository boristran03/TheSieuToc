package me.lxc.thesieutoc.internal;

import me.lxc.thesieutoc.TheSieuToc;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.util.FormBuilder;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

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
        int cardType = response.getDropdown(0);
        int cardAmount = response.getDropdown(1);
        String serial = response.getInput(2);
        String pin = response.getInput(3);
        LocalCardInfo info = new LocalCardInfo(String.valueOf(cardType), cardAmount, serial, pin);
        main.getLogger().info(info.toString());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)) return true;
        Player player = (Player) commandSender;
        getNapTheForm(player);
        return true;
    }
}
