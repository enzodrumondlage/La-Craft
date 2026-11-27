package br.com.vozdacriacao;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;
public record Pedido(String nome,int custo,Tipo tipo,List<Celula> celulas,String alvo,int quantidade,int escala,List<Pedido> acoes){
    public record Celula(int x,int y,int z,String bloco){}
    public Pedido(String nome,int custo,Tipo tipo){this(nome,custo,tipo,List.of(),"",1,1);}
    public Pedido(String nome,int custo,Tipo tipo,List<Celula> celulas){this(nome,custo,tipo,celulas,"",1,1);}
    public Pedido(String nome,int custo,Tipo tipo,List<Celula> celulas,String alvo,int quantidade,int escala){this(nome,custo,tipo,celulas,alvo,quantidade,escala,List.of());}
    public Pedido {celulas=List.copyOf(celulas);acoes=List.copyOf(acoes);}
    static Pedido acao(String nome,Tipo tipo,String alvo,int qtd,int duracao){return new Pedido(nome,0,tipo,List.of(),alvo,qtd,duracao);}
    static Pedido sequencia(List<Pedido> acoes){if(acoes.isEmpty()||acoes.size()>8||acoes.stream().anyMatch(a->a.tipo()==Tipo.SEQUENCIA))throw new IllegalArgumentException("Sequência inválida");return new Pedido("Plano combinado",0,Tipo.SEQUENCIA,List.of(),"",1,1,acoes);}
    public Pedido gratuito(){return new Pedido(nome,0,tipo,celulas,alvo,quantidade,escala,acoes);}
    public enum Tipo { FONTE,CASA,TORRE,FOGUETE,CASTELO,BURROS,CAPIVARA,ARCO,ARMADURA,VOXEL,VOO,VELOCIDADE,SALTO,INVISIVEL,LASER,ESPADA,ITEM,MOB,ARTEFATO,RESIDENCIA,TEMPO,CLIMA,CHUVA_ITEM,CHUVA_MOB,SEQUENCIA,CARRO,FENOMENO }
    static String normal(String s){return Normalizer.normalize(s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","").trim();}
    static boolean palavra(String s,String p){return Pattern.compile("(?<![a-z0-9_])(?:"+p+")(?![a-z0-9_])").matcher(s).find();}
    public static boolean pedidoChat(String s){
        String t=normal(s);
        if(t.startsWith("lacraft: ")||t.matches("^(quero|desejo|crie|criar|construa|construir|invoque|invocar|me de|me da|me transforme|faca|gere|casa|mansao|castelo|torre|estatua|cristal|amuleto|cetro|varinha)\\b.*"))return true;
        try{return (AcoesTexto.interpretar(s)!=null||interpretar(s)!=null)&&t.split("\\s+").length<=4;}
        catch(IllegalArgumentException ex){return false;}
    }
    public static Pedido dinamico(String nome,Tipo tipo,String alvo,int qtd,int escala){
        if(tipo!=Tipo.ITEM&&tipo!=Tipo.MOB)throw new IllegalArgumentException("Tipo inválido");
        if(!alvo.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||alvo.length()>120||qtd<1||qtd>(tipo==Tipo.MOB?8:64)||escala<1||escala>4)throw new IllegalArgumentException("Dados inválidos");
        return new Pedido(nome,(tipo==Tipo.MOB?30*escala:8)*qtd,tipo,List.of(),alvo,qtd,escala);
    }
    public static Pedido interpretar(String texto){
        String t=normal(texto);
        Matcher id=Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)").matcher(t);
        if(id.find()){
            String alvo=id.group(1);
            if(alvo.startsWith("lacraft:"))return null;
            boolean mob=palavra(t,"mob|entidade|invoque|invocar");
            return dinamico(alvo,mob?Tipo.MOB:Tipo.ITEM,alvo,1,palavra(t,"gigante")?3:1);
        }
        if(palavra(t,"voar|voo|voando"))return new Pedido("Poder de voo",120,Tipo.VOO);
        if(palavra(t,"velocidade|super velocidade|rapidez"))return new Pedido("Supervelocidade",60,Tipo.VELOCIDADE);
        if(palavra(t,"salto|super pulo|pular"))return new Pedido("Super salto",60,Tipo.SALTO);
        if(palavra(t,"invisivel|invisibilidade"))return new Pedido("Invisibilidade",80,Tipo.INVISIVEL);
        if(palavra(t,"laser|raio|varinha"))return new Pedido("Cetro de luz",150,Tipo.LASER);
        if(palavra(t,"espada|espadas"))return new Pedido("Espada mágica",180,Tipo.ESPADA);
        String[][] mobs={{"piglin|piglim","piglin"},{"zumbi|zombie","zombie"},{"warden|guardiao","warden"},{"vaca|vacas","cow"},{"galinha|galinhas","chicken"},{"porco|porcos","pig"},{"ovelha|ovelhas","sheep"},{"lobo|lobos","wolf"},{"gato|gatos","cat"},{"cavalo|cavalos","horse"},{"aldeao|aldeoes","villager"},{"creeper","creeper"},{"slime","slime"},{"enderman","enderman"},{"coelho|coelhos","rabbit"},{"axolote|axolotl","axolotl"}};
        for(String[] m:mobs)if(palavra(t,m[0]))return dinamico("Invocar "+m[1],Tipo.MOB,"minecraft:"+m[1],palavra(t,"chuva|varios|varias")?8:1,palavra(t,"gigante|gigantes")?3:1);
        if(palavra(t,"barco|barcos"))return dinamico("Barco",Tipo.ITEM,"minecraft:oak_boat",1,1);
        if(palavra(t,"diamante|diamantes"))return dinamico("Diamantes",Tipo.ITEM,"minecraft:diamond",palavra(t,"diamantes")?8:1,1);
        if(palavra(t,"castelo"))return new Pedido("Castelo",500,Tipo.CASTELO);
        if(palavra(t,"foguete"))return new Pedido("Foguete decorativo",300,Tipo.FOGUETE);
        if(palavra(t,"capivara"))return new Pedido("Estátua de capivara",100,Tipo.CAPIVARA);
        if(palavra(t,"burro|burros"))return new Pedido("Chuva de burros",50,Tipo.BURROS);
        if(palavra(t,"armadura"))return new Pedido("Armadura esmeralda",200,Tipo.ARMADURA);
        if(palavra(t,"arco|arcos"))return new Pedido("Arco mágico",200,Tipo.ARCO);
        if(palavra(t,"torre"))return new Pedido("Torre",150,Tipo.TORRE);
        if(palavra(t,"casa|casas"))return new Pedido("Casa",100,Tipo.CASA);
        if(palavra(t,"fonte|particula|particulas"))return new Pedido("Fonte de partículas",12,Tipo.FONTE);
        return null;
    }
}
