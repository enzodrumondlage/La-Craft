package br.com.vozdacriacao;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class VozCliente implements ClientModInitializer {
    private static KeyBinding voz,sugar;
    private static boolean gravando,sugando,bloqueio;
    private static int ticks,inicio,materia,custo;
    private static String pedido="Nenhum";
    private static final List<Fragmento> fragmentos=new ArrayList<>();
    private record Fragmento(Vec3d origem,int estado,int jogador,int inicio){}
    @Override public void onInitializeClient(){
        voz=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vozdacriacao.voz",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_F,"category.vozdacriacao"));
        sugar=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vozdacriacao.sugar",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_G,"category.vozdacriacao"));
        ClientTickEvents.END_CLIENT_TICK.register(VozCliente::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->limpar());
        ClientPlayNetworking.registerGlobalReceiver(VozDaCriacao.HUD,(c,h,b,s)->{int m=b.readVarInt(),k=b.readVarInt();String n=b.readString(80);c.execute(()->{materia=m;custo=k;pedido=n;});});
        ClientPlayNetworking.registerGlobalReceiver(VozDaCriacao.EFEITO,(c,h,b,s)->{Vec3d o=Vec3d.ofCenter(b.readBlockPos());int e=b.readVarInt(),id=b.readVarInt();c.execute(()->{if(fragmentos.size()<250)fragmentos.add(new Fragmento(o,e,id,ticks));});});
        HudRenderCallback.EVENT.register((ctx,delta)->{
            var mc=MinecraftClient.getInstance();
            if(mc.player==null||mc.options.hudHidden||!ClientPlayNetworking.canSend(VozDaCriacao.SUGAR))return;
            int x=8,y=8,l=214;
            ctx.fill(x,y,x+l,y+56,0xCC171B24);
            ctx.drawTextWithShadow(mc.textRenderer,"LA CRAFT • Voz da Criação",x+7,y+6,0x9BD8FF);
            ctx.drawTextWithShadow(mc.textRenderer,"Matéria: "+materia+(custo>0?" / "+custo:""),x+7,y+19,0xFFFFFF);
            ctx.fill(x+7,y+31,x+l-7,y+35,0xFF414855);
            if(custo>0)ctx.fill(x+7,y+31,x+7+(int)((l-14)*Math.min(1.0,materia/(double)custo)),y+35,0xFF4DE29C);
            String n=custo>0?"Pedido: "+pedido:(!pedido.equals("Nenhum")?pedido:"G: sugar | F: voz | /lacraft");
            ctx.drawTextWithShadow(mc.textRenderer,n,x+7,y+40,0xFFFFFF);
            if(gravando)ctx.drawTextWithShadow(mc.textRenderer,"● GRAVANDO LOCALMENTE",x,y+62,(ticks/5)%2==0?0xFF5555:0xFFAAAA);
        });
    }
    private static void tick(MinecraftClient mc){
        ticks++;
        if(mc.player==null||mc.world==null){limpar();return;}
        boolean habilitado=mc.currentScreen==null&&mc.isWindowFocused();
        boolean f=habilitado&&pressionada(mc,voz);
        if(!habilitado){BufferVoz.cancelar();gravando=false;bloqueio=true;}
        if(habilitado&&!f)bloqueio=false;
        if(f&&!gravando&&!bloqueio){BufferVoz.iniciar();gravando=true;inicio=ticks;}
        if(gravando&&(!f||ticks-inicio>=300)){
            short[] pcm=BufferVoz.finalizar();gravando=false;bloqueio=f;
            mc.player.sendMessage(Text.literal(pcm.length==0?"§eSem áudio. Confira o microfone do Simple Voice Chat.":String.format(Locale.ROOT,"§aVoz capturada: %.1f s. Teste local; áudio descartado.",pcm.length/(double)BufferVoz.TAXA)),true);
            Arrays.fill(pcm,(short)0);
        }
        boolean g=habilitado&&pressionada(mc,sugar)&&ClientPlayNetworking.canSend(VozDaCriacao.SUGAR);
        if(ClientPlayNetworking.canSend(VozDaCriacao.SUGAR)&&(g!=sugando||(g&&ticks%4==0))){var b=PacketByteBufs.create();b.writeBoolean(g);ClientPlayNetworking.send(VozDaCriacao.SUGAR,b);}
        sugando=g;
        for(Iterator<Fragmento> it=fragmentos.iterator();it.hasNext();){
            Fragmento fr=it.next();Entity alvo=mc.world.getEntityById(fr.jogador());int idade=ticks-fr.inicio();
            if(alvo==null||idade>=12){it.remove();continue;}
            double t=(idade+1)/12.0;Vec3d pos=fr.origem().lerp(alvo.getPos().add(0,1.2,0),t*t);
            mc.world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK,Block.getStateFromRawId(fr.estado())),pos.x,pos.y,pos.z,0,0,0);
            mc.world.addParticle(ParticleTypes.END_ROD,pos.x,pos.y,pos.z,0,0,0);
        }
    }
    private static boolean pressionada(MinecraftClient mc,KeyBinding b){
        InputUtil.Key t=KeyBindingHelper.getBoundKeyOf(b);if(t.getCode()<0)return false;
        long h=mc.getWindow().getHandle();
        if(t.getCategory()==InputUtil.Type.KEYSYM)return InputUtil.isKeyPressed(h,t.getCode());
        if(t.getCategory()==InputUtil.Type.MOUSE)return GLFW.glfwGetMouseButton(h,t.getCode())==GLFW.GLFW_PRESS;
        return b.isPressed();
    }
    private static void limpar(){BufferVoz.cancelar();gravando=false;sugando=false;bloqueio=false;materia=0;custo=0;pedido="Nenhum";fragmentos.clear();}
}
