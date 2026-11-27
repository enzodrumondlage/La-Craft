package br.com.vozdacriacao;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.slf4j.LoggerFactory;

/** Reconhecimento offline. O áudio nunca é enviado à API nem salvo em disco. */
public final class ReconhecedorVoz {
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"LaCraft-Voz");t.setDaemon(true);return t;});
    private static final String NOME="vosk-model-small-pt-0.3";
    private static Model modelo;
    private ReconhecedorVoz(){}
    public static CompletableFuture<String> transcrever(short[] pcm){
        return CompletableFuture.supplyAsync(()->{
            try{
                if(modelo==null)modelo=new Model(preparar().toString());
                try(Recognizer r=new Recognizer(modelo,48000f)){
                    StringJoiner texto=new StringJoiner(" ");
                    for(int off=0;off<pcm.length;off+=4800){
                        short[] parte=Arrays.copyOfRange(pcm,off,Math.min(off+4800,pcm.length));
                        try{if(r.acceptWaveForm(parte,parte.length))texto.add(extrair(r.getResult()));}
                        finally{Arrays.fill(parte,(short)0);}
                    }
                    texto.add(extrair(r.getFinalResult()));
                    return texto.toString().trim();
                }
            }catch(Exception|LinkageError e){
                LoggerFactory.getLogger("LaCraft").warn("Falha ao carregar ou usar reconhecimento offline",e);
                throw new CompletionException(e);
            }finally{Arrays.fill(pcm,(short)0);}
        },WORK);
    }
    private static String extrair(String json){return JsonParser.parseString(json).getAsJsonObject().get("text").getAsString().trim();}
    private static Path preparar() throws IOException{
        Path base=FabricLoader.getInstance().getGameDir().resolve("config/lacraft/voz").toAbsolutePath().normalize();
        Path destino=base.resolve(NOME), pronto=destino.resolve(".lacraft-pronto");
        if(Files.isRegularFile(pronto)&&Files.isRegularFile(destino.resolve("am/final.mdl")))return destino;
        Files.createDirectories(base);
        Path temp=Files.createTempDirectory(base,"modelo-");
        try{
            try(InputStream in=ReconhecedorVoz.class.getResourceAsStream("/vozdacriacao/modelo-pt.zip")){
                if(in==null)throw new IOException("Modelo português ausente no JAR.");
                try(ZipInputStream zip=new ZipInputStream(in)){
                    long total=0;ZipEntry e;int entradas=0;byte[] buffer=new byte[8192];
                    while((e=zip.getNextEntry())!=null){
                        if(++entradas>2000)throw new IOException("Muitos arquivos");
                        Path out=temp.resolve(e.getName()).normalize();
                        if(!out.startsWith(temp.resolve(NOME)))throw new IOException("Caminho inválido no modelo");
                        if(e.isDirectory()){Files.createDirectories(out);continue;}
                        Files.createDirectories(out.getParent());
                        try(OutputStream os=Files.newOutputStream(out,StandardOpenOption.CREATE_NEW)){
                            int n;while((n=zip.read(buffer))!=-1){total+=n;if(total>200_000_000L)throw new IOException("Modelo grande demais");os.write(buffer,0,n);}
                        }
                    }
                }
            }
            Path extraido=temp.resolve(NOME);
            if(!Files.isRegularFile(extraido.resolve("am/final.mdl")))throw new IOException("Modelo inválido");
            Files.writeString(extraido.resolve(".lacraft-pronto"),"0.3");
            if(Files.exists(destino))throw new IOException("Modelo local incompleto; remova config/lacraft/voz/"+NOME+" e tente novamente");
            Files.move(extraido,destino);
            return destino;
        }finally{
            try(var walk=Files.walk(temp)){for(Path p:walk.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
