package br.com.vozdacriacao;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;
final class DiagnosticoLaCraft {
    static boolean gerar(){
        try{
            var loader=FabricLoader.getInstance();var cfg=ConfigLaCraft.obter();
            var linhas=new ArrayList<String>();
            linhas.add("La Craft — diagnóstico local (sem chaves, áudio ou mensagens)");
            linhas.add("Java: "+System.getProperty("java.version"));
            linhas.add("Memória máxima JVM MiB: "+Runtime.getRuntime().maxMemory()/1048576);
            linhas.add("Provedor: "+cfg.provedor+" | Configurado: "+IAConstrucoes.ativo());
            linhas.add("Microfone habilitado: "+cfg.voz+" | Confirmação: "+cfg.confirmarVoz);
            linhas.add("Mods carregados:");
            loader.getAllMods().stream().map(m->m.getMetadata().getId()+" "+m.getMetadata().getVersion().getFriendlyString()).sorted().forEach(linhas::add);
            Files.createDirectories(loader.getConfigDir());Files.write(loader.getConfigDir().resolve("lacraft-diagnostico.txt"),linhas);
            return true;
        }catch(Exception e){return false;}
    }
}
