package br.com.vozdacriacao;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
final class RenderSer extends EntityRenderer<SerLaCraft>{
    RenderSer(EntityRendererFactory.Context c){super(c);shadowRadius=.5f;}
    @Override public Identifier getTexture(SerLaCraft e){return new Identifier("minecraft","textures/block/white_concrete.png");}
    @Override public void render(SerLaCraft e,float yaw,float delta,MatrixStack m,VertexConsumerProvider v,int light){
        m.push();m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180-yaw));m.scale(e.largura(),e.altura(),e.comprimento());m.translate(-.5,0,-.5);
        RenderArtefato.renderVisual(e.visual(),0x99bbff,true,e.age+delta,e.getVelocity().horizontalLength()>.025,m,v,light,OverlayTexture.DEFAULT_UV);
        m.pop();super.render(e,yaw,delta,m,v,light);
    }
}
