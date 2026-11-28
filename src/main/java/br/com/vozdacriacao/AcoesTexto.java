package br.com.vozdacriacao;
import java.util.*;
import java.util.regex.*;
/** Ações simples sem inferência; pedidos compostos passam pelo planejador. */
final class AcoesTexto {
    static String corrigir(String frase){
        String t=Pedido.normal(frase);
        String[][] pares={{"teremoto|terremotu|terramoto","terremoto"},{"furacau|furacaoo|furacao","furacao"},{"manção|mancao|manssao","mansao"},{"dragau|dragaoo","dragao"},{"giganti","gigante"},{"metioro|metioros|meteoros","meteoros"},{"tsunamy|tsuname","tsunami"},{"aviao","aviao"},{"wheater|witer|wihter","wither"},{"onibuz","onibus"}};
        for(String[] p:pares)t=t.replaceAll("(?<![a-z0-9_:/.-])(?:"+p[0]+")(?![a-z0-9_:/.-])",p[1]);
        return t;
    }
    static Pedido interpretar(String frase){
        String t=corrigir(frase).replaceFirst("^lacraft:\\s*","").replaceFirst("^(quero|faca|deixe|coloque|crie|invoque)\\s+","").replaceFirst("^(ficar|que fique)\\s+","").trim();
        Pedido carro=CarroSpec.interpretar(frase);if(carro!=null)return carro;
        if(t.matches("^(?:um )?(tsunami|furacao|tornado|terremoto|buraco negro|chuva de meteoros)(?: por [0-9]{1,2} segundos?)?$")){
            String tipo=t.contains("tsunami")?"onda":t.contains("terremoto")?"tremor":t.contains("buraco negro")?"gravidade":t.contains("meteoros")?"meteoros":"redemoinho";
            Matcher dur=Pattern.compile("por ([0-9]{1,2}) segundos?").matcher(t);
            int segundos=dur.find()?Integer.parseInt(dur.group(1)):10;
            if(segundos<1||segundos>30)throw new IllegalArgumentException("Evento: 1 a 30 segundos");
            return new Pedido(switch(tipo){case "onda"->"Tsunami";case "tremor"->"Terremoto";case "gravidade"->"Buraco negro";case "meteoros"->"Chuva de meteoros";default->"Furacão";},80,Pedido.Tipo.FENOMENO,List.of(),tipo,1,segundos);
        }
        String hora=switch(t){case "dia","amanhecer","manha","nascer do sol"->"0";case "meio-dia","meio dia"->"6000";case "tarde"->"9000";case "por do sol","entardecer"->"12000";case "noite","anoitecer"->"13000";case "meia-noite","meia noite"->"18000";case "madrugada"->"22000";default->null;};
        if(hora!=null)return Pedido.acao("Horário",Pedido.Tipo.TEMPO,hora,1,1);
        String clima=switch(t){case "chuva","chover"->"chuva";case "tempestade","trovoada"->"tempestade";case "ceu limpo","tire a chuva","tirar chuva","tirar a chuva","pare de chover","parar chuva","sem chuva","sol"->"limpo";default->null;};
        if(clima!=null)return Pedido.acao("Clima "+clima,Pedido.Tipo.CLIMA,clima,1,300);
        Matcher voo=Pattern.compile("^(?:me deixe |me faca |me permita )?(?:voar|voo)(?: por (\\d{1,4}) segundos?)?$").matcher(t);
        if(voo.matches()){int segundos=voo.group(1)==null?300:Integer.parseInt(voo.group(1));if(segundos<1||segundos>600)throw new IllegalArgumentException("Duração de voo: 1 a 600 segundos");return new Pedido("Voo por "+segundos+" segundos",120,Pedido.Tipo.VOO,List.of(),"duracao",segundos,1);}
        if(t.startsWith("chuva de ")){
            String id=t.substring(9).trim();
            String mob=switch(id){case "burro","burros"->"donkey";case "esqueleto","esqueletos"->"skeleton";case "galinha","galinhas"->"chicken";case "porco","porcos"->"pig";case "vaca","vacas"->"cow";case "zumbi","zumbis"->"zombie";default->null;};
            if(mob!=null)return new Pedido("Chuva de "+id,50,Pedido.Tipo.CHUVA_MOB,List.of(),"minecraft:"+mob,8,10);
            String item=switch(id){case "diamante","diamantes"->"diamond";case "esmeralda","esmeraldas"->"emerald";case "espada","espadas"->"iron_sword";default->null;};
            if(item!=null)return new Pedido("Chuva de "+id,50,Pedido.Tipo.CHUVA_ITEM,List.of(),"minecraft:"+item,16,10);
        }
        return null;
    }
}
