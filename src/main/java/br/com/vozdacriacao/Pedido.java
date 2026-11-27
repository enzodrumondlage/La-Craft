package br.com.vozdacriacao;
import java.text.Normalizer;
import java.util.Locale;
/** Catálogo procedural de teste. Não é um LLM. */
public record Pedido(String nome,int custo,Tipo tipo){
    public enum Tipo { FONTE, CASA, TORRE, FOGUETE, CASTELO, BURROS, CAPIVARA, ARCO, ARMADURA }
    public static Pedido interpretar(String texto){
        String t=Normalizer.normalize(texto.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");
        if(t.contains("castelo"))return new Pedido("Castelo",500,Tipo.CASTELO);
        if(t.contains("foguete"))return new Pedido("Foguete decorativo",300,Tipo.FOGUETE);
        if(t.contains("capivara"))return new Pedido("Estátua de capivara",100,Tipo.CAPIVARA);
        if(t.contains("burro"))return new Pedido("Chuva de burros",50,Tipo.BURROS);
        if(t.contains("armadura"))return new Pedido("Armadura esmeralda",200,Tipo.ARMADURA);
        if(t.contains("arco"))return new Pedido("Arco mágico",200,Tipo.ARCO);
        if(t.contains("torre"))return new Pedido("Torre",150,Tipo.TORRE);
        if(t.contains("casa"))return new Pedido("Casa",100,Tipo.CASA);
        if(t.contains("fonte")||t.contains("particula"))return new Pedido("Fonte de partículas",12,Tipo.FONTE);
        return null;
    }
}
