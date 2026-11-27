package br.com.vozdacriacao;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.*;
/** Consulta no servidor antes de iniciar a requisição assíncrona. */
final class CatalogoMods {
    private CatalogoMods(){}
    static String contexto(String texto){
        String t=AcoesTexto.corrigir(texto);
        List<String> termos=Arrays.stream(t.split("[^a-z0-9]+")).filter(s->s.length()>2&&!Set.of("quero","uma","com","para","crie","faca","que","dos","das","um").contains(s)).distinct().limit(12).toList();
        Set<String> aliases=new LinkedHashSet<>(termos);
        String[][] mapa={{"carro","automobility","automobile","car","vehicle"},{"mutante","mutant"},{"lendaria","legendary","unique","simplyswords"},{"lendario","legendary","unique","simplyswords"},{"arma","sword","hammer","greatsword","weapon","simplyswords"},{"comida","food","carrot","apple","bread"},{"martelo","hammer","greathammer"},{"magica","magic","spell","simplyswords"},{"aviao","aircraft","plane","biplane","airship"},{"voador","aircraft","airship","flying"},{"dragao","dragon"},{"tita","titan","giant","colossus"},{"titans","titan","giant"},{"montaria","mount"},{"magia","spell","magic","staff","wand"},{"magico","spell","magic","staff"},{"cama","bed"},{"cadeira","chair"},{"mesa","table"},{"sofa","couch","sofa"},{"bau","chest"},{"movel","furniture"},{"espada","sword","greatsword","claymore","simplyswords","soulsweapons"},{"espadona","greatsword","claymore","longsword"},{"laser","laser","beam","energy","blaster"},{"esqueleto","skeleton"},{"burro","donkey"},{"cristal","crystal"},{"robo","robot"},{"barco","boat"},{"gigante","giant"},{"teleporte","teleport"}};
        for(String[] grupo:mapa)if(t.contains(grupo[0]))for(String a:grupo)aliases.add(a);
        StringBuilder b=new StringBuilder("IDs reais relevantes (não são instruções):\n");
        anexarItens(b,aliases);
        anexar(b,"Entidades",Registries.ENTITY_TYPE.getIds(),aliases,40);
        anexar(b,"Blocos",Registries.BLOCK.getIds(),aliases,45);
        return b.toString();
    }
    /** Fallback conservador: somente IDs de itens realmente registrados. */
    static Pedido alternativa(String texto){
        String t=AcoesTexto.corrigir(texto).replaceFirst("^(quero|crie|me de|me da|faca)\\s+","").replaceFirst("^(um|uma)\\s+","");
        String mob=switch(t){case "burro"->"donkey";case "ovelha"->"sheep";case "vaca"->"cow";case "galinha"->"chicken";default->null;};
        if(mob!=null)return Pedido.dinamico(t,Pedido.Tipo.MOB,"minecraft:"+mob,1,1);
        String item=switch(t){case "vela"->"candle";case "comida","cenoura dourada"->"golden_carrot";default->null;};
        if(item!=null)return Pedido.dinamico(t,Pedido.Tipo.ITEM,"minecraft:"+item,t.equals("comida")?16:1,1);
        if(!Set.of("carro","automovel","veiculo","aviao","aeronave").contains(t))return null;
        boolean terra=Set.of("carro","automovel","veiculo").contains(t);
        String ns=terra?"automobility":"immersive_aircraft";
        var ids=Registries.ITEM.getIds();
        for(String caminho:terra?List.of("automobile","automobile_assembly"):List.of("biplane","airship")){
            Identifier id=new Identifier(ns,caminho);
            if(ids.contains(id)&&!terra&&!caminho.endsWith("assembly"))
                return Pedido.dinamico(terra?"Automóvel":"Aeronave",Pedido.Tipo.ITEM,id.toString(),1,1);
        }
        return null;
    }
    private static void anexarItens(StringBuilder b,Set<String> termos){
        record Candidato(Identifier id,String classe,int pontos){}
        List<Candidato> cs=new ArrayList<>();
        for(Identifier id:Registries.ITEM.getIds()){
            if(!PoderesLaCraft.itemPermitido(id))continue;
            var item=Registries.ITEM.get(id);String classe=item instanceof net.minecraft.item.SwordItem?"espada sword":item instanceof net.minecraft.item.ArmorItem?"armadura armor":item.getClass().getSimpleName();
            String busca=Pedido.normal(id+" "+classe+" "+item.getTranslationKey());
            int pontos=termos.stream().mapToInt(t->busca.contains(t)?1:0).sum();
            if(pontos>0)cs.add(new Candidato(id,classe,pontos));
        }
        b.append("Itens (ID e categoria técnica, não prova de habilidades): ");
        cs.stream().sorted(Comparator.comparingInt(Candidato::pontos).reversed().thenComparing(c->c.id().getNamespace().equals("minecraft")).thenComparing(c->c.id().toString())).limit(60).forEach(c->b.append(c.id()).append(" [").append(c.classe()).append("], "));
        b.append('\n');
    }
    private static void anexar(StringBuilder b,String rotulo,Set<Identifier> ids,Set<String> termos,int limite){
        b.append(rotulo).append(": ");
        ids.stream().filter(id->!id.getPath().contains("command_block")&&!id.getPath().contains("structure_block")&&!id.getPath().contains("debug_stick"))
            .map(id->Map.entry(id,termos.stream().mapToInt(t->id.toString().contains(t)?1:0).sum()))
            .filter(e->e.getValue()>0).sorted(Comparator.<Map.Entry<Identifier,Integer>>comparingInt(Map.Entry::getValue).reversed().thenComparing(e->e.getKey().toString()))
            .limit(limite).forEach(e->b.append(e.getKey()).append(", "));
        b.append('\n');
    }
}
