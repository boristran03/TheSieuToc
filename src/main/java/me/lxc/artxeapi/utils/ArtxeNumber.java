package me.lxc.artxeapi.utils;

public class ArtxeNumber {

    private ArtxeNumber() {}

    public static boolean isInteger(String o) {
        try {
            Integer.parseInt(o);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
