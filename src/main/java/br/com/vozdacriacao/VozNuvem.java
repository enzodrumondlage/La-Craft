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

public final class VozNuvem {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    public static CompletableFuture<String> transcrever(short[] pcm){
        String chave=ConfigLaCraft.chave(ConfigLaCraft.obter());
        return CompletableFuture.supplyAsync(()->{
            byte[] wav=null,body=null;
            try{
                wav=wav(pcm);
                String b="LaCraft"+UUID.randomUUID();ByteArrayOutputStream out=new ByteArrayOutputStream();
                for(String[] p:new String[][]{{"model","gpt-4o-mini-transcribe"},{"language","pt"},{"prompt","Minecraft, pedidos em português brasileiro, casa, castelo, piglin, Warden, capivara, espada, arco, barco, poderes, voar."}}){
                    out.write(("--"+b+"\r\nContent-Disposition: form-data; name=\""+p[0]+"\"\r\n\r\n"+p[1]+"\r\n").getBytes(StandardCharsets.UTF_8));
                }
                out.write(("--"+b+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"voz.wav\"\r\nContent-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.UTF_8));out.write(wav);out.write(("\r\n--"+b+"--\r\n").getBytes(StandardCharsets.UTF_8));body=out.toByteArray();
                var req=HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/audio/transcriptions")).timeout(Duration.ofSeconds(45)).header("Authorization","Bearer "+chave).header("Content-Type","multipart/form-data; boundary="+b).POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
                var response=HTTP.send(req,HttpResponse.BodyHandlers.ofInputStream());
                try(InputStream in=response.body()){
                    if(response.statusCode()!=200)throw new IOException("Transcrição HTTP "+response.statusCode());
                    byte[] bytes=in.readNBytes(32769);if(bytes.length>32768)throw new IOException("Resposta longa");
                    return JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject().get("text").getAsString().trim();
                }
            }catch(Exception e){throw new CompletionException(e);}
            finally{Arrays.fill(pcm,(short)0);if(wav!=null)Arrays.fill(wav,(byte)0);if(body!=null)Arrays.fill(body,(byte)0);}
        });
    }
    static byte[] wav(short[] pcm){
        ByteBuffer b=ByteBuffer.allocate(44+2*pcm.length).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36+2*pcm.length).put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short)1).putShort((short)1).putInt(48000).putInt(96000).putShort((short)2).putShort((short)16).put("data".getBytes(StandardCharsets.US_ASCII)).putInt(2*pcm.length);
        for(short s:pcm)b.putShort(s);return b.array();
    }
}
