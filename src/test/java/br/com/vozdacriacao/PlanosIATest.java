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
        eq(IAConstrucoes.validar("{\"nome\":\"Voo\",\"tipo\":\"voo\"}").custo(),120);
        eq(IAConstrucoes.validar("{\"nome\":\"Mob\",\"tipo\":\"mob\",\"id\":\"minecraft:piglin\",\"quantidade\":2,\"escala\":3}").custo(),180);
        rejeitar("{\"nome\":\"Mob\",\"tipo\":\"mob\",\"id\":\"minecraft:piglin\",\"quantidade\":99,\"escala\":3}");
        Pedido a=IAConstrucoes.validar("{\"nome\":\"Cristal\",\"tipo\":\"artefato\",\"cor\":\"#22ff88\",\"efeitos\":[\"cura\",\"respirar\"],\"duracao\":30,\"recarga\":5}");
        eq(a.custo(),140);eq(ArtefatoSpec.ler(a.alvo()).efeitos().size(),2);
        if(ArtefatoSpec.offline("cristal verde de cura e velocidade")==null)throw new AssertionError();
        rejeitar("{\"nome\":\"Cristal\",\"tipo\":\"artefato\",\"cor\":\"#22ff88\",\"efeitos\":[\"comando\"],\"duracao\":30,\"recarga\":5}");
        var seq=IAConstrucoes.validar("{\"nome\":\"Plano\",\"tipo\":\"sequencia\",\"acoes\":[{\"nome\":\"Noite\",\"tipo\":\"tempo\",\"hora\":13000},{\"nome\":\"Voo\",\"tipo\":\"voo\",\"duracao\":15}]}");
        eq(seq.acoes().size(),2);eq(seq.acoes().get(1).quantidade(),15);
        rejeitar("{\"nome\":\"Erro\",\"tipo\":\"tempo\",\"hora\":24000}");
        rejeitar("{\"nome\":\"Erro\",\"tipo\":\"clima\",\"clima\":\"qualquer\",\"duracao\":10}");
        if(!AcoesTexto.interpretar("teremoto").alvo().equals("tremor"))throw new AssertionError("Correção de terremoto");
        if(!AcoesTexto.interpretar("buraco negro").alvo().equals("gravidade"))throw new AssertionError("Gravidade");
        if(AcoesTexto.interpretar("nao quero terremoto")!=null)throw new AssertionError("Negação");
        Pedido casa=IAConstrucoes.validar("{\"nome\":\"Casa IA\",\"tipo\":\"arquitetura\",\"caixas\":["+caixa("white_concrete",10,2,3,false)+","+caixa("air",1,1,1,false)+"]}");
        if(casa.tipo()!=Pedido.Tipo.RESIDENCIA||casa.celulas().stream().anyMatch(c->c.bloco().equals("air")))throw new AssertionError("Arquitetura");
        try{IAConstrucoes.validarIntencao("macaco","minecraft:donkey",Pedido.dinamico("Burro",Pedido.Tipo.MOB,"minecraft:donkey",1,3));throw new AssertionError("Macaco trocado por burro");}catch(IllegalArgumentException esperado){}
        String peca="{\"x\":0,\"y\":0,\"z\":0,\"sx\":1,\"sy\":2,\"sz\":1,\"bloco\":\"brown_concrete\",\"movimento\":\"balanco\"}";
        Pedido modelo=IAConstrucoes.validar("{\"nome\":\"Macaco\",\"tipo\":\"modelo\",\"pecas\":["+peca+","+peca+","+peca+"]}");
        if(modelo.tipo()!=Pedido.Tipo.MODELO||ModeloSpec.ler(modelo.alvo()).size()!=3)throw new AssertionError("Modelo");
        IAConstrucoes.validarIntencao("macaco animado","",modelo);
        System.out.println("Planos IA: testes aprovados");
    }
}
