package br.com.vozdacriacao;
import com.google.gson.*;
import java.util.*;
/** Geometria e mobilidade declarativas e limitadas. */
record SerSpec(boolean montavel,boolean anda,String visual,String modo,float velocidade,float largura,float altura,float comprimento){
    SerSpec(boolean m,boolean a,String v){this(m,a,v,"criatura",.25f,1f,1.5f,1f);}
    SerSpec{
        if(visual.length()>12000||VisualArtefato.ler(visual).pecas().size()<3)throw new IllegalArgumentException("Ser: modelo de 3 a 16 peças");
        if(!Set.of("criatura","veiculo").contains(modo)||modo.equals("veiculo")&&(!montavel||anda))throw new IllegalArgumentException("Modo inválido");
        validar(velocidade,.12f,.45f);validar(largura,.6f,2.8f);validar(altura,.6f,3f);validar(comprimento,.6f,3.6f);
    }
    private static void validar(float n,float min,float max){if(!Float.isFinite(n)||n<min||n>max)throw new IllegalArgumentException("Dimensão ou velocidade fora do limite");}
    private static float n(JsonObject p,String k,float padrao){
        if(!p.has(k))return padrao;
        var e=p.get(k).getAsJsonPrimitive();if(!e.isNumber())throw new IllegalArgumentException("Número inválido: "+k);return e.getAsFloat();
    }
    static SerSpec ler(String s){
        if(s.length()>14000)throw new IllegalArgumentException("Ser: dados longos");
        var p=JsonParser.parseString(s).getAsJsonObject();
        var m=p.get("montavel").getAsJsonPrimitive();var a=p.get("anda").getAsJsonPrimitive();
        if(!m.isBoolean()||!a.isBoolean())throw new IllegalArgumentException("Ser: booleanos esperados");
        return new SerSpec(m.getAsBoolean(),a.getAsBoolean(),p.getAsJsonArray("visual").toString(),p.has("modo")?p.get("modo").getAsString():"criatura",n(p,"velocidade",.25f),n(p,"largura",1),n(p,"altura",1.5f),n(p,"comprimento",1));
    }
    String json(){
        JsonObject dados=new JsonObject();dados.addProperty("montavel",montavel);dados.addProperty("anda",anda);dados.add("visual",JsonParser.parseString(visual));dados.addProperty("modo",modo);dados.addProperty("velocidade",velocidade);dados.addProperty("largura",largura);dados.addProperty("altura",altura);dados.addProperty("comprimento",comprimento);return dados.toString();
    }
    static Pedido pedido(String nome,JsonObject p){
        SerSpec s=ler(p.toString());
        return new Pedido(nome,160+VisualArtefato.ler(s.visual()).pecas().size()*5,Pedido.Tipo.SER,List.of(),s.json(),1,1);
    }
}
