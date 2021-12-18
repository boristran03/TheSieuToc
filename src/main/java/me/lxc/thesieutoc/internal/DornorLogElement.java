package me.lxc.thesieutoc.internal;

import com.google.gson.Gson;
import me.lxc.thesieutoc.TheSieuToc;
import me.lxc.thesieutoc.handlers.InputCardHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class DornorLogElement {

    private final Date date;
    private final String playerName;
    private final InputCardHandler.LocalCardInfo cardInfo;
    private final boolean success;
    private final String notes;

    public DornorLogElement(Date date, String playerName, InputCardHandler.LocalCardInfo cardInfo, boolean success, String notes) {
        this.date = date;
        this.playerName = playerName;
        this.cardInfo = cardInfo;
        this.success = success;
        this.notes = notes;
    }

    public DornorLogElement(String player, InputCardHandler.LocalCardInfo cardInfo, boolean success, String notes) {
        this(new Date(), player, cardInfo, success, notes);
    }

    public Date getDate() {
        return date;
    }

    public String getPlayerName() {
        return playerName;
    }

    public InputCardHandler.LocalCardInfo getCardInfo() {
        return cardInfo;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getNotes() {
        return notes;
    }

    public static DornorLogElement parse(String line) {
        String[] data = line.split("[|]");
        SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        Date date;
        try {
            date = parser.parse(data[0]);
        } catch (ParseException e) {
            date = new Date(0);
        }
        String playerName = data[1].replace(" NAME ", "").trim();
        String serial = data[2].replace(" SERIAL ", "").trim();
        String pin = data[3].replace(" PIN ", "").trim();
        String type = data[4].replace(" TYPE ", "").trim();
        int amount = Integer.parseInt(data[5].replace(" AMOUNT ", "").trim());
        InputCardHandler.LocalCardInfo card = new InputCardHandler.LocalCardInfo(type, amount, serial, pin);
        boolean success = Boolean.parseBoolean(data[6].replace(" SUCCESS ", "").trim());
        String notes = data[7].replace(" NOTES ", "").trim();

        return new DornorLogElement(date, playerName, card, success, notes);
    }

    public String toString() {
        Gson gson = new Gson();
        DonorObject object = new DonorObject();
        object.DATE = date.getTime();
        object.PLAYER = playerName;
        object.SUCCESS = success;
        object.CARD = cardInfo.type + " | " + cardInfo.amount + " | " + cardInfo.serial + " | " + cardInfo.pin;
        object.NOTES = notes;
        return gson.toJson(object);
    }

    public static List<DornorLogElement> loadFromFile(File file) {
        List<DornorLogElement> logContent = Collections.emptyList();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            logContent = br.lines().map(DornorLogElement::parse).collect(Collectors.toList());
        } catch (IOException e) {
            TheSieuToc.pluginDebug.debug("Cannot load log from file...");
        }
        return logContent;
    }
}


