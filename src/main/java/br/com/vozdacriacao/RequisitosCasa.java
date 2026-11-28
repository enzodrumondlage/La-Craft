package br.com.vozdacriacao;
import java.util.*;
import java.util.regex.*;
/** Verifica marcadores materiais; não certifica conectividade ou cômodos completos. */
final class RequisitosCasa {
    private static int qtd(String t,String termo){
        var m=Pattern.compile("(\\d{1,2}|um|uma|dois|duas|tres|quatro|cinco|seis|sete|oito|nove|dez)\\s+"+termo+"\\b").matcher(t);
        if(!m.find())return -1;
        return switch(m.group(1)){case "um","uma"->1;case "dois","duas"->2;case "tres"->3;case "quatro"->4;case "cinco"->5;case "seis"->6;case "sete"->7;case "oito"->8;case "nove"->9;case "dez"->10;default->Integer.parseInt(m.group(1));};
    }
    static void verificar(String t,Pedido p){
        int quartos=qtd(t,"quartos?"),banheiros=qtd(t,"banheiros?");
        var cs=p.celulas();
        Set<Pedido.Celula> conjunto=new HashSet<>(cs);
        for(var c:cs){
            String par=switch(c.bloco()){case "porta_baixo"->"porta_cima";case "porta_cima"->"porta_baixo";case "cama_pe"->"cama_cabeca";case "cama_cabeca"->"cama_pe";default->null;};
            if(par==null)continue;
            int dy=c.bloco().equals("porta_baixo")?1:c.bloco().equals("porta_cima")?-1:0;
            int dz=c.bloco().equals("cama_pe")?-1:c.bloco().equals("cama_cabeca")?1:0;
            if(!conjunto.contains(new Pedido.Celula(c.x(),c.y()+dy,c.z()+dz,par)))throw new IllegalArgumentException("Peça incompleta ou orientação inválida: "+c.bloco());
        }
        if(quartos>=0&&cs.stream().filter(c->c.bloco().equals("cama_pe")).count()!=quartos)throw new IllegalArgumentException("Quantidade de camas não corresponde aos "+quartos+" quartos solicitados");
        if(banheiros>=0&&cs.stream().filter(c->c.bloco().equals("cauldron")).count()!=banheiros)throw new IllegalArgumentException("Quantidade de marcadores de banheiro não corresponde a "+banheiros);
        if(Pedido.palavra(t,"porta dupla")){
            var portas=cs.stream().filter(c->c.bloco().equals("porta_baixo")).toList();
            boolean dupla=portas.stream().anyMatch(a->portas.stream().anyMatch(b->a.y()==b.y()&&a.z()==b.z()&&Math.abs(a.x()-b.x())==1));
            if(!dupla)throw new IllegalArgumentException("Porta dupla ausente");
        }
        if(Pedido.palavra(t,"janelas pretas")&&cs.stream().noneMatch(c->c.bloco().equals("black_stained_glass")))throw new IllegalArgumentException("Vidro preto ausente");
        if(qtd(t,"andares?")>=2&&cs.stream().noneMatch(c->c.bloco().equals("quartz_stairs")))throw new IllegalArgumentException("Escada ausente no projeto de vários andares");
    }
}
