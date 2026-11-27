package br.com.vozdacriacao;
import java.util.*;
import java.util.regex.*;
/** Planta modular de um andar, com corredores e ambientes independentes. */
final class PlantaFlexivel {
    record Pos(int x,int y,int z){}
    record Ambiente(String tipo,int x,int z){}
    record Spec(int quartos,int banheiros,boolean cozinha,boolean sala,boolean garagem,boolean piscina,boolean jardim,String estilo){}
    static int contar(String t,String singular,String plural,int padrao){
        Matcher m=Pattern.compile("\\b(\\d+|um|uma|dois|duas|tres|quatro|cinco|seis|sete|oito|zero)\\s+(?:"+singular+"|"+plural+")\\b").matcher(t);
        if(m.find()){
            String s=m.group(1);int n=switch(s){case "zero"->0;case "um","uma"->1;case "dois","duas"->2;case "tres"->3;case "quatro"->4;case "cinco"->5;case "seis"->6;case "sete"->7;case "oito"->8;default->Integer.parseInt(s);};
            if(n>8||n<0)throw new IllegalArgumentException("Até oito ambientes de cada tipo");
            return n;
        }
        return Pedido.palavra(t,singular+"|"+plural)?1:padrao;
    }
    static Pedido interpretar(String frase){
        String t=Pedido.normal(frase);
        if(!t.matches("^(?:(?:quero|desejo|crie|criar|construa|construir|faca|gere)\\s+)?(?:uma?\\s+)?(?:casa|mansao)\\b.*"))return null;
        if(!Pedido.palavra(t,"banheiro|banheiros|piscina|garagem|cozinha|sala|jardim"))return null;
        if(Pedido.palavra(t,"andar|andares|voadora|submarina|porao|sotao|cinema|academia|biblioteca|varanda|carro|carros"))throw new IllegalArgumentException("A planta flexível ainda não suporta todos os ambientes solicitados");
        String estilo=Pedido.palavra(t,"medieval")?"medieval":Pedido.palavra(t,"rustica|rustico")?"rustico":Pedido.palavra(t,"luxuosa|luxuoso")?"luxuoso":"moderno";
        return planejar(new Spec(contar(t,"quarto","quartos",Pedido.palavra(t,"mansao")?2:1),contar(t,"banheiro","banheiros",0),Pedido.palavra(t,"cozinha"),Pedido.palavra(t,"sala"),Pedido.palavra(t,"garagem"),Pedido.palavra(t,"piscina"),Pedido.palavra(t,"jardim"),estilo));
    }
    static Pedido planejar(Spec s){
        if(s.quartos()<0||s.quartos()>8||s.banheiros()<0||s.banheiros()>8||!Residencia.ESTILOS.contains(s.estilo()))throw new IllegalArgumentException("Planta inválida");
        List<String> tipos=new ArrayList<>();
        for(int i=0;i<s.quartos();i++)tipos.add("quarto");for(int i=0;i<s.banheiros();i++)tipos.add("banheiro");
        if(s.cozinha())tipos.add("cozinha");if(s.sala())tipos.add("sala");if(s.garagem())tipos.add("garagem");
        if(tipos.isEmpty())tipos.add("sala");if(tipos.size()>12)throw new IllegalArgumentException("Até doze ambientes internos");
        int col=(tipos.size()+1)/2, w=col*8+1;
        Map<Pos,String> m=new LinkedHashMap<>();
        String parede=switch(s.estilo()){case "rustico"->"oak_planks";case "medieval"->"stone_bricks";case "luxuoso"->"quartz_block";default->"white_concrete";};
        for(int x=-1;x<=w;x++)for(int z=-8;z<=8;z++){put(m,x,0,z,"smooth_stone");put(m,x,5,z,"smooth_quartz");}
        for(int x=0;x<w;x++)for(int z=-7;z<=7;z++)if(x==0||x==w-1||Math.abs(z)==7)for(int y=1;y<=4;y++){
            if(x==0&&z==0&&y<=2)continue;
            boolean janela=(y==2||y==3)&&Math.abs(z)==7&&x%8>=2&&x%8<=5;
            put(m,x,y,z,janela?"glass":parede);
        }
        for(int i=0;i<tipos.size();i++){
            String tipo=tipos.get(i);int x=1+(i/2)*8, lado=i%2==0?-1:1, z=lado*3;
            for(int dx=0;dx<8;dx++)for(int y=1;y<=4;y++){
                if(dx==3&&y<=2)continue;put(m,x+dx,y,lado*2,parede);
            }
            if(i/2>0)for(int dz=2;dz<=6;dz++)for(int y=1;y<=4;y++)put(m,x-1,y,lado*dz,parede);
            put(m,x+3,1,lado*2,"porta_baixo");put(m,x+3,2,lado*2,"porta_cima");
            put(m,x+1,1,lado*5,"lantern");
            switch(tipo){
                case "quarto"->{put(m,x+2,1,z,"cama_pe");put(m,x+2,1,z-1,"cama_cabeca");if(lado==1){m.remove(new Pos(x+2,1,z-1));put(m,x+2,1,z+2,"cama_pe");put(m,x+2,1,z+1,"cama_cabeca");m.remove(new Pos(x+2,1,z));}put(m,x+5,1,lado*5,"bau");}
                case "banheiro"->{put(m,x+2,1,lado*5,"cauldron");put(m,x+5,1,lado*5,"quartz_stairs");put(m,x+5,2,lado*6,"quartz_block");put(m,x+4,1,lado*6,"white_carpet");}
                case "cozinha"->{put(m,x+2,1,lado*5,"fornalha");put(m,x+3,1,lado*5,"crafting_table");put(m,x+4,1,lado*5,"bau");}
                case "sala"->{for(int dx=2;dx<=4;dx++)put(m,x+dx,1,lado*5,"quartz_stairs");put(m,x+3,1,lado*4,"white_carpet");}
                case "garagem"->{for(int dx=2;dx<=5;dx++)for(int y=1;y<=3;y++)m.remove(new Pos(x+dx,y,lado*7));}
            }
        }
        for(int x=2;x<w-1;x+=4)put(m,x,4,0,"sea_lantern");
        if(s.piscina()){
            for(int x=2;x<=10;x++)for(int z=10;z<=16;z++){put(m,x,0,z,"quartz_block");if(x==2||x==10||z==10||z==16)put(m,x,1,z,"quartz_block");else put(m,x,1,z,"water");}
        }
        if(s.jardim())for(int x=2;x<=Math.min(w-1,14);x+=2){put(m,x,0,-10,"grass_block");put(m,x,1,-10,"poppy");}
        int meio=w/2;
        var cells=m.entrySet().stream().map(e->new Pedido.Celula(e.getKey().x-meio,e.getKey().y,e.getKey().z,e.getValue())).toList();
        if(cells.size()>10000)throw new IllegalArgumentException("Planta grande demais");
        return new Pedido("Residência: "+s.quartos()+"Q "+s.banheiros()+"B",cells.size(),Pedido.Tipo.RESIDENCIA,cells);
    }
    static void put(Map<Pos,String> m,int x,int y,int z,String b){m.put(new Pos(x,y,z),b);}
}
