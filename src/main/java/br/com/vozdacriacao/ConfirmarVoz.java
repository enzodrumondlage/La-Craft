package br.com.vozdacriacao;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
public final class ConfirmarVoz extends Screen{
    private final String inicial;
    private TextFieldWidget campo;
    public ConfirmarVoz(String t){super(Text.literal("Confirmar desejo"));inicial=t;}
    @Override protected void init(){
        int w=Math.min(width-24,360),x=(width-w)/2,y=height/2;
        campo=new TextFieldWidget(textRenderer,x,y-22,w,20,Text.literal("Frase reconhecida"));campo.setMaxLength(240);campo.setText(inicial);addDrawableChild(campo);setInitialFocus(campo);
        addDrawableChild(ButtonWidget.builder(Text.literal("Realizar desejo"),b->enviar()).dimensions(x,y+8,w/2-2,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancelar"),b->close()).dimensions(x+w/2+2,y+8,w/2-2,20).build());
    }
    private void enviar(){
        String t=campo.getText().trim();if(t.isEmpty()||!ClientPlayNetworking.canSend(VozDaCriacao.PEDIR))return;
        var b=PacketByteBufs.create();b.writeString(t,240);ClientPlayNetworking.send(VozDaCriacao.PEDIR,b);close();
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void render(DrawContext c,int x,int y,float delta){renderBackground(c);c.drawCenteredTextWithShadow(textRenderer,title,width/2,height/2-44,0xffffff);super.render(c,x,y,delta);}
}
