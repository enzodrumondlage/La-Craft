package br.com.vozdacriacao;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
/** Inicia apenas a instalação oficial local existente, nunca código sugerido pela IA. */
final class OllamaLocal {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static Process iniciado;
    static boolean disponivel(){
        try{
            var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:11434/api/version")).timeout(Duration.ofSeconds(2)).GET().build();
            return HTTP.send(req,HttpResponse.BodyHandlers.discarding()).statusCode()==200;
        }catch(Exception e){return false;}
    }
    static Path executavel(){
        String os=System.getProperty("os.name","").toLowerCase(java.util.Locale.ROOT);
        if(!os.contains("win"))return null;
        String local=System.getenv("LOCALAPPDATA");
        if(local==null||local.isBlank())return null;
        Path p=Path.of(local,"Programs","Ollama","ollama.exe");
        return Files.isRegularFile(p)?p:null;
    }
    static synchronized boolean iniciar() throws InterruptedException {
        if(disponivel())return true;
        Path exe=executavel();if(exe==null)return false;
        try{
            if(iniciado==null||!iniciado.isAlive()){
                ProcessBuilder pb=new ProcessBuilder(exe.toString(),"serve");
                pb.environment().put("OLLAMA_HOST","127.0.0.1:11434");
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                iniciado=pb.start();
            }
            for(int i=0;i<20;i++){
                if(disponivel())return true;
                if(!iniciado.isAlive())return false;
                TimeUnit.MILLISECONDS.sleep(500);
            }
        }catch(IOException e){return false;}
        return false;
    }
    static synchronized void parar(){
        if(iniciado!=null&&iniciado.isAlive())iniciado.destroy();
        iniciado=null;
    }
}
