package br.com.vozdacriacao;
import com.google.gson.*;
import java.util.*;
/** Combina qualquer conjunto de IDs validados, alternando tipos. */
final class ChuvaSpec {
    record Gota(boolean mob,String id){}
    static Pedido plano(String nome,JsonObject p){
        var grupos=p.getAsJsonArray("grupos");
        if(grupos==null||grupos.isEmpty()||grupos.size()>8)throw new IllegalArgumentException("Chuva: 1 a 8 grupos");
        int dur=p.get("duracao").getAsBigDecimal().intValueExact();
        if(dur<1||dur>30)throw new IllegalArgumentException("Chuva: duração de 1 a 30 segundos");
        List<List<Gota>> listas=new ArrayList<>();int total=0;
        for(var e:grupos){
            var o=e.getAsJsonObject();String tipo=o.get("tipo").getAsString(),id=o.get("id").getAsString();
            int qtd=o.get("quantidade").getAsBigDecimal().intValueExact();
            if(!Set.of("mob","item").contains(tipo)||!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||id.length()>120||qtd<1||qtd>32)throw new IllegalArgumentException("Grupo inválido");
            total+=qtd;if(total>32)throw new IllegalArgumentException("Chuva: até 32 aparições no total");
            listas.add(Collections.nCopies(qtd,new Gota(tipo.equals("mob"),id)));
        }
        if(total>dur*4)throw new IllegalArgumentException("Aumente a duração para caber no limite de quatro aparições por segundo");
        List<String> ordem=new ArrayList<>();
        for(int i=0;i<32;i++)for(var l:listas)if(i<l.size()){var g=l.get(i);ordem.add((g.mob()?"m@":"i@")+g.id());}
        return new Pedido(nome,50+total*2,Pedido.Tipo.CHUVA_MISTA,List.of(),String.join(",",ordem),total,dur);
    }
    static List<Gota> ler(String s){
        if(s.length()>4096)throw new IllegalArgumentException("Chuva inválida");
        List<Gota> r=new ArrayList<>();
        for(String t:s.split(",")){
            if(!t.matches("[mi]@[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Gota inválida");
            r.add(new Gota(t.startsWith("m@"),t.substring(2)));
        }
        if(r.isEmpty()||r.size()>32)throw new IllegalArgumentException("Chuva inválida");
        return List.copyOf(r);
    }
}
