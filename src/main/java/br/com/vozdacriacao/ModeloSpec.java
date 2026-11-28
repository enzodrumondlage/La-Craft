package br.com.vozdacriacao;
import com.google.gson.*;
import java.util.*;
/** Especificação declarativa de peças coloridas, não código executável. */
final class ModeloSpec {
    record Peca(float x,float y,float z,float sx,float sy,float sz,String bloco,String movimento){}
    private static final Set<String> MATERIAIS=Set.of("white_concrete","black_concrete","gray_concrete","light_gray_concrete","brown_concrete","orange_concrete","yellow_concrete","red_concrete","blue_concrete","green_concrete","pink_concrete","brown_terracotta","terracotta","black_wool","white_wool","brown_wool","glass","iron_block","gold_block","oak_planks","sea_lantern","quartz_block");
    private static float numero(JsonObject o,String k,float min,float max){
        var v=o.get(k).getAsJsonPrimitive();if(!v.isNumber())throw new IllegalArgumentException("Peça: número esperado");
        float n=v.getAsFloat();if(!Float.isFinite(n)||n<min||n>max)throw new IllegalArgumentException("Peça fora do tamanho permitido");return n;
    }
    static List<Peca> ler(JsonArray a){
        if(a==null||a.size()<3||a.size()>24)throw new IllegalArgumentException("Modelo: use 3 a 24 peças");
        List<Peca> pecas=new ArrayList<>();
        for(var el:a){
            JsonObject o=el.getAsJsonObject();String bloco=o.get("bloco").getAsString(),mov=o.has("movimento")?o.get("movimento").getAsString():"fixo";
            if(!MATERIAIS.contains(bloco)||!Set.of("fixo","balanco","giro","flutuar").contains(mov))throw new IllegalArgumentException("Material ou movimento inválido");
            pecas.add(new Peca(numero(o,"x",-10,10),numero(o,"y",0,12),numero(o,"z",-10,10),numero(o,"sx",.2f,8),numero(o,"sy",.2f,8),numero(o,"sz",.2f,8),bloco,mov));
        }
        return List.copyOf(pecas);
    }
    static Pedido pedido(String nome,JsonObject plano){
        JsonArray a=plano.getAsJsonArray("pecas");List<Peca> p=ler(a);
        List<Pedido.Celula> visual=new ArrayList<>();
        for(Peca v:p)for(int x=0;x<Math.ceil(v.sx());x++)for(int y=0;y<Math.ceil(v.sy());y++)for(int z=0;z<Math.ceil(v.sz());z++){
            if(visual.size()<800)visual.add(new Pedido.Celula(Math.round(v.x()+x),Math.round(v.y()+y),Math.round(v.z()+z),v.bloco()));
        }
        return new Pedido(nome,80+p.size()*4,Pedido.Tipo.MODELO,visual,a.toString(),1,1);
    }
    static List<Peca> ler(String json){if(json.length()>12000)throw new IllegalArgumentException("Modelo grande demais");return ler(JsonParser.parseString(json).getAsJsonArray());}
}
