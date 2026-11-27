import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
/** Aplica a sintaxe de versões do próprio Fabric Loader. */
class AuditarVersao {
    public static void main(String[] args) throws Exception {
        int erros=0,total=0;
        for(String linha:Files.readAllLines(Path.of(args[0]))){
            String[] campos=linha.split("\\t",-1);
            String[] versoes=campos[4].isEmpty()?new String[0]:campos[4].split("\\|");
            boolean coincide=false;
            for(String v:versoes)for(String regra:campos[3].split("\\|")){
                if(VersionPredicate.parse(regra).test(Version.parse(v)))coincide=true;
            }
            total++;
            if(campos[2].equals("depends")?!coincide:coincide){
                System.out.println("ERRO "+campos[0]+" "+campos[1]+" "+campos[2]+" "+campos[3]+" ; encontrado: "+campos[4]);erros++;
            }
        }
        System.out.println("Predicados verificados: "+total+"; erros: "+erros);
        if(erros>0)System.exit(1);
    }
}
