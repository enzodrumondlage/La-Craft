package br.com.vozdacriacao;
import com.google.gson.*;
import java.util.*;
/** Compila uma planta escolhida pela IA. Valida topologia antes de criar blocos. */
final class ProjetoComodos {
    private record P(int x,int y,int z){}
    private record R(String id,String tipo,int andar,int x1,int z1,int x2,int z2){
        int y(){return andar*5;}
        boolean exterior(){return tipo.equals("jardim")||tipo.equals("piscina");}
    }
    private record Ligacao(String a,String b){}
    private record Vao(int x,int z,String face){}
    private static final Set<String> TIPOS=Set.of("hall","quarto","banheiro","cozinha","sala","garagem","escritorio","biblioteca","salao","deposito","jardim","piscina","outro");
    private static final Set<String> PAREDES=Set.of("white_concrete","quartz_block","stone_bricks","oak_planks","spruce_planks","black_concrete","bricks","sandstone");
    private static int n(JsonObject p,String k,int min,int max){
        var v=p.get(k).getAsJsonPrimitive();if(!v.isNumber())throw new IllegalArgumentException("Planta: número inválido");
        int i=v.getAsBigDecimal().intValueExact();if(i<min||i>max)throw new IllegalArgumentException("Planta: "+k+" fora do limite");return i;
    }
    private static String str(JsonObject p,String k){return p.get(k).getAsString();}
    private static boolean bool(JsonObject p,String k){
        var v=p.get(k).getAsJsonPrimitive();if(!v.isBoolean())throw new IllegalArgumentException("Planta: booleano inválido");return v.getAsBoolean();
    }
    private static void put(Map<P,String> m,int x,int y,int z,String b){m.put(new P(x,y,z),b);}
    private static void porta(Map<P,String> m,R r,Vao v,boolean direita){
        String face=v.face();String h=direita?"d":"e";
        put(m,v.x(),r.y()+1,v.z(),"porta_"+face+"_"+h+"_baixo");
        put(m,v.x(),r.y()+2,v.z(),"porta_"+face+"_"+h+"_cima");
    }
    private static Vao vao(R a,R b){
        if(a.andar()!=b.andar())return null;
        int x0=Math.max(a.x1(),b.x1()),x1=Math.min(a.x2(),b.x2()),z0=Math.max(a.z1(),b.z1()),z1=Math.min(a.z2(),b.z2());
        if((a.x2()==b.x1()||b.x2()==a.x1())&&z1-z0>=3)return new Vao(x0,(z0+z1)/2,"leste");
        if((a.z2()==b.z1()||b.z2()==a.z1())&&x1-x0>=3)return new Vao((x0+x1)/2,z0,"norte");
        return null;
    }
    static Pedido planejar(String nome,JsonObject p){
        int andares=n(p,"andares",1,2);
        boolean dupla=bool(p,"porta_dupla"),preta=bool(p,"janelas_pretas");
        String parede=str(p,"parede");if(!PAREDES.contains(parede))throw new IllegalArgumentException("Material de parede inválido");
        var arr=p.getAsJsonArray("comodos");if(arr==null||arr.isEmpty()||arr.size()>20)throw new IllegalArgumentException("Planta: 1 a 20 ambientes");
        Map<String,R> rs=new LinkedHashMap<>();
        for(var el:arr){
            var o=el.getAsJsonObject();String id=str(o,"id"),tipo=str(o,"funcao");
            if(!id.matches("[a-zA-Z0-9_]{1,20}")||!TIPOS.contains(tipo)||rs.containsKey(id))throw new IllegalArgumentException("Ambiente inválido");
            R r=new R(id,tipo,n(o,"andar",0,andares-1),n(o,"x1",-24,24),n(o,"z1",-24,24),n(o,"x2",-24,24),n(o,"z2",-24,24));
            if(r.x2()-r.x1()<5||r.z2()-r.z1()<5||r.exterior()&&r.andar()!=0)throw new IllegalArgumentException("Ambiente pequeno ou exterior em andar superior: "+id);
            for(R q:rs.values())if(r.andar()==q.andar()&&Math.min(r.x2(),q.x2())-Math.max(r.x1(),q.x1())>0&&Math.min(r.z2(),q.z2())-Math.max(r.z1(),q.z1())>0)throw new IllegalArgumentException("Ambientes sobrepostos: "+id+" e "+q.id());
            rs.put(id,r);
        }
        R entrada=rs.get(str(p,"entrada"));if(entrada==null||entrada.andar()!=0||!entrada.tipo().equals("hall"))throw new IllegalArgumentException("Entrada deve ser hall do térreo");
        int ex=(entrada.x1()+entrada.x2())/2;
        for(R r:rs.values())if(r!=entrada&&r.andar()==0&&r.z2()==entrada.z1()&&r.x1()<=ex+1&&r.x2()>=ex)throw new IllegalArgumentException("Entrada norte não tem saída externa");
        // Cada ambiente superior precisa estar apoiado pela planta térrea.
        for(R r:rs.values())if(r.andar()==1)for(int x=r.x1();x<=r.x2();x++)for(int z=r.z1();z<=r.z2();z++){
            final int fx=x,fz=z;
            if(rs.values().stream().noneMatch(q->q.andar()==0&&!q.exterior()&&fx>=q.x1()&&fx<=q.x2()&&fz>=q.z1()&&fz<=q.z2()))throw new IllegalArgumentException("Andar superior sem apoio: "+r.id());
        }
        Map<String,Set<String>> adj=new LinkedHashMap<>();rs.keySet().forEach(k->adj.put(k,new LinkedHashSet<>()));
        List<Ligacao> links=new ArrayList<>();var la=p.getAsJsonArray("ligacoes");
        if(la==null||la.size()>40)throw new IllegalArgumentException("Ligações inválidas");
        for(var el:la){
            var l=el.getAsJsonArray();if(l.size()!=2)throw new IllegalArgumentException("Ligação exige dois IDs");
            String a=l.get(0).getAsString(),b=l.get(1).getAsString();R ra=rs.get(a),rb=rs.get(b);
            if(ra==null||rb==null||a.equals(b)||!adj.get(a).add(b))throw new IllegalArgumentException("Ligação inválida ou duplicada");
            if(vao(ra,rb)==null)throw new IllegalArgumentException("Ambientes não compartilham parede: "+a+" e "+b);
            if(ra.tipo().equals("piscina")||rb.tipo().equals("piscina"))throw new IllegalArgumentException("Piscina deve ficar ao lado de área externa acessível, sem porta na água");
            adj.get(b).add(a);links.add(new Ligacao(a,b));
        }
        R escada=null;
        if(andares==2){
            escada=rs.get(str(p,"hall_superior"));
            if(escada==null||escada.andar()!=1||!escada.tipo().equals("hall")||entrada.x1()!=escada.x1()||entrada.x2()!=escada.x2()||entrada.z1()!=escada.z1()||entrada.z2()!=escada.z2()||entrada.z2()-entrada.z1()<9||entrada.x2()-entrada.x1()<7)throw new IllegalArgumentException("Dois andares exigem halls sobrepostos de pelo menos 8x10");
            adj.get(entrada.id()).add(escada.id());adj.get(escada.id()).add(entrada.id());
        }
        Set<String> vistos=new HashSet<>();Deque<String> fila=new ArrayDeque<>();fila.add(entrada.id());
        while(!fila.isEmpty()){String id=fila.removeFirst();if(vistos.add(id))fila.addAll(adj.get(id));}
        for(R r:rs.values()){
            if(r.tipo().equals("piscina")){
                if(rs.values().stream().noneMatch(q->q.tipo().equals("jardim")&&vistos.contains(q.id())&&vao(q,r)!=null))throw new IllegalArgumentException("Piscina sem acesso por jardim");
            }else if(!vistos.contains(r.id()))throw new IllegalArgumentException("Ambiente sem acesso: "+r.id());
        }
        Map<P,String> m=new LinkedHashMap<>();
        for(R r:rs.values()){
            int y=r.y();
            for(int x=r.x1();x<=r.x2();x++)for(int z=r.z1();z<=r.z2();z++){
                boolean borda=x==r.x1()||x==r.x2()||z==r.z1()||z==r.z2();
                put(m,x,y,z,r.tipo().equals("jardim")?"grass_block":"smooth_quartz");
                if(r.tipo().equals("piscina")){put(m,x,y+1,z,borda?"quartz_block":"water");continue;}
                if(r.tipo().equals("jardim"))continue;
                put(m,x,y+5,z,"smooth_quartz");
                if(borda)for(int h=1;h<=4;h++){
                    boolean canto=(x==r.x1()||x==r.x2())&&(z==r.z1()||z==r.z2());
                    boolean externa=true;
                    for(R q:rs.values())if(q!=r&&!q.exterior()&&q.andar()==r.andar()&&x>=q.x1()&&x<=q.x2()&&z>=q.z1()&&z<=q.z2()){externa=false;break;}
                    put(m,x,y+h,z,externa&&!canto&&h>=2&&h<=3&&(x+z)%4!=0?(preta?"black_stained_glass":"glass"):parede);
                }
            }
            int x=r.x1()+1,z=r.z1()+1;
            if(r.exterior()){
                if(r.tipo().equals("jardim")){put(m,x,y+1,z,"poppy");put(m,r.x2()-1,y+1,r.z2()-1,"lantern");}
                continue;
            }
            put(m,r.x2()-1,y+4,r.z2()-1,"sea_lantern");
            switch(r.tipo()){
                case "quarto"->{put(m,x,y+1,z+1,"cama_pe");put(m,x,y+1,z,"cama_cabeca");put(m,x+2,y+1,z,"bau");}
                case "banheiro"->{put(m,x,y+1,z,"cauldron");put(m,x+2,y+1,z,"quartz_stairs");}
                case "cozinha"->{put(m,x,y+1,z,"fornalha");put(m,x+1,y+1,z,"crafting_table");put(m,x+2,y+1,z,"bau");}
                case "sala","salao"->{for(int k=0;k<3;k++)put(m,x+k,y+1,z,"quartz_stairs");put(m,x+1,y+1,z+1,"white_carpet");}
                case "biblioteca"->{for(int k=0;k<3;k++)put(m,x+k,y+1,z,"bookshelf");}
                case "escritorio"->{put(m,x,y+1,z,"oak_planks");put(m,x+1,y+1,z+1,"quartz_stairs");}
                case "deposito"->put(m,x,y+1,z,"bau");
                default->{}
            }
        }
        for(Ligacao l:links){
            R a=rs.get(l.a()),b=rs.get(l.b());Vao v=vao(a,b);
            if(a.exterior()&&b.exterior())continue;
            if(a.tipo().equals("garagem")||b.tipo().equals("garagem")){
                m.remove(new P(v.x(),a.y()+1,v.z()));m.remove(new P(v.x(),a.y()+2,v.z()));
            }else porta(m,a,v,false);
        }
        porta(m,entrada,new Vao(ex,entrada.z1(),"norte"),false);
        if(dupla)porta(m,entrada,new Vao(ex+1,entrada.z1(),"norte"),true);
        if(escada!=null){
            int sx=entrada.x1()+2,sz=entrada.z1()+2;
            for(int k=0;k<5;k++)for(int w=0;w<2;w++){
                if(k<4)m.remove(new P(sx+w,5,sz+k));
                put(m,sx+w,k+1,sz+k,"escada_sul");
                for(int h=1;h<=2;h++)m.remove(new P(sx+w,k+1+h,sz+k));
            }
        }
        if(m.isEmpty()||m.size()>16000)throw new IllegalArgumentException("Planta excede 16000 blocos");
        JsonObject meta=new JsonObject();meta.addProperty("andares",andares);
        for(String tipo:TIPOS)meta.addProperty(tipo,rs.values().stream().filter(r->r.tipo().equals(tipo)).count());
        long suites=rs.values().stream().filter(r->r.tipo().equals("quarto")&&adj.get(r.id()).stream().anyMatch(id->rs.get(id).tipo().equals("banheiro")&&adj.get(id).size()==1)).count();
        meta.addProperty("suites",suites);meta.addProperty("dupla",dupla);meta.addProperty("preta",preta);
        var celulas=m.entrySet().stream().map(e->new Pedido.Celula(e.getKey().x(),e.getKey().y(),e.getKey().z(),e.getValue())).toList();
        return new Pedido(nome,celulas.size(),Pedido.Tipo.RESIDENCIA,celulas,"planta2:"+meta,1,1);
    }
}
