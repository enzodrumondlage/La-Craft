package br.com.vozdacriacao;
import com.google.gson.*;
import java.util.*;
/** Plano declarativo de um ser terrestre original. Não carrega código da IA. */
record SerSpec(boolean montavel,boolean anda,String visual){
    SerSpec{if(visual.length()>12000||VisualArtefato.ler(visual).pecas().size()<3)throw new IllegalArgumentException("Ser: modelo de 3 a 16 peças");}
    static SerSpec ler(String s){
        if(s.length()>13000)throw new IllegalArgumentException("Ser: dados longos");
        var p=JsonParser.parseString(s).getAsJsonObject();
        var m=p.get("montavel").getAsJsonPrimitive();var a=p.get("anda").getAsJsonPrimitive();
        if(!m.isBoolean()||!a.isBoolean())throw new IllegalArgumentException("Ser: booleanos esperados");
        return new SerSpec(m.getAsBoolean(),a.getAsBoolean(),p.getAsJsonArray("visual").toString());
    }
    static Pedido pedido(String nome,JsonObject p){
        SerSpec s=ler(p.toString());JsonObject dados=new JsonObject();
        dados.addProperty("montavel",s.montavel());dados.addProperty("anda",s.anda());dados.add("visual",JsonParser.parseString(s.visual()));
        return new Pedido(nome,160+VisualArtefato.ler(s.visual()).pecas().size()*5,Pedido.Tipo.SER,List.of(),dados.toString(),1,1);
    }
}
