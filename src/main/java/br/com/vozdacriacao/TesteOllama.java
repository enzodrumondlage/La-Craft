package br.com.vozdacriacao;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import com.google.gson.JsonParser;
final class TesteOllama {
    static CompletableFuture<String> testar(String modelo){
        return CompletableFuture.supplyAsync(()->{
            try{
                if(!OllamaLocal.iniciar())return "Ollama indisponível; instale o serviço local.";
                var http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
                var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:11434/api/tags")).timeout(Duration.ofSeconds(5)).GET().build();
                var res=http.send(req,HttpResponse.BodyHandlers.ofInputStream());
                try(var in=res.body()){
                    if(res.statusCode()!=200)return "Ollama: HTTP "+res.statusCode();
                    byte[] bytes=in.readNBytes(262145);if(bytes.length>262144)return "Lista de modelos muito grande.";
                    var modelos=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("models");
                    for(var e:modelos)if(e.getAsJsonObject().get("name").getAsString().equals(modelo))return "Ollama conectado; modelo instalado.";
                    return "Ollama conectado; modelo não instalado.";
                }
            }catch(Exception ex){return "Ollama indisponível em 127.0.0.1:11434.";}
        });
    }
}
