package br.com.vozdacriacao;
import de.kherud.llama.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
/** Inferência JNI na JVM. Pesos oficiais fixos, sem servidor, sem chave, sem código gerado. */
final class IAIntegrada {
    private static final String ARQUIVO="qwen2.5-1.5b-instruct-q4_k_m.gguf";
    private static final String HASH="6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e";
    private static final long MAX=1_250_000_000L;
    private static final Path PASTA=FabricLoader.getInstance().getConfigDir().resolve("lacraft-ia");
    private static LlamaModel modelo;
    private static String hash(Path p)throws Exception{
        var d=MessageDigest.getInstance("SHA-256");
        try(var in=Files.newInputStream(p)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}
        return HexFormat.of().formatHex(d.digest());
    }
    private static Path preparar()throws Exception{
        Files.createDirectories(PASTA);Path destino=PASTA.resolve(ARQUIVO);
        if(Files.isRegularFile(destino)&&Files.size(destino)<MAX&&HASH.equals(hash(destino)))return destino;
        if(Files.getFileStore(PASTA).getUsableSpace()<MAX+100_000_000L)throw new IOException("IA integrada: espaço em disco insuficiente.");
        Path tmp=Files.createTempFile(PASTA,"modelo-",".part");
        try{
            var http=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();
            var req=HttpRequest.newBuilder(URI.create("https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/"+ARQUIVO)).timeout(Duration.ofMinutes(15)).GET().build();
            var r=http.send(req,HttpResponse.BodyHandlers.ofInputStream());
            try(var in=r.body();var out=Files.newOutputStream(tmp)){
                if(r.statusCode()!=200)throw new IOException("IA integrada: download HTTP "+r.statusCode());
                byte[] b=new byte[65536];long total=0;int n;
                while((n=in.read(b))!=-1){total+=n;if(total>MAX)throw new IOException("IA integrada: tamanho inválido");out.write(b,0,n);}
            }
            if(!HASH.equals(hash(tmp)))throw new IOException("IA integrada: SHA-256 não corresponde ao modelo oficial");
            Files.move(tmp,destino,StandardCopyOption.REPLACE_EXISTING);return destino;
        }finally{Files.deleteIfExists(tmp);}
    }
    static synchronized String gerar(String sistema,String pedido)throws Exception{
        try{
            if(modelo==null)modelo=new LlamaModel(new ModelParameters().setModel(preparar().toString()).setCtxSize(8192).setThreads(Math.max(2,Math.min(4,Runtime.getRuntime().availableProcessors()-2))).setGpuLayers(0).setParallel(1).setBatchSize(256).setJsonSchema("{\"type\":\"object\"}"));
            // ChatML do Qwen; não deixa texto inserir delimitadores de papel.
            String prompt="<|im_start|>system\n"+limpar(sistema)+"<|im_end|>\n<|im_start|>user\n"+limpar(pedido)+"<|im_end|>\n<|im_start|>assistant\n";
            String resposta=modelo.complete(new InferenceParameters(prompt).setNPredict(3072).setTemperature(.15f).setStopStrings("<|im_end|>","<|endoftext|>"));
            if(resposta==null||resposta.length()>65536)throw new IOException("IA integrada: resposta inválida");
            return resposta.strip();
        }catch(LinkageError e){throw new IOException("IA integrada: biblioteca nativa incompatível com o sistema",e);}
    }
    private static String limpar(String s){return s.replace("<|","< |");}
    static synchronized void fechar(){if(modelo!=null){modelo.close();modelo=null;}}
}
