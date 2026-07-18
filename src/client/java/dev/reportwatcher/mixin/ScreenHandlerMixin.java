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

    @Inject(method = "method_1507", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (!(screen instanceof HandledScreen<?>)) return;
        if (!ReportScheduler.WAITING_FOR_SCREEN.get()) return;

        ReportScheduler.WAITING_FOR_SCREEN.set(false);

        HandledScreen<?> handledScreen = (HandledScreen<?>) screen;
        ScreenHandler handler = handledScreen.getScreenHandler();

        ReportChecker.analyzeScreen(handler);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.closeHandledScreen();
        }

        ci.cancel();
    }
}
