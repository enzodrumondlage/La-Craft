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
    private static boolean gravando,sugando,bloqueio,processando;
    private static long geracao;
    private static int ticks,inicio,materia,custo,fimGravacao=-1;
    private static MicrofoneDireto microfone;
    private static String pedido="Nenhum";private static boolean ilimitado;private static int feitos,total;
    private static final List<Fragmento> fragmentos=new ArrayList<>();
    private record Fragmento(Vec3d origem,int estado,int jogador,int inicio){}
    @Override public void onInitializeClient(){
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register((stack,tint)->{
            var a=ArtefatoLaCraft.spec(stack);return a==null?0xffffff:a.cor();
        },ArtefatoLaCraft.ITEM);
        voz=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vozdacriacao.voz",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_F,"category.vozdacriacao"));
        sugar=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vozdacriacao.sugar",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_G,"category.vozdacriacao"));
        ClientTickEvents.END_CLIENT_TICK.register(VozCliente::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->limpar());
        ClientPlayNetworking.registerGlobalReceiver(VozDaCriacao.HUD,(c,h,b,s)->{int m=b.readVarInt(),k=b.readVarInt();String n=b.readString(80);boolean inf=b.isReadable()&&b.readBoolean();int f=b.isReadable()?b.readVarInt():0,tt=b.isReadable()?b.readVarInt():0;c.execute(()->{materia=m;custo=k;pedido=n;ilimitado=inf;feitos=f;total=tt;});});
        ClientPlayNetworking.registerGlobalReceiver(VozDaCriacao.PREVIA,(c,h,b,s)->{String n=b.readString(80);int qtd=b.readVarInt();if(qtd<0||qtd>800)return;List<Pedido.Celula> cs=new ArrayList<>();for(int i=0;i<qtd;i++)cs.add(new Pedido.Celula(b.readInt(),b.readInt(),b.readInt(),b.readString(120)));c.execute(()->PreviaHud.receber(n,cs,ticks));});
        ClientPlayNetworking.registerGlobalReceiver(VozDaCriacao.EFEITO,(c,h,b,s)->{Vec3d o=Vec3d.ofCenter(b.readBlockPos());int e=b.readVarInt(),id=b.readVarInt();c.execute(()->{if(ConfigLaCraft.obter().particulas&&fragmentos.size()<250)fragmentos.add(new Fragmento(o,e,id,ticks));});});
        ClientPlayNetworking.registerGlobalReceiver(VozDaCriacao.SOM,(c,h,b,s)->{
            var pos=b.readBlockPos();int tipo=b.readUnsignedByte();
            c.execute(()->{
                if(c.world==null||!ConfigLaCraft.obter().sons)return;
                var evento=switch(tipo){
                    case 0->net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
                    case 1->net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE;
                    case 2->net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE;
                    case 3->net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP;
                    case 4->net.minecraft.sound.SoundEvents.BLOCK_DISPENSER_FAIL;
                    default->null;
                };
                if(evento==null)return;
                float volume=tipo==0?0.32f:tipo==2?0.42f:0.65f;
                float pitch=tipo==0?1.25f+c.world.getRandom().nextFloat()*0.35f:tipo==2?1.15f:1f;
                c.world.playSound(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,evento,net.minecraft.sound.SoundCategory.PLAYERS,volume*ConfigLaCraft.obter().volumeSons/100f,pitch,false);
            });
        });
        HudRenderCallback.EVENT.register((ctx,delta)->{
            var mc=MinecraftClient.getInstance();
            if(!ConfigLaCraft.obter().hud||mc.player==null||mc.options.hudHidden||!ClientPlayNetworking.canSend(VozDaCriacao.SUGAR))return;
            int x=8,y=8,l=214;
            ctx.fill(x,y,x+l,y+56,0xCC171B24);
            ctx.drawTextWithShadow(mc.textRenderer,"LA CRAFT • Voz da Criação",x+7,y+6,0x9BD8FF);
            ctx.drawTextWithShadow(mc.textRenderer,"Matéria: "+(ilimitado?"∞":Integer.toString(materia))+(custo>0?" / "+custo:""),x+7,y+19,0xFFFFFF);
            ctx.fill(x+7,y+31,x+l-7,y+35,0xFF414855);
            if(custo>0)ctx.fill(x+7,y+31,x+7+(int)((l-14)*Math.min(1.0,materia/(double)custo)),y+35,0xFF4DE29C);
            String n=custo>0?"Pedido: "+pedido:(!pedido.equals("Nenhum")?pedido:"G: sugar | F: voz | /lacraft");
            ctx.drawTextWithShadow(mc.textRenderer,n,x+7,y+40,0xFFFFFF);
            PreviaHud.render(ctx,ticks,feitos,total);
            if(processando)ctx.drawTextWithShadow(mc.textRenderer,"Reconhecendo voz...",x,y+62,0x9BD8FF);
            if(gravando){ctx.drawTextWithShadow(mc.textRenderer,microfone!=null&&!microfone.pronto()&&!microfone.falhou()?"Preparando microfone...":fimGravacao>=0?"Finalizando áudio...":"● GRAVANDO LOCALMENTE",x,y+62,(ticks/5)%2==0?0xFF5555:0xFFAAAA);ctx.fill(x,y+74,x+100,y+78,0xFF414855);ctx.fill(x,y+74,x+(int)(100*Math.min(1,(microfone!=null&&microfone.pronto()?microfone.nivel():BufferVoz.nivel())*8)),y+78,0xFF4DE29C);}
        });
    }
    private static void tick(MinecraftClient mc){
        ticks++;
        if(mc.player==null||mc.world==null){limpar();return;}
        boolean habilitado=mc.currentScreen==null&&mc.isWindowFocused();
        boolean f=ConfigLaCraft.obter().voz&&habilitado&&pressionada(mc,voz);
        if(!habilitado){cancelarCaptura();gravando=false;bloqueio=true;}
        if(habilitado&&!f)bloqueio=false;
        if(f&&!gravando&&!bloqueio&&!processando&&ClientPlayNetworking.canSend(VozDaCriacao.PEDIR)){BufferVoz.iniciar();microfone=ConfigLaCraft.obter().microfoneDireto?MicrofoneDireto.iniciar():null;gravando=true;inicio=ticks;fimGravacao=-1;}
        if(gravando&&!f&&fimGravacao<0)fimGravacao=ticks+5;
        if(gravando&&(ticks-inicio>=600||(fimGravacao>=0&&ticks>=fimGravacao))){
            short[] pcm=BufferVoz.finalizar();
            if(microfone!=null){
                short[] direto=microfone.finalizar();microfone=null;
                if(direto.length>=12000&&AudioLimpo.preparar(direto)){Arrays.fill(pcm,(short)0);pcm=direto;}
                else {Arrays.fill(direto,(short)0);mc.player.sendMessage(Text.literal("§eMicrofone direto sem áudio suficiente; usando Simple Voice Chat."),false);}
            }
            gravando=false;bloqueio=f;fimGravacao=-1;
            if(!AudioLimpo.preparar(pcm)){Arrays.fill(pcm,(short)0);mc.player.sendMessage(Text.literal("§eÁudio muito baixo ou silencioso. Configure o microfone e a ativação do Simple Voice Chat."),true);}
            else {
                processando=true;long g=++geracao;
                var cfg=ConfigLaCraft.obter();
                java.util.concurrent.CompletableFuture<WhisperLocal.Resultado> tarefa=cfg.whisperLocal?WhisperLocal.transcrever(pcm):(cfg.vozNuvem&&cfg.permitirNuvem&&!ConfigLaCraft.chave(cfg).isBlank()?VozNuvem.transcrever(pcm):ReconhecedorVoz.transcrever(pcm)).thenApply(t->new WhisperLocal.Resultado(t,false));
                tarefa.whenComplete((resultado,erro)->mc.execute(()->{
                    if(g!=geracao)return;
                    processando=false;
                    if(mc.player==null||!ClientPlayNetworking.canSend(VozDaCriacao.PEDIR))return;
                    if(erro!=null){org.slf4j.LoggerFactory.getLogger("LaCraft").warn("Falha no reconhecimento de voz",erro);mc.player.sendMessage(Text.literal("§eFalha na transcrição. Confira a configuração e latest.log."),false);return;}
                    if(resultado.fallback())mc.player.sendMessage(Text.literal("§eWhisper local indisponível; usando Vosk."),false);
                    String frase=TextoVoz.corrigir(resultado.texto());
                    if(frase.isBlank()){mc.player.sendMessage(Text.literal("§eNão entendi. Fale novamente."),true);return;}
                    String texto=frase.substring(0,Math.min(frase.length(),240));
                    mc.player.sendMessage(Text.literal("§bEntendi: "+texto),false);
                    if(ConfigLaCraft.obter().confirmarVoz){if(mc.currentScreen==null)mc.setScreen(new ConfirmarVoz(texto));else mc.player.sendMessage(Text.literal("§eFrase não enviada: feche o menu e repita ou escreva no chat."),false);return;}
                    var b=PacketByteBufs.create();b.writeString(texto,240);ClientPlayNetworking.send(VozDaCriacao.PEDIR,b);
                }));
            }
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
    private static void cancelarCaptura(){BufferVoz.cancelar();if(microfone!=null){microfone.cancelar();microfone=null;}fimGravacao=-1;}
    private static void limpar(){geracao++;processando=false;cancelarCaptura();gravando=false;sugando=false;bloqueio=false;materia=0;custo=0;pedido="Nenhum";fragmentos.clear();PreviaHud.limpar();total=0;feitos=0;ilimitado=false;}
}
