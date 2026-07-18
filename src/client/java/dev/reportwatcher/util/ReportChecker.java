package dev.reportwatcher.util;

import dev.reportwatcher.ReportWatcherClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import dev.reportwatcher.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Получает список слотов из открытого ScreenHandler и анализирует репорты.
 * Приоритет: первая строка (слоты 0–8).
 */
public class ReportChecker {

    private static final int FIRST_ROW_END = 9; // слоты 0..8 — первая строка

    /**
     * Вызывается из миксина, когда сервер прислал содержимое экрана.
     */
    public static void analyzeScreen(ScreenHandler handler) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        List<ReportData> triggered = new ArrayList<>();
        List<ReportData> firstRowTriggered = new ArrayList<>();

        List<Slot> slots = handler.slots;
        int total = slots.size();

        // Анализируем все слоты (кроме инвентаря игрока — последние 36)
        int guiSlotCount = Math.max(0, total - 36);

        for (int i = 0; i < guiSlotCount; i++) {
            ItemStack stack = slots.get(i).getStack();
            ReportData data = LoreParser.parse(stack, i);
            if (data == null) continue;
            if (!data.isFree) continue; // Занятые (wither skull) пропускаем

            if (data.matchesTrigger()) {
                triggered.add(data);
                if (i < FIRST_ROW_END) {
                    firstRowTriggered.add(data);
                }
            }
        }

        if (triggered.isEmpty()) return;

        // Сортируем: сначала первый ряд, потом по слоту
        triggered.sort((a, b) -> {
            boolean aFirst = a.slot < FIRST_ROW_END;
            boolean bFirst = b.slot < FIRST_ROW_END;
            if (aFirst && !bFirst) return -1;
            if (!aFirst && bFirst) return 1;
            return Integer.compare(a.slot, b.slot);
        });

        // Выводим сообщения
        sendAlerts(client, triggered, firstRowTriggered.size());

        // Звук
        if (ModConfig.PLAY_SOUND && client.player != null) {
            client.execute(() -> {
                client.player.playSound(
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.MASTER,
                    1.0f, 1.5f
                );
            });
        }
    }

    private static void sendAlerts(MinecraftClient client, List<ReportData> reports, int firstRowCount) {
        client.execute(() -> {
            if (client.player == null) return;

            // Шапка
            client.player.sendMessage(net.minecraft.text.Text.literal(
                "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            ), false);

            client.player.sendMessage(net.minecraft.text.Text.literal(
                "  §b✦ §fReportWatcher §7нашёл §e" + reports.size()
                + " §7подходящ" + plural(reports.size(), "ий", "их", "их") + " репорт"
                + plural(reports.size(), "", "а", "ов")
                + (firstRowCount > 0 ? " §a(§l" + firstRowCount + " в первом ряду§a)" : "")
            ), false);

            client.player.sendMessage(net.minecraft.text.Text.literal(
                "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            ), false);

            for (int idx = 0; idx < reports.size(); idx++) {
                ReportData r = reports.get(idx);
                boolean priority = r.slot < FIRST_ROW_END;

                String priorityTag = priority ? " §a§l[ПРИОРИТЕТ]" : "";
                String slotTag = " §8(слот " + r.slot + ")";

                // Заголовок репорта
                client.player.sendMessage(net.minecraft.text.Text.literal(
                    "§e#" + (idx + 1) + priorityTag + slotTag
                ), false);

                // Заявитель
                client.player.sendMessage(net.minecraft.text.Text.literal(
                    "  §7Заявитель: §f" + r.complainantName
                    + " §8| §aПЖ:§f" + r.complainantPJ
                    + " §8| §cЛЖ:§f" + r.complainantLJ
                    + " §8| §7ПП:§f" + r.complainantPP
                ), false);

                // Подозреваемый
                client.player.sendMessage(net.minecraft.text.Text.literal(
                    "  §7Подозреваемый: §f" + r.suspectName
                    + " §8| §7Донат: §c✗"
                    + " §8| §7ПП: §c" + r.suspectPP
                    + " §8| §7ПЖ:§f" + r.suspectPJ + "§8/§cЛЖ:§f" + r.suspectLJ
                ), false);

                // Сервер и вес
                if (!r.server.isEmpty()) {
                    client.player.sendMessage(net.minecraft.text.Text.literal(
                        "  §7Сервер: §b" + r.server
                        + (r.weight > 0 ? " §8| §7Вес: §e" + r.weight : "")
                    ), false);
                }

                // Дата создания
                if (!r.createdDate.isEmpty()) {
                    client.player.sendMessage(net.minecraft.text.Text.literal(
                        "  §7Создана: §f" + r.createdDate
                        + (r.lastCheckDate.isEmpty() ? "" : " §8| §7Проверка: §f" + r.lastCheckDate)
                    ), false);
                }

                // Причины срабатывания
                StringBuilder reasons = new StringBuilder("  §7Триггеры: ");
                if (r.complainantPJ > r.complainantLJ) {
                    reasons.append("§a✔ ПЖ>ЛЖ §8| ");
                }
                if (!r.suspectHasDonate) {
                    reasons.append("§a✔ Нет доната §8| ");
                }
                if (r.suspectPP == 0) {
                    reasons.append("§a✔ Нет ПП");
                }
                client.player.sendMessage(net.minecraft.text.Text.literal(reasons.toString()), false);

                if (idx < reports.size() - 1) {
                    client.player.sendMessage(net.minecraft.text.Text.literal("  §8──────────────────────────"), false);
                }
            }

            client.player.sendMessage(net.minecraft.text.Text.literal(
                "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            ), false);
        });
    }

    /** Простое склонение по числу (1 / 2-4 / 5+) */
    private static String plural(int n, String one, String few, String many) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 14) return many;
        if (mod10 == 1) return one;
        if (mod10 >= 2 && mod10 <= 4) return few;
        return many;
    }
}
