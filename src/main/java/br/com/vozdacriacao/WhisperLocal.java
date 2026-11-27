package br.com.vozdacriacao;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.JsonParser;
/** Cliente de transcrição local, sem chave API, sem gravação em disco. */
final class WhisperLocal {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"LaCraft-Whisper");t.setDaemon(true);return t;});
    record Resultado(String texto,boolean fallback){}
    static CompletableFuture<Resultado> transcrever(short[] pcm){
        short[] copia=pcm.clone();
        return CompletableFuture.supplyAsync(()->{
            byte[] wav=null,body=null;
            try{
                wav=wav16(pcm);
                String b="LaCraft"+UUID.randomUUID();ByteArrayOutputStream out=new ByteArrayOutputStream();
                for(String[] p:new String[][]{{"language","pt"},{"temperature","0.0"},{"response_format","json"}}){
                    out.write(("--"+b+"\r\nContent-Disposition: form-data; name=\""+p[0]+"\"\r\n\r\n"+p[1]+"\r\n").getBytes(StandardCharsets.UTF_8));
                }
                out.write(("--"+b+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"voz.wav\"\r\nContent-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.UTF_8));out.write(wav);out.write(("\r\n--"+b+"--\r\n").getBytes(StandardCharsets.UTF_8));body=out.toByteArray();
                var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/inference")).timeout(Duration.ofSeconds(90)).header("Content-Type","multipart/form-data; boundary="+b).POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
                var response=HTTP.send(req,HttpResponse.BodyHandlers.ofInputStream());
                try(var in=response.body()){
                    if(response.statusCode()!=200)throw new IOException("Whisper HTTP "+response.statusCode());
                    byte[] data=in.readNBytes(32769);if(data.length>32768)throw new IOException("Resposta extensa");
                    String texto=JsonParser.parseString(new String(data,StandardCharsets.UTF_8)).getAsJsonObject().get("text").getAsString().trim();
                    return texto;
                }
            }catch(Exception e){throw new CompletionException(e);}
            finally{Arrays.fill(pcm,(short)0);if(wav!=null)Arrays.fill(wav,(byte)0);if(body!=null)Arrays.fill(body,(byte)0);}
        },WORK).handle((texto,erro)->{
            if(erro==null){Arrays.fill(copia,(short)0);return CompletableFuture.completedFuture(new Resultado(texto,false));}
            return ReconhecedorVoz.transcrever(copia).thenApply(t->new Resultado(t,true));
        }).thenCompose(x->x);
    }
    static byte[] wav16(short[] pcm){
        int n=pcm.length/3;
        ByteBuffer b=ByteBuffer.allocate(44+2*n).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36+2*n).put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16).put("data".getBytes(StandardCharsets.US_ASCII)).putInt(2*n);
        for(int i=0;i<n;i++){int j=i*3;b.putShort((short)(((int)pcm[j]+pcm[j+1]+pcm[j+2])/3));}
        return b.array();
    }
}
