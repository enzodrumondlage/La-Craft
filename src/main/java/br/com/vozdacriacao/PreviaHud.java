package br.com.vozdacriacao;
import java.util.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
/** Projeção isométrica dos voxels reais recebidos do servidor; não inventa geometria. */
final class PreviaHud {
    static List<Pedido.Celula> celulas=List.of();
    static String nome="";
    static int ate;
    static void receber(String n,List<Pedido.Celula> c,int t){nome=n;celulas=List.copyOf(c);ate=t+120;}
    static void limpar(){celulas=List.of();nome="";ate=0;}
    static int cor(String b){if(b.contains("water"))return 0xff43bde6;if(b.contains("glass"))return 0xff568fba;if(b.contains("grass")||b.contains("poppy"))return 0xff72a65d;if(b.contains("plank")||b.contains("log"))return 0xffa68054;if(b.contains("lantern"))return 0xffffd17c;return 0xffb4c4d0;}
    static void render(DrawContext ctx,int t,int feitos,int total){
        if(!ConfigLaCraft.obter().previa3d||celulas.isEmpty()||total==0&&t>ate)return;
        int x=8,y=94,w=214,h=104;
        ctx.fill(x,y,x+w,y+h,0xe0172536);
        var tr=MinecraftClient.getInstance().textRenderer;
        ctx.drawTextWithShadow(tr,nome,x+6,y+5,0xff9bd8ff);
        int max=celulas.stream().mapToInt(c->Math.max(Math.abs(c.x()),Math.max(Math.abs(c.z()),c.y()))).max().orElse(1);
        double s=Math.max(0.65,Math.min(3,36.0/Math.max(1,max))),ang=t*.025;
        double a=Math.cos(ang),b=Math.sin(ang);
        record P(double depth,int x,int y,int cor){}
        List<P> pontos=new ArrayList<>();
        double frac=total>0?Math.min(1,feitos/(double)total):1;
        int vis=(int)Math.ceil(celulas.size()*frac);
        for(int i=0;i<celulas.size();i++){
            var c=celulas.get(i);double rx=c.x()*a-c.z()*b,rz=c.x()*b+c.z()*a;
            int px=x+w/2+(int)(rx*s*1.6),py=y+63+(int)(rz*s*.65-c.y()*s);
            if(px<x+3||px>x+w-4||py<y+17||py>y+h-17)continue;
            pontos.add(new P(rz+c.y()*.2,px,py,i<vis?cor(c.bloco()):0xff304b5d));
        }
        pontos.sort(Comparator.comparingDouble(P::depth));
        int r=Math.max(1,(int)s);for(var p:pontos)ctx.fill(p.x()-r,p.y()-r,p.x()+r+1,p.y()+r+1,p.cor());
        ctx.fill(x+5,y+h-9,x+w-5,y+h-5,0xff384858);ctx.fill(x+5,y+h-9,x+5+(int)((w-10)*frac),y+h-5,0xff60d5ff);
        ctx.drawTextWithShadow(tr,total>0?feitos+" / "+total+" blocos":"Prévia do plano",x+6,y+h-20,0xffffffff);
    }
}
