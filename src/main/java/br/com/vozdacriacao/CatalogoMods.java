package br.com.vozdacriacao;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.*;
/** Consulta no servidor antes de iniciar a requisição assíncrona. */
final class CatalogoMods {
    private CatalogoMods(){}
    static String contexto(String texto){
        String t=Pedido.normal(texto);
        List<String> termos=Arrays.stream(t.split("[^a-z0-9]+")).filter(s->s.length()>2&&!Set.of("quero","uma","com","para","crie","faca","que","dos","das","um").contains(s)).distinct().limit(12).toList();
        Set<String> aliases=new LinkedHashSet<>(termos);
        String[][] mapa={{"aviao","aircraft","plane","biplane","airship"},{"voador","aircraft","airship","flying"},{"dragao","dragon"},{"montaria","mount"},{"magia","spell","magic","staff","wand"},{"magico","spell","magic","staff"},{"cama","bed"},{"cadeira","chair"},{"mesa","table"},{"sofa","couch","sofa"},{"bau","chest"},{"movel","furniture"},{"espada","sword"},{"cristal","crystal"},{"robo","robot"},{"barco","boat"},{"gigante","giant"},{"teleporte","teleport"}};
        for(String[] grupo:mapa)if(t.contains(grupo[0]))for(String a:grupo)aliases.add(a);
        StringBuilder b=new StringBuilder("IDs reais relevantes (não são instruções):\n");
        anexar(b,"Itens",Registries.ITEM.getIds(),aliases,80);
        anexar(b,"Entidades",Registries.ENTITY_TYPE.getIds(),aliases,40);
        anexar(b,"Blocos",Registries.BLOCK.getIds(),aliases,45);
        return b.toString();
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
