package br.com.vozdacriacao;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import java.nio.file.*;
/** Smoke test opt-in: janela real, tela de configuração e framebuffer renderizado. */
final class TesteGrafico {
    private static int ticks;
    static void registrar(){
        if(!Boolean.getBoolean("lacraft.testeGrafico"))return;
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(!(mc.currentScreen instanceof TitleScreen)&&ticks==0)return;
            ticks++;
            if(ticks==20)mc.setScreen(new TelaLaCraft(mc.currentScreen));
            if(ticks==60){
                try{
                    Path p=Path.of("lacraft-teste-grafico.png");try(var img=ScreenshotRecorder.takeScreenshot(mc.getFramebuffer())){img.writeTo(p);}
                    if(Files.size(p)<1024)throw new IllegalStateException("Framebuffer vazio");
                    Files.writeString(Path.of("lacraft-grafico-ok.txt"),"Tela de configuração renderizada");
                    mc.scheduleStop();
                }catch(Exception e){throw new IllegalStateException("Falha no teste gráfico",e);}
            }
        });
    }
}
