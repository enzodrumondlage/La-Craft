package br.com.vozdacriacao;
import com.google.gson.*;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
/** LLM opcional via serviço local. Somente caixas de voxels validadas, nunca comandos. */
public final class IAConstrucoes {
    private static final AtomicBoolean OCUPADO=new AtomicBoolean();
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"LaCraft-IA");t.setDaemon(true);return t;});
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final Set<String> BLOCOS=Set.of("stone_bricks","cobblestone","oak_planks","spruce_planks","birch_planks","glass","obsidian","quartz_block","bricks","sandstone","white_concrete","black_concrete","red_concrete","blue_concrete","light_blue_concrete","green_concrete","yellow_concrete","orange_concrete","purple_concrete","pink_concrete","brown_terracotta","terracotta","gold_block","iron_block","sea_lantern","glowstone","white_wool","brown_wool","black_wool","moss_block","grass_block");
    private static final String SISTEMA="""
        Planeje desejos fictícios para Minecraft, em português.
        Responda somente um objeto JSON: {"nome":"nome curto","caixas":[{"x1":-2,"y1":0,"z1":-2,"x2":2,"y2":4,"z2":2,"bloco":"stone_bricks","oco":true}]}.
        Use até 64 caixas, no máximo 1500 voxels únicos. Coordenadas inteiras x,z entre -6 e 6, y de 0 a 20, mínimos antes dos máximos.
        oco=true significa apenas a superfície da caixa; false significa volume preenchido.
        Para itens existentes use {"nome":"nome","tipo":"item","id":"minecraft:diamond","quantidade":8}.
        Para mobs existentes use {"nome":"nome","tipo":"mob","id":"minecraft:piglin","quantidade":1,"escala":3}; escala de 1 a 4, quantidade 1 a 8.
        Para um item mágico combinável use {"nome":"Cristal do oceano","tipo":"artefato","cor":"#33AAFF","efeitos":["respirar","cura","brilho"],"duracao":30,"recarga":5}. Efeitos válidos: cura, respirar, velocidade, salto, invisivel, resistencia, levitacao, brilho, nuvem, musica. Combine até 5 efeitos. Duração em segundos 5 a 120, recarga 2 a 30. Para poderes use {"nome":"nome","tipo":"voo"} ou velocidade, salto, invisivel, laser, espada, arco, armadura.
        Não gere comandos, código ou mecanismos reais. Para estruturas use caixas e para desejos não suportados devolva caixas vazias.
        Use exclusivamente os blocos permitidos. Camadas e caixas posteriores substituem voxels anteriores.
        """;
    private IAConstrucoes(){}
    public static boolean ativo(){
        var c=ConfigLaCraft.obter();
        return c.provedor.equals("OLLAMA")&&!c.modelo.toLowerCase(Locale.ROOT).contains("cloud")||c.provedor.equals("OPENAI")&&c.permitirNuvem&&!ConfigLaCraft.chave(c).isBlank();
    }
    public static CompletableFuture<Pedido> planejar(String texto){
        if(!ativo()||!OCUPADO.compareAndSet(false,true))return null;
        final ConfigLaCraft cfg=ConfigLaCraft.obter().copia();
        return CompletableFuture.supplyAsync(()->{
            try{
                JsonObject req=new JsonObject();
                req.addProperty("model",cfg.modelo);req.addProperty("system",SISTEMA+" Blocos: "+String.join(", ",BLOCOS));
                req.addProperty("prompt",texto);req.addProperty("stream",false);req.addProperty("format","json");
                JsonObject opts=new JsonObject();opts.addProperty("temperature",0.2);opts.addProperty("num_predict",2048);req.add("options",opts);
                boolean nuvem=cfg.provedor.equals("OPENAI");
                HttpRequest.Builder builder;
                if(nuvem){
                    if(!cfg.permitirNuvem)throw new IllegalArgumentException("Nuvem não autorizada");
                    JsonObject payload=new JsonObject();payload.addProperty("model",cfg.modelo);
                    JsonArray msgs=new JsonArray();
                    JsonObject sys=new JsonObject();sys.addProperty("role","system");sys.addProperty("content",SISTEMA+" Blocos: "+String.join(", ",BLOCOS));msgs.add(sys);
                    JsonObject user=new JsonObject();user.addProperty("role","user");user.addProperty("content",texto);msgs.add(user);payload.add("messages",msgs);
                    JsonObject formato=new JsonObject();formato.addProperty("type","json_object");payload.add("response_format",formato);
                    payload.addProperty("max_completion_tokens",2048);req=payload;
                    builder=HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/chat/completions")).header("Authorization","Bearer "+ConfigLaCraft.chave(cfg));
                }else builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:11434/api/generate"));
                HttpRequest http=builder.timeout(Duration.ofSeconds(90)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(req.toString())).build();
                var response=HTTP.send(http,HttpResponse.BodyHandlers.ofInputStream());
                try(InputStream in=response.body()){
                    if(response.statusCode()!=200)throw new IllegalArgumentException("IA HTTP "+response.statusCode());
                    byte[] b=in.readNBytes(65537);if(b.length>65536)throw new IllegalArgumentException("Resposta muito grande");
                    JsonObject body=JsonParser.parseString(new String(b,StandardCharsets.UTF_8)).getAsJsonObject();
                    String json=nuvem?body.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString():body.get("response").getAsString();
                    return validar(json);
                }
            }catch(Exception e){throw new CompletionException(e);}
            finally{OCUPADO.set(false);}
        },WORK);
    }
    static Pedido validar(String json){
        if(json.length()>32768)throw new IllegalArgumentException("JSON grande demais");
        JsonObject plano=JsonParser.parseString(json).getAsJsonObject();
        String nome=plano.get("nome").getAsString().replaceAll("[\\p{Cntrl}§]","").trim();
        if(nome.isEmpty())nome="Construção IA";
        nome=nome.substring(0,Math.min(48,nome.length()));
        if(plano.has("tipo")){
            String tipo=plano.get("tipo").getAsString();
            if(tipo.equals("artefato"))return ArtefatoSpec.validar(nome,plano);
            if(tipo.equals("item")||tipo.equals("mob")){
                Pedido.Tipo t=tipo.equals("item")?Pedido.Tipo.ITEM:Pedido.Tipo.MOB;
                return Pedido.dinamico(nome,t,plano.get("id").getAsString(),num(plano,"quantidade",1,t==Pedido.Tipo.MOB?8:64),t==Pedido.Tipo.MOB?num(plano,"escala",1,4):1);
            }
            Pedido p=switch(tipo){
                case "voo"->new Pedido(nome,120,Pedido.Tipo.VOO);
                case "velocidade"->new Pedido(nome,60,Pedido.Tipo.VELOCIDADE);
                case "salto"->new Pedido(nome,60,Pedido.Tipo.SALTO);
                case "invisivel"->new Pedido(nome,80,Pedido.Tipo.INVISIVEL);
                case "laser"->new Pedido(nome,150,Pedido.Tipo.LASER);
                case "espada"->new Pedido(nome,180,Pedido.Tipo.ESPADA);
                case "arco"->new Pedido(nome,200,Pedido.Tipo.ARCO);
                case "armadura"->new Pedido(nome,200,Pedido.Tipo.ARMADURA);
                default->null;
            };
            if(p!=null)return p;
            if(!tipo.equals("voxel"))throw new IllegalArgumentException("Tipo desconhecido");
        }
        JsonArray caixas=plano.getAsJsonArray("caixas");
        if(caixas.isEmpty()||caixas.size()>64)throw new IllegalArgumentException("Quantidade de caixas inválida");
        record Pos(int x,int y,int z){}
        Map<Pos,Pedido.Celula> voxels=new LinkedHashMap<>();
        for(JsonElement elemento:caixas){
            JsonObject c=elemento.getAsJsonObject();
            int x1=num(c,"x1",-6,6),x2=num(c,"x2",-6,6),z1=num(c,"z1",-6,6),z2=num(c,"z2",-6,6),y1=num(c,"y1",0,20),y2=num(c,"y2",0,20);
            if(x1>x2||y1>y2||z1>z2)throw new IllegalArgumentException("Caixa invertida");
            String bloco=c.get("bloco").getAsString();if(!BLOCOS.contains(bloco))throw new IllegalArgumentException("Bloco proibido");
            if(!c.get("oco").getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("oco inválido");
            boolean oco=c.get("oco").getAsBoolean();
            for(int x=x1;x<=x2;x++)for(int y=y1;y<=y2;y++)for(int z=z1;z<=z2;z++){
                if(oco&&x>x1&&x<x2&&y>y1&&y<y2&&z>z1&&z<z2)continue;
                voxels.put(new Pos(x,y,z),new Pedido.Celula(x,y,z,bloco));
                if(voxels.size()>1500)throw new IllegalArgumentException("Limite de voxels");
            }
        }
        if(voxels.isEmpty())throw new IllegalArgumentException("Plano vazio");
        return new Pedido(nome,voxels.size(),Pedido.Tipo.VOXEL,List.copyOf(voxels.values()));
    }
    private static int num(JsonObject o,String k,int min,int max){
        var e=o.get(k).getAsJsonPrimitive();if(!e.isNumber())throw new IllegalArgumentException("Coordenada não numérica");
        int n=e.getAsBigDecimal().intValueExact();if(n<min||n>max)throw new IllegalArgumentException("Coordenada fora do limite");return n;
    }
}
