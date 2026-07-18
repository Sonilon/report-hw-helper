package dev.reportwatcher.mixin;

import dev.reportwatcher.util.ReportChecker;
import dev.reportwatcher.util.ReportScheduler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ScreenHandlerMixin {

    /**
     * Перехватываем setScreen — вызывается когда сервер открывает GUI игроку.
     * Если мы ждём экран репортов — читаем его содержимое и отменяем открытие.
     */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        // Нас интересует только HandledScreen (GUI с инвентарём)
        if (!(screen instanceof HandledScreen<?>)) return;

        // Если мы не ждём экран от /reportlist — не трогаем
        if (!ReportScheduler.WAITING_FOR_SCREEN.get()) return;

        // Сбрасываем флаг ожидания
        ReportScheduler.WAITING_FOR_SCREEN.set(false);

        HandledScreen<?> handledScreen = (HandledScreen<?>) screen;
        ScreenHandler handler = handledScreen.getScreenHandler();

        // Анализируем содержимое
        ReportChecker.analyzeScreen(handler);

        // Закрываем пакет закрытия окна, чтобы сервер знал что мы "закрыли"
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.closeHandledScreen();
        }

        // Отменяем открытие GUI — игрок ничего не видит
        ci.cancel();
    }
}
