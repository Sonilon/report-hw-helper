package dev.reportwatcher.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoreParser {

    private static final Pattern COMPLAINANT_PATTERN = Pattern.compile(
        "Заявитель:.*?\\s(\\S+)\\s*\\(ПЖ:\\s*(\\d+),\\s*ЛЖ:\\s*(\\d+)\\s*\\|\\s*ПП:\\s*(\\d+),\\s*ЛП:\\s*(\\d+)\\)"
    );
    private static final Pattern SUSPECT_PATTERN = Pattern.compile(
        "Подозреваемый:.*?\\s(\\S+)\\s*\\(ПЖ:\\s*(\\d+),\\s*ЛЖ:\\s*(\\d+)\\s*\\|\\s*ПП:\\s*(\\d+),\\s*ЛП:\\s*(\\d+)\\)"
    );
    private static final Pattern SERVER_PATTERN = Pattern.compile("Сервер:\\s*(.+)");
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("\\[вес\\s*(\\d+)\\]");
    private static final Pattern DATE_CREATED_PATTERN = Pattern.compile("Жалоба создана:\\s*(.+)");
    private static final Pattern DATE_CHECK_PATTERN = Pattern.compile("Последняя проверка:\\s*(.+)");

    public static ReportData parse(ItemStack stack, int slot) {
        if (stack == null || stack.isEmpty()) return null;

        boolean isFree;
        if (stack.getItem() == Items.ZOMBIE_HEAD) {
            isFree = true;
        } else if (stack.getItem() == Items.WITHER_SKELETON_SKULL) {
            isFree = false;
        } else {
            return null;
        }

        List<String> loreLines = getLoreAsPlainText(stack);
        String titleLine = stack.getName() != null ? stack.getName().getString() : "";

        List<String> allLines = new ArrayList<>();
        allLines.add(titleLine);
        allLines.addAll(loreLines);

        ReportData data = new ReportData();
        data.slot = slot;
        data.isFree = isFree;
        data.suspectHasDonate = isSuspectDonater(stack);

        for (String line : allLines) {
            Matcher cm = COMPLAINANT_PATTERN.matcher(line);
            if (cm.find()) {
                data.complainantName = cm.group(1);
                data.complainantPJ   = parseInt(cm.group(2));
                data.complainantLJ   = parseInt(cm.group(3));
                data.complainantPP   = parseInt(cm.group(4));
                data.complainantLP   = parseInt(cm.group(5));
            }

            Matcher sm = SUSPECT_PATTERN.matcher(line);
            if (sm.find()) {
                data.suspectName = sm.group(1);
                data.suspectPJ   = parseInt(sm.group(2));
                data.suspectLJ   = parseInt(sm.group(3));
                data.suspectPP   = parseInt(sm.group(4));
                data.suspectLP   = parseInt(sm.group(5));
            }

            Matcher srvM = SERVER_PATTERN.matcher(line);
            if (srvM.find()) data.server = srvM.group(1).trim();

            Matcher wM = WEIGHT_PATTERN.matcher(line);
            if (wM.find()) data.weight = parseInt(wM.group(1));

            Matcher dcM = DATE_CREATED_PATTERN.matcher(line);
            if (dcM.find()) data.createdDate = dcM.group(1).trim();

            Matcher dlM = DATE_CHECK_PATTERN.matcher(line);
            if (dlM.find()) data.lastCheckDate = dlM.group(1).trim();
        }

        if (data.complainantName.isEmpty() && data.suspectName.isEmpty()) return null;
        return data;
    }

    private static List<String> getLoreAsPlainText(ItemStack stack) {
        List<String> result = new ArrayList<>();
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return result;
        NbtCompound display = nbt.getCompound("display");
        if (display == null || !display.contains("Lore")) return result;
        NbtList loreList = display.getList("Lore", 8);
        for (int i = 0; i < loreList.size(); i++) {
            String raw = loreList.getString(i);
            try {
                Text text = Text.Serializer.fromJson(raw);
                if (text != null) result.add(text.getString());
            } catch (Exception e) {
                result.add(raw.replaceAll("§[0-9a-fk-or]", ""));
            }
        }
        return result;
    }

    private static boolean isSuspectDonater(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return false;
        NbtCompound display = nbt.getCompound("display");
        if (display == null || !display.contains("Lore")) return false;
        NbtList loreList = display.getList("Lore", 8);
        for (int i = 0; i < loreList.size(); i++) {
            String raw = loreList.getString(i);
            if (!raw.contains("Подозреваемый")) continue;
            int guillemet = raw.indexOf("\u00ab");
            if (guillemet == -1) continue;
            int checkFrom = Math.max(0, guillemet - 5);
            String slice = raw.substring(checkFrom, guillemet + 1);
            String withoutNormal = slice.replace("§7", "").replace("§f", "").replace("§r", "");
            if (withoutNormal.contains("§")) return true;
        }
        return false;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }
}
