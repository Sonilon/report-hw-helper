package dev.reportwatcher.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;

public class ModConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("reportwatcher");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("reportwatcher.json");

    // Интервал проверки в секундах
    public static int CHECK_INTERVAL_SECONDS = 30;

    // Команда открытия меню репортов
    public static String REPORT_COMMAND = "reportlist";

    // Минимальный перевес ПЖ над ЛЖ у заявителя для триггера
    public static int MIN_PJ_ADVANTAGE = 1;

    // Включить звуковой сигнал при нахождении репорта
    public static boolean PLAY_SOUND = true;

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            save();
            return;
        }
        try (Reader reader = new FileReader(file)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                CHECK_INTERVAL_SECONDS = data.checkIntervalSeconds;
                REPORT_COMMAND = data.reportCommand;
                MIN_PJ_ADVANTAGE = data.minPjAdvantage;
                PLAY_SOUND = data.playSound;
            }
        } catch (Exception e) {
            LOGGER.error("[ReportWatcher] Ошибка загрузки конфига: " + e.getMessage());
        }
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.checkIntervalSeconds = CHECK_INTERVAL_SECONDS;
        data.reportCommand = REPORT_COMMAND;
        data.minPjAdvantage = MIN_PJ_ADVANTAGE;
        data.playSound = PLAY_SOUND;
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            LOGGER.error("[ReportWatcher] Ошибка сохранения конфига: " + e.getMessage());
        }
    }

    private static class ConfigData {
        int checkIntervalSeconds = 30;
        String reportCommand = "reportlist";
        int minPjAdvantage = 1;
        boolean playSound = true;
    }
}
