package br.com.vozdacriacao;
import com.google.gson.*;
import java.util.*;
/** Geometria e textura declarativas persistentes; sem código arbitrário. */
record VisualArtefato(List<Peca> pecas){
    record Peca(float x,float y,float z,float sx,float sy,float sz,int cor,int detalhe,String pixels,String animacao){}
    private static final String LISO="00000000".repeat(8);
    private static final String RUNAS="00010000"+"00111000"+"01101100"+"11000110"+"01101100"+"00111000"+"00010000"+"00000000";
    VisualArtefato{pecas=List.copyOf(pecas);if(pecas.isEmpty()||pecas.size()>16)throw new IllegalArgumentException("Visual: 1 a 16 peças");}
    private static float n(JsonObject o,String k,float min,float max){
        var v=o.get(k).getAsJsonPrimitive();if(!v.isNumber())throw new IllegalArgumentException("Visual: número esperado");
        float f=v.getAsFloat();if(!Float.isFinite(f)||f<min||f>max)throw new IllegalArgumentException("Visual: dimensão fora do limite");return f;
    }
    private static int cor(JsonObject o,String k){
        String s=o.get(k).getAsString();if(!s.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Visual: cor inválida");return Integer.parseInt(s.substring(1),16);
    }
    static VisualArtefato ler(JsonArray a){
        if(a==null||a.size()<1||a.size()>16)throw new IllegalArgumentException("Visual: peças inválidas");
        List<Peca> ps=new ArrayList<>();
        for(var e:a){
            JsonObject o=e.getAsJsonObject();String anim=o.has("animacao")?o.get("animacao").getAsString():"fixo";
            String pixels=o.has("pixels")?o.get("pixels").getAsString():LISO;
            if(!Set.of("fixo","giro","balanco","flutuar","pulsar","roda").contains(anim)||!pixels.matches("[012]{64}|[012]{256}"))throw new IllegalArgumentException("Visual: animação ou textura 8x8 ou 16x16 inválida");
            float x=n(o,"x",0,16),y=n(o,"y",0,16),z=n(o,"z",0,16),sx=n(o,"sx",.25f,16),sy=n(o,"sy",.25f,16),sz=n(o,"sz",.25f,16);
            if(x+sx>16||y+sy>16||z+sz>16)throw new IllegalArgumentException("Visual: peça fora do cubo 16");
            ps.add(new Peca(x,y,z,sx,sy,sz,cor(o,"cor"),o.has("detalhe")?cor(o,"detalhe"):cor(o,"cor"),pixels,anim));
        }
        return new VisualArtefato(ps);
    }
    static VisualArtefato ler(String s){if(s.length()>12000)throw new IllegalArgumentException("Visual longo");return ler(JsonParser.parseString(s).getAsJsonArray());}
    static VisualArtefato legado(int c){
        return new VisualArtefato(List.of(new Peca(5,4,5,6,8,6,c,0xffe9aa,RUNAS,"pulsar"),new Peca(6,2,6,4,2,4,0x544264,c,LISO,"fixo"),new Peca(6,12,6,4,2,4,0x544264,c,LISO,"fixo")));
    }
}
