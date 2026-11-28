package br.com.vozdacriacao;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
/** Renderiza cuboides reais com textura de pixels gerada e animação por peça. */
final class RenderArtefato {
    private record Chave(int cor,int detalhe,String pixels){}
    private static final Map<String,VisualArtefato> MODELOS=new LinkedHashMap<>(32,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,VisualArtefato> e){return size()>128;}
    };
    private static final Map<Chave,Identifier> TEXTURAS=new HashMap<>();
    private static final Identifier BASE=new Identifier("minecraft","textures/block/white_concrete.png");
    private static Identifier textura(VisualArtefato.Peca p){
        Chave k=new Chave(p.cor(),p.detalhe(),p.pixels());
        Identifier existente=TEXTURAS.get(k);if(existente!=null)return existente;
        if(TEXTURAS.size()>=128)return BASE;
        NativeImage img=new NativeImage(8,8,false);
        for(int y=0;y<8;y++)for(int x=0;x<8;x++){
            char px=p.pixels().charAt(y*8+x);int rgb=px=='1'?p.detalhe():p.cor();
            if(px=='2')rgb=((rgb>>1)&0x7f7f7f);
            int abgr=0xff000000|((rgb&255)<<16)|(rgb&0xff00)|((rgb>>16)&255);
            img.setColor(x,y,abgr);
        }
        Identifier id=new Identifier("vozdacriacao","generated/item_"+TEXTURAS.size());
        MinecraftClient.getInstance().getTextureManager().registerTexture(id,new NativeImageBackedTexture(img));
        TEXTURAS.put(k,id);return id;
    }
    static void limpar(){
        var tm=MinecraftClient.getInstance().getTextureManager();
        for(Identifier id:TEXTURAS.values())tm.destroyTexture(id);
        TEXTURAS.clear();MODELOS.clear();
    }
    static void render(ItemStack stack,ModelTransformationMode mode,MatrixStack m,VertexConsumerProvider buffers,int light,int overlay){
        var a=ArtefatoLaCraft.spec(stack);int cor=a==null?0x99bbff:a.cor();
        String json=stack.hasNbt()?stack.getNbt().getString("lacraft_visual"):"";
        VisualArtefato visual;
        if(json.isEmpty())visual=MODELOS.computeIfAbsent("legacy"+cor,k->VisualArtefato.legado(cor));
        else try{visual=MODELOS.computeIfAbsent(json,VisualArtefato::ler);}catch(RuntimeException ex){visual=VisualArtefato.legado(cor);}
        var mc=MinecraftClient.getInstance();double t=mc.world==null?0:mc.world.getTime()+mc.getTickDelta();
        // Nunca corta peças do modelo; o perfil leve só desliga animações no inventário.
        boolean anima=mode!=ModelTransformationMode.GUI||ConfigLaCraft.obter().perfilFPS!=0;
        for(var p:visual.pecas()){
            m.push();
            float sx=p.sx()/16,sy=p.sy()/16,sz=p.sz()/16;
            m.translate((p.x()+p.sx()/2)/16,(p.y()+p.sy()/2)/16,(p.z()+p.sz()/2)/16);
            if(anima)switch(p.animacao()){
                case "giro"->m.multiply(RotationAxis.POSITIVE_Y.rotation((float)(t*.08)));
                case "balanco"->m.multiply(RotationAxis.POSITIVE_Z.rotation((float)Math.sin(t*.09)*.25f));
                case "flutuar"->m.translate(0,Math.sin(t*.08)*.035,0);
                case "pulsar"->{float s=1+(float)Math.sin(t*.12)*.1f;m.scale(s,s,s);}
                default->{}
            }
            m.scale(sx,sy,sz);m.translate(-.5,-.5,-.5);
            Identifier tex=textura(p);int tint=tex.equals(BASE)?p.cor():0xffffff;
            VertexConsumer v=buffers.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
            cubo(v,m.peek(),tint,light,overlay);m.pop();
        }
    }
    private static final float[][][] FACES={
        {{0,0,0},{0,1,0},{1,1,0},{1,0,0}},
        {{1,0,1},{1,1,1},{0,1,1},{0,0,1}},
        {{0,0,1},{0,1,1},{0,1,0},{0,0,0}},
        {{1,0,0},{1,1,0},{1,1,1},{1,0,1}},
        {{0,1,0},{0,1,1},{1,1,1},{1,1,0}},
        {{0,0,1},{0,0,0},{1,0,0},{1,0,1}}
    };
    private static final int[][] NORMAIS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    private static final float[][] UV={{0,1},{0,0},{1,0},{1,1}};
    private static void cubo(VertexConsumer v,MatrixStack.Entry m,int c,int luz,int overlay){
        for(int i=0;i<6;i++)for(int j=0;j<4;j++){
            float[] p=FACES[i][j];int[] n=NORMAIS[i];
            v.vertex(m.getPositionMatrix(),p[0],p[1],p[2]).color((c>>16)&255,(c>>8)&255,c&255,255).texture(UV[j][0],UV[j][1]).overlay(overlay).light(luz).normal(m.getNormalMatrix(),n[0],n[1],n[2]).next();
        }
    }
}
