package br.com.vozdacriacao;

public final class PlanosIATest {
    private static String caixa(String bloco,int x2,int y2,int z2,boolean oco){
        return "{\"x1\":0,\"y1\":0,\"z1\":0,\"x2\":"+x2+",\"y2\":"+y2+",\"z2\":"+z2+",\"bloco\":\""+bloco+"\",\"oco\":"+oco+"}";
    }
    private static String plano(String caixas){return "{\"nome\":\"Teste\",\"caixas\":["+caixas+"]}";}
    private static void eq(int a,int b){if(a!=b)throw new AssertionError(a+" != "+b);}
    private static void rejeitar(String texto){
        try{IAConstrucoes.validar(texto);}catch(RuntimeException e){return;}
        throw new AssertionError("Plano inválido foi aceito");
    }
    public static void main(String[] args){
        Pedido solido=IAConstrucoes.validar(plano(caixa("stone_bricks",2,2,2,false)));
        eq(solido.custo(),27);eq(solido.celulas().size(),27);
        eq(IAConstrucoes.validar(plano(caixa("glass",2,2,2,true))).custo(),26);
        eq(IAConstrucoes.validar(plano(caixa("glass",1,1,1,false)+","+caixa("oak_planks",1,1,1,false))).custo(),8);
        rejeitar(plano(caixa("command_block",1,1,1,false)));
        rejeitar(plano(caixa("stone_bricks",7,1,1,false)));
        rejeitar(plano(caixa("stone_bricks",-1,1,1,false)));
        rejeitar(plano(""));
        rejeitar(plano("{\"x1\":-6,\"y1\":0,\"z1\":-6,\"x2\":6,\"y2\":20,\"z2\":6,\"bloco\":\"stone_bricks\",\"oco\":false}"));
        rejeitar(plano(caixa("stone_bricks",1,1,1,false).replace("\"x1\":0","\"x1\":0.5")));
        System.out.println("Planos IA: 9 testes aprovados");
    }
}
