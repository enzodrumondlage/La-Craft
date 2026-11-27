package br.com.vozdacriacao;
import java.util.*;
/** Componentes confirmados no código do Automobility v0.4.2.b+1.20.1. */
final class CarroSpec {
    static final Set<String> CORES=Set.of("white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black");
    static String cor(String t){
        String[][] mapa={{"vermelho|vermelha","red"},{"azul claro","light_blue"},{"azul","blue"},{"verde","green"},{"roxo|roxa","purple"},{"preto|preta","black"},{"branco|branca","white"},{"amarelo|amarela","yellow"},{"rosa","pink"},{"laranja","orange"},{"cinza","gray"},{"marrom","brown"}};
        for(String[] c:mapa)if(Pedido.palavra(t,c[0]))return c[1];return "red";
    }
    static Pedido pedido(String cor){
        if(!CORES.contains(cor))throw new IllegalArgumentException("Cor de carro inválida");
        return new Pedido("Automóvel montado",80,Pedido.Tipo.CARRO,List.of(),cor,1,1);
    }
    static Pedido interpretar(String texto){
        String t=Pedido.normal(texto).replaceFirst("^(quero|crie|me de|me da|faca)\\s+","").replaceFirst("^(um|uma)\\s+","");
        if(!t.matches("^(carro|automovel)(?: (?:vermelho|vermelha|azul claro|azul|verde|roxo|roxa|preto|preta|branco|branca|amarelo|amarela|rosa|laranja|cinza|marrom))?(?: funcional)?$"))return null;
        return pedido(cor(t));
    }
}
