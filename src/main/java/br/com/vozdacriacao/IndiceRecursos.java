package br.com.vozdacriacao;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.nio.file.*;
import java.util.*;
/** Índice único por sessão. Traduções dos recursos dos mods, não apenas IDs. */
final class IndiceRecursos {
    record Entrada(String tipo,Identifier id,String nome,String texto){}
    private static List<Entrada> indice;
    static void limpar(){indice=null;}
    static List<Entrada> obter(){
        if(indice!=null)return indice;
        Map<String,String> pt=new HashMap<>(),en=new HashMap<>();
        for(var mod:FabricLoader.getInstance().getAllMods())for(Path root:mod.getRootPaths()){
            Path assets=root.resolve("assets");
            if(!Files.isDirectory(assets))continue;
            try(var dirs=Files.list(assets)){
                for(Path ns:dirs.filter(Files::isDirectory).toList()){
                    ler(ns.resolve("lang/pt_br.json"),pt);ler(ns.resolve("lang/en_us.json"),en);
                }
            }catch(Exception ignored){}
        }
        List<Entrada> lista=new ArrayList<>();
        for(Identifier id:Registries.ITEM.getIds()){
            if(!PoderesLaCraft.itemPermitido(id)||id.getNamespace().equals("vozdacriacao"))continue;
            var item=Registries.ITEM.get(id);
            add(lista,"item",id,item.getTranslationKey(),item.getClass().getSimpleName(),pt,en);
        }
        for(Identifier id:Registries.ENTITY_TYPE.getIds())add(lista,"mob",id,Registries.ENTITY_TYPE.get(id).getTranslationKey(),"",pt,en);
        for(Identifier id:Registries.BLOCK.getIds())add(lista,"bloco",id,Registries.BLOCK.get(id).getTranslationKey(),"",pt,en);
        indice=List.copyOf(lista);return indice;
    }
    private static void ler(Path p,Map<String,String> map){
        try{
            if(!Files.isRegularFile(p)||Files.size(p)>4_000_000)return;
            var obj=JsonParser.parseString(Files.readString(p)).getAsJsonObject();
            for(var e:obj.entrySet())if(e.getValue().isJsonPrimitive()&&e.getValue().getAsJsonPrimitive().isString()){
                String v=e.getValue().getAsString().replaceAll("[\\p{Cntrl}§]"," ");
                if(v.length()<=120)map.putIfAbsent(e.getKey(),v);
            }
        }catch(Exception ignored){}
    }
    static String normal(String s){return Pedido.normal(s).replaceAll("[^a-z0-9]+"," ").trim();}
    private static void add(List<Entrada> l,String tipo,Identifier id,String key,String classe,Map<String,String> pt,Map<String,String> en){
        String nome=pt.getOrDefault(key,en.getOrDefault(key,id.getPath().replace('_',' ')));
        l.add(new Entrada(tipo,id,nome,normal(nome+" "+en.getOrDefault(key,"")+" "+id+" "+classe)));
    }
    static List<Entrada> buscar(String tipo,Set<String> termos,int limite){
        return buscar(obter(),tipo,termos,limite);
    }
    static List<Entrada> buscar(List<Entrada> fonte,String tipo,Set<String> termos,int limite){
        Set<String> normalizados=new LinkedHashSet<>();for(String t:termos){String n=normal(t);if(!n.isBlank())normalizados.add(n);}
        record Ponto(Entrada e,int valor){}
        return fonte.stream().filter(e->e.tipo().equals(tipo))
            .map(e->new Ponto(e,normalizados.stream().mapToInt(n->{
                if(normal(e.nome()).equals(n)||normal(e.id().getPath()).equals(n))return 8;
                return (" "+e.texto()+" ").contains(" "+n+" ")?3:e.texto().contains(n)&&n.length()>3?1:0;
            }).sum())).filter(p->p.valor()>0)
            .sorted(Comparator.comparingInt(Ponto::valor).reversed().thenComparing(p->p.e().id().toString()))
            .limit(limite).map(Ponto::e).toList();
    }
    static String consultar(List<Entrada> fonte,ConsultaIA.Consulta c){
        var encontrados=buscar(fonte,c.categoria(),new LinkedHashSet<>(c.termos()),24);
        String titulo=switch(c.categoria()){case "mob"->"Entidades";case "bloco"->"Blocos";default->"Itens";};
        StringBuilder b=new StringBuilder(titulo+": ");
        for(var e:encontrados)b.append(e.id()).append(" [").append(e.nome().replace('[','(').replace(']',')')).append("], ");
        return b.toString();
    }
    static Pedido exato(String texto){
        String t=normal(texto).replaceFirst("^(quero|me de|me da|invoque|crie) ","").replaceFirst("^(um|uma) ","");
        if(t.isBlank()||t.length()>80)return null;
        var encontrados=obter().stream().filter(e->!e.tipo().equals("bloco")&&(normal(e.nome()).equals(t)||normal(e.id().getPath()).equals(t))).toList();
        if(encontrados.size()!=1)return null;
        var e=encontrados.get(0);
        if(e.tipo().equals("mob")&&!Registries.ENTITY_TYPE.get(e.id()).isSummonable())return null;
        return Pedido.dinamico(e.nome(),e.tipo().equals("mob")?Pedido.Tipo.MOB:Pedido.Tipo.ITEM,e.id().toString(),1,1);
    }
}
