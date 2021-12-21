package me.lxc.thesieutoc;

import me.lxc.artxeapi.data.ArtxeYAML;
import me.lxc.artxeapi.utils.ArtxeDebug;
import me.lxc.artxeapi.utils.ArtxeTime;
import me.lxc.thesieutoc.event.PlayerChat;
import me.lxc.thesieutoc.handlers.InputCardHandler;
import me.lxc.thesieutoc.internal.*;
import me.lxc.thesieutoc.tasks.CardCheckTask;
import net.thesieutoc.data.CardInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static me.lxc.artxeapi.utils.ArtxeChat.console;

public final class TheSieuToc extends JavaPlugin {

    public static String pluginVersion;
    public static ArtxeDebug pluginDebug;

    private Settings settings;
    private DonorLog donorLog;
    private Messages messages;
    private Ui ui;
    public HashMap<Player, List<CardInfo>> queue;

    public boolean hasAPIInfo;
    public CardCheckTask cardCheckTask;
    private static TheSieuToc instance;

    @Override
    public void onEnable() {
        javaCheck();
        preStartup();
        loadData();
        registerCommands();
        registerListeners();
    }

    @Override
    public void onDisable() {
        // Nothing Here?
    }

    private void javaCheck() {
        if (getVersion() < 16) {
            getLogger().info("No suitable jdk version found, disabling plugin");
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            getLogger().info("Found suitable jdk version, enabling plugin");
        }
    }

    private int getVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

    private void preStartup() {
        pluginVersion = getDescription().getVersion();
        console("§b  ________            _____ _               ______");
        console("§b  /_  __/ /_  ___     / ___/(____  __  __   /_  ______  _____");
        console("§b   / / / __ \\/ _ \\    \\__ \\/ / _ \\/ / / /    / / / __ \\/ ___/");
        console("§b  / / / / / /  __/   ___/ / /  __/ /_/ /    / / / /_/ / /__");
        console("§b /_/ /_/ /_/\\___/   /____/_/\\___/\\__._/    /_/  \\____/\\___/");
        console("               §f| §bVersion: §6" + pluginVersion + " §f| §bAuthor: §6LXC §f|");
        console("            §f| §aCopyright (c) 2018-" + ArtxeTime.getCurrentYear() + " §bTheSieuToc §f|");
        console("                         §cBug fixed by quanphungg_");
        instance = this;
        queue = new HashMap<>();
    }

    public void loadData() {
        settings = new Settings(new ArtxeYAML(this, getDataFolder() + File.separator + "settings", "general.yml", "settings"));
        hasAPIInfo = !(settings.iTheSieuTocKey.isEmpty() && settings.iTheSieuTocSecret.isEmpty());
        donorLog = new DonorLog(new File(getDataFolder() + File.separator + "logs", "donation.log"));
        pluginDebug = new ArtxeDebug(this, settings.debug);
        messages = new Messages(new ArtxeYAML(this, getDataFolder() + File.separator + "languages", "messages.yml", "languages"));
        ui = new Ui(new ArtxeYAML(this, getDataFolder() + File.separator + "ui", "chat.yml", "ui"));
        cardCheckTask = new CardCheckTask(this);
    }

    public void reload(short type) {
        switch (type) {
            case 1 -> settings.reload();
            case 2 -> messages.reload();
            case 3 -> ui.reload();
            default -> {
                settings.reload();
                hasAPIInfo = !(settings.iTheSieuTocKey.isEmpty() && settings.iTheSieuTocSecret.isEmpty());
                pluginDebug = new ArtxeDebug(this, settings.debug);
                messages.reload();
                ui.reload();
            }
        }
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("napthe")).setExecutor(new Commands());
        Objects.requireNonNull(getCommand("napthepe")).setExecutor(new PEDonateCommand());
    }

    private void registerListeners() {
        PluginManager bkplm = Bukkit.getPluginManager();
        bkplm.registerEvents(new PlayerChat(), this);
    }

    public static TheSieuToc getInstance() {
        return instance;
    }

    public Settings getSettings() {
        return this.settings;
    }

    public Messages getMessages() {
        return this.messages;
    }

    public Ui getUi() {
        return this.ui;
    }

    public DonorLog getDonorLog() {
        if (donorLog.logFile != null && Objects.requireNonNull(donorLog.logFile).exists())
            return this.donorLog;
        else {
            donorLog.createFile();
            return this.donorLog;
        }
    }

    public void submitCardDebug(Player player, InputCardHandler.LocalCardInfo info) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String hashedPin = DigestUtils.md5Hex(info.pin).toUpperCase();
            getLogger().warning("Bedrock player " + player.getName()
                    + " submit a card  with type=" + info.type + ", amount=" + info.amount
                    + ", seri=" + info.serial + ", pin=" + hashedPin);
        });
    }

    public String getRegex() {
        return "\\d+";
    }
}
