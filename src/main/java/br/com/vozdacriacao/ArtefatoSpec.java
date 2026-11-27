package br.com.vozdacriacao;
import com.google.gson.*;
import java.util.*;
/** Especificação declarativa persistente, limitada e sem execução de código. */
public record ArtefatoSpec(int cor,List<String> efeitos,int duracao,int recarga){
    static final Set<String> VALIDOS=Set.of("cura","respirar","velocidade","salto","invisivel","resistencia","levitacao","brilho","nuvem","musica");
    public ArtefatoSpec{
        efeitos=List.copyOf(new LinkedHashSet<>(efeitos));
        if(cor<0||cor>0xffffff||efeitos.isEmpty()||efeitos.size()>5||!VALIDOS.containsAll(efeitos)||duracao<5||duracao>120||recarga<2||recarga>30)throw new IllegalArgumentException("Artefato inválido");
    }
    static Pedido validar(String nome,JsonObject p){
        String cor=p.get("cor").getAsString();
        if(!cor.matches("#[a-fA-F0-9]{6}"))throw new IllegalArgumentException("Cor inválida");
        var a=p.getAsJsonArray("efeitos");if(a.size()>5)throw new IllegalArgumentException("Efeitos demais");
        List<String> e=new ArrayList<>();for(var v:a)e.add(v.getAsString());
        return new ArtefatoSpec(Integer.parseInt(cor.substring(1),16),e,inteiro(p,"duracao"),inteiro(p,"recarga")).pedido(nome);
    }
    private static int inteiro(JsonObject p,String k){var v=p.get(k).getAsJsonPrimitive();if(!v.isNumber())throw new IllegalArgumentException("Número inválido");return v.getAsBigDecimal().intValueExact();}
    Pedido pedido(String nome){
        String dados=String.format(Locale.ROOT,"%06x;%d;%d;%s",cor,duracao,recarga,String.join(",",efeitos));
        return new Pedido(nome,40+efeitos.size()*35+duracao,Pedido.Tipo.ARTEFATO,List.of(),dados,1,1);
    }
    static ArtefatoSpec ler(String s){
        if(s.length()>160)throw new IllegalArgumentException("Dados longos");
        String[] a=s.split(";",-1);if(a.length!=4)throw new IllegalArgumentException("Dados inválidos");
        return new ArtefatoSpec(Integer.parseInt(a[0],16),Arrays.asList(a[3].split(",")),Integer.parseInt(a[1]),Integer.parseInt(a[2]));
    }
    static Pedido offline(String texto){
        String t=Pedido.normal(texto);
        if(!Pedido.palavra(t,"cristal|amuleto|artefato|cetro|varinha|talismã|talisma"))return null;
        List<String> e=new ArrayList<>();
        String[][] m={{"curar|cura","cura"},{"respirar|aquatico|agua","respirar"},{"velocidade|rapidez","velocidade"},{"pulo|salto","salto"},{"invisibilidade|invisivel","invisivel"},{"resistencia|escudo","resistencia"},{"levitar|levitacao","levitacao"},{"luz|brilho|laser","brilho"},{"nuvem|peido|gases","nuvem"},{"musica|som","musica"}};
        for(String[] x:m)if(Pedido.palavra(t,x[0])&&e.size()<5)e.add(x[1]);
        if(e.isEmpty())return null;
        int c=Pedido.palavra(t,"verde")?0x66dd33:Pedido.palavra(t,"vermelho")?0xff4444:Pedido.palavra(t,"roxo")?0xbb55ff:0x44bbff;
        return new ArtefatoSpec(c,e,30,5).pedido("Artefato da criação");
    }
}
