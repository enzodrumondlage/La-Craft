package br.com.vozdacriacao;
import com.google.gson.*;
import java.util.*;
/** Consulta declarativa, limitada, sem executar código. */
final class ConsultaIA {
    record Consulta(String categoria,List<String> termos){}
    static Consulta ler(JsonObject o){
        String categoria=o.get("categoria").getAsString();
        if(!Set.of("item","mob","bloco").contains(categoria))throw new IllegalArgumentException("Categoria de pesquisa inválida");
        var a=o.getAsJsonArray("termos");
        if(a==null||a.isEmpty()||a.size()>8)throw new IllegalArgumentException("Pesquisa: 1 a 8 termos");
        List<String> termos=new ArrayList<>();
        for(var e:a){
            if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Termo inválido");
            String t=e.getAsString().trim();
            if(t.isEmpty()||t.length()>80||t.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Termo inválido");
            termos.add(t);
        }
        return new Consulta(categoria,List.copyOf(termos));
    }
}
