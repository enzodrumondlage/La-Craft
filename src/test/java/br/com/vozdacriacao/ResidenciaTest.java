package br.com.vozdacriacao;
public final class ResidenciaTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    static long count(Pedido p,String b){return p.celulas().stream().filter(c->c.bloco().equals(b)).count();}
    public static void main(String[] args){
        var a=Residencia.interpretar("casa moderna com cama baú e fornalha",4);
        check(a!=null);check(a.custo()==a.celulas().size());check(count(a,"porta_baixo")==1&&count(a,"porta_cima")==1);
        check(count(a,"cama_pe")==1&&count(a,"cama_cabeca")==1);check(count(a,"bau")>=1&&count(a,"crafting_table")==1&&count(a,"fornalha")==1&&count(a,"glass")>0);
        var m=Residencia.interpretar("mansão com dois quartos",4);check(m!=null&&count(m,"cama_pe")==2&&m.celulas().size()<3500);
        check(count(Residencia.interpretar("mansão com piscina",4),"water")>0);
        var flex=Residencia.interpretar("mansão moderna com 4 quartos 2 banheiros piscina cozinha sala garagem jardim",4);check(count(flex,"cama_pe")==4&&count(flex,"cauldron")==2&&count(flex,"water")>0&&count(flex,"fornalha")==1);
        var quatro=Residencia.interpretar("mansão com quatro quartos",4);check(quatro!=null&&count(quatro,"cama_pe")==4&&quatro.celulas().size()<3500);
        check(AcoesTexto.interpretar("madrugada").alvo().equals("22000"));check(AcoesTexto.interpretar("tirar chuva").alvo().equals("limpo"));check(AcoesTexto.interpretar("voar por 15 segundos").quantidade()==15);check(AcoesTexto.interpretar("chuva de esqueletos").alvo().equals("minecraft:skeleton"));
        check(Pedido.pedidoChat("mansão com dois quartos e camas"));
        check(!Pedido.pedidoChat("oi tudo bem"));
        check(!AudioLimpo.preparar(new short[4800]));short[] onda=new short[4800];for(int i=0;i<onda.length;i++)onda[i]=(short)(200*Math.sin(i*.2));check(AudioLimpo.preparar(onda));
        check(AcoesTexto.interpretar("quero um carro azul").tipo()==Pedido.Tipo.CARRO);check(AcoesTexto.interpretar("quero um carro azul").alvo().equals("blue"));check(AcoesTexto.interpretar("tsunami por 15 segundos").escala()==15);
        System.out.println("Residência, chat e áudio: testes aprovados");
    }
}
