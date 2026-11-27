package br.com.vozdacriacao;
import java.util.*;
/** Planta procedural independente do Minecraft; estados especiais são resolvidos pelo executor. */
final class Residencia {
    static final Set<String> ESTILOS=Set.of("rustico","moderno","medieval","luxuoso");
    private record Pos(int x,int y,int z){}
    static Pedido interpretar(String frase,int qualidade){
        String t=Pedido.normal(frase).replaceFirst("^lacraft:\\s*","");
        if(!t.matches("^(?:(?:quero|desejo|crie|criar|construa|construir|faca|gere|me de)\\s+)?(?:(?:uma|um|minha)\\s+)?(?:casa|mansao)\\b.*"))return null;
        Pedido flex=PlantaFlexivel.interpretar(frase);if(flex!=null)return flex;
        boolean grande=Pedido.palavra(t,"mansao|grande|dois quartos|2 quartos");
        int quartos=Pedido.palavra(t,"quatro quartos|4 quartos")?4:Pedido.palavra(t,"tres quartos|3 quartos")?3:Pedido.palavra(t,"um quarto|1 quarto")?1:grande?2:1;
        // Não descartar requisitos extras não implementados.
        if(Pedido.palavra(t,"piscina|garagem|andar|andares|voadora|submarina"))return null;
        String estilo=Pedido.palavra(t,"moderna|moderno")?"moderno":Pedido.palavra(t,"medieval")?"medieval":Pedido.palavra(t,"luxuosa|luxuoso|luxo")?"luxuoso":"rustico";
        return planejar(grande?"Mansão mobiliada":"Casa mobiliada",quartos,estilo,qualidade);
    }
    static Pedido planejar(String nome,boolean grande,String estilo,int qualidade){
        return planejar(nome,grande?2:1,estilo,qualidade);
    }
    static Pedido planejar(String nome,int quartos,String estilo,int qualidade){
        boolean grande=quartos>1;
        if(quartos<1||quartos>4||!ESTILOS.contains(estilo)||qualidade<1||qualidade>4)throw new IllegalArgumentException("Estilo ou qualidade inválidos");
        int w=quartos>2?10:grande?7:4,d=quartos>2?9:grande?6:5;
        Map<Pos,String> m=new LinkedHashMap<>();
        String parede=switch(estilo){case "moderno"->"white_concrete";case "medieval"->"stone_bricks";case "luxuoso"->"quartz_block";default->"oak_planks";};
        String pilar=estilo.equals("rustico")?"spruce_log":"stone_bricks";
        for(int x=-w;x<=w;x++)for(int z=-d;z<=d;z++)put(m,x,0,z,"spruce_planks");
        for(int x=-w;x<=w;x++)for(int z=-d;z<=d;z++)if(Math.abs(x)==w||Math.abs(z)==d){
            for(int y=1;y<=4;y++){
                if(x==0&&z==-d&&y<=2)continue;
                boolean canto=Math.abs(x)==w&&Math.abs(z)==d;
                boolean janela=(y==2||y==3)&&!canto&&(Math.abs(x)==w?Math.abs(z)%3==1:Math.abs(x)%3==2);
                put(m,x,y,z,canto?pilar:janela?"glass":y==1&&qualidade>=2?"stone_bricks":parede);
            }
        }
        put(m,0,1,-d,"porta_baixo");put(m,0,2,-d,"porta_cima");put(m,0,0,-d-1,"stone_bricks");
        if(estilo.equals("moderno")){
            for(int x=-w-1;x<=w+1;x++)for(int z=-d-1;z<=d+1;z++)put(m,x,5,z,"smooth_quartz");
        }else{
            for(int x=-w-1;x<=w+1;x++){
                int y=5+w+1-Math.abs(x);
                for(int z=-d-1;z<=d+1;z++)put(m,x,y,z,x<0?"telhado_leste":x>0?"telhado_oeste":"spruce_planks");
                if(Math.abs(x)<=w)for(int k=5;k<y;k++){put(m,x,k,-d,parede);put(m,x,k,d,parede);}
            }
        }
        put(m,-w+1,1,-d+1,"crafting_table");put(m,-w+2,1,-d+1,"fornalha");put(m,w-1,1,-d+1,"bau");
        put(m,-w+1,1,-d+2,"lantern");put(m,w-1,1,-d+2,"lantern");
        if(quartos>2){
            for(int x:new int[]{-2,2})for(int z=-5;z<d;z++)for(int y=1;y<=4;y++){if((z==-3||z==4)&&y<=2)continue;put(m,x,y,z,parede);}
            for(int x=-w+1;x<w;x++)if(Math.abs(x)>=2)for(int y=1;y<=4;y++){put(m,x,y,-6,parede);put(m,x,y,0,parede);}
            int n=0;for(int z:new int[]{-3,5})for(int x:new int[]{-6,6})if(n++<quartos){cama(m,x,z);put(m,x+1,1,z,"bau");put(m,x-1,1,z,"lantern");}
        }else if(grande){
            for(int x=-w+1;x<w;x++)for(int y=1;y<=4;y++){
                if((x==-3||x==3)&&y<=2)continue;
                put(m,x,y,1,parede);
            }
            for(int z=2;z<d;z++)for(int y=1;y<=4;y++)put(m,0,y,z,parede);
            for(int x:new int[]{-4,4}){cama(m,x,d-2);put(m,x+1,1,d-2,"bau");put(m,x-1,1,d-2,"lantern");}
        }else{cama(m,w-1,d-2);put(m,w-2,1,d-2,"bau");put(m,-w+1,1,d-2,"lantern");}
        if(qualidade>=3){put(m,-2,1,-1,"white_carpet");put(m,-1,1,-1,"white_carpet");put(m,1,1,-1,"white_carpet");put(m,2,1,-1,"white_carpet");}
        if(qualidade>=4){put(m,-2,0,-d-1,"stone_bricks");put(m,-2,1,-d-1,"lantern");put(m,2,0,-d-1,"stone_bricks");put(m,2,1,-d-1,"lantern");}
        List<Pedido.Celula> cells=m.entrySet().stream().map(e->new Pedido.Celula(e.getKey().x,e.getKey().y,e.getKey().z,e.getValue())).toList();
        return new Pedido(nome,cells.size(),Pedido.Tipo.RESIDENCIA,cells);
    }
    private static void cama(Map<Pos,String> m,int x,int z){put(m,x,1,z,"cama_pe");put(m,x,1,z-1,"cama_cabeca");}
    private static void put(Map<Pos,String> m,int x,int y,int z,String b){m.put(new Pos(x,y,z),b);}
}
