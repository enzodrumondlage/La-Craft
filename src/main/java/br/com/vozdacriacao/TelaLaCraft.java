package br.com.vozdacriacao;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/** Duas páginas de opções, inclusive em resoluções pequenas. */
public final class TelaLaCraft extends Screen {
    private final Screen anterior;
    private final ConfigLaCraft rascunho=ConfigLaCraft.obter().copia();
    private int pagina;
    private TextFieldWidget modelo,chave;
    private String aviso="";
    public TelaLaCraft(Screen anterior){super(Text.literal("La Craft — Configurações"));this.anterior=anterior;}
    private String sn(boolean b){return b?"Sim":"Não";}
    private ButtonWidget botao(int x,int y,int w,String t,Runnable r){
        return addDrawableChild(ButtonWidget.builder(Text.literal(t),b->r.run()).dimensions(x,y,w,20).build());
    }
    private void atualizar(){capturar();clearChildren();init();}
    private void capturar(){
        if(modelo!=null)rascunho.modelo=modelo.getText().trim();
        if(chave!=null&&!chave.getText().isBlank())rascunho.chaveApi=chave.getText().trim();
        modelo=null;chave=null;
    }
    @Override protected void init(){
        int w=Math.min(310,width-24),x=(width-w)/2,y=28;
        boolean remoto=client!=null&&client.world!=null&&!client.isInSingleplayer();
        if(pagina==0){
            var materia=botao(x,y,w,"Precisar coletar blocos: "+sn(rascunho.coletarMateria),()->{rascunho.coletarMateria=!rascunho.coletarMateria;atualizar();});materia.active=!remoto;
            botao(x,y+22,w,"Exibir HUD: "+sn(rascunho.hud),()->{rascunho.hud=!rascunho.hud;atualizar();});
            botao(x,y+44,w,"Partículas de absorção: "+sn(rascunho.particulas),()->{rascunho.particulas=!rascunho.particulas;atualizar();});
            botao(x,y+66,w,"Microfone: "+sn(rascunho.voz),()->{rascunho.voz=!rascunho.voz;atualizar();});
            var sugar=botao(x,y+88,w,"Sugador habilitado: "+sn(rascunho.sugar),()->{rascunho.sugar=!rascunho.sugar;atualizar();});sugar.active=!remoto;
            int metade=(w-4)/2;
            var s=botao(x,y+110,metade,"Absorção: "+rascunho.velocidadeSugador,()->{rascunho.velocidadeSugador=rascunho.velocidadeSugador%6+1;atualizar();});s.active=!remoto;
            var c=botao(x+metade+4,y+110,w-metade-4,"Construção: "+rascunho.velocidadeConstrucao,()->{rascunho.velocidadeConstrucao=rascunho.velocidadeConstrucao>=16?2:rascunho.velocidadeConstrucao+2;atualizar();});c.active=!remoto;
        }else{
            var prov=botao(x,y,w,"IA: "+rascunho.provedor,()->{
                capturar();
                rascunho.provedor=switch(rascunho.provedor){case "CATALOGO"->"OLLAMA";case "OLLAMA"->"OPENAI";default->"CATALOGO";};
                rascunho.modelo=rascunho.provedor.equals("OPENAI")?"gpt-4o-mini":"qwen2.5:3b";atualizar();
            });prov.active=!remoto;
            modelo=new TextFieldWidget(textRenderer,x,y+24,w,20,Text.literal("Modelo da IA"));modelo.setMaxLength(80);modelo.setText(rascunho.modelo);modelo.setEditable(!remoto);addDrawableChild(modelo);
            // A chave já salva nunca é mostrada; o campo vazio mantém a anterior.
            chave=new TextFieldWidget(textRenderer,x,y+48,w,20,Text.literal("Nova chave API"));
            chave.setMaxLength(512);chave.setPlaceholder(Text.literal(rascunho.chaveApi.isEmpty()?"Cole a nova chave API":"Chave salva — cole para substituir"));
            chave.setRenderTextProvider((str,off)->net.minecraft.text.OrderedText.styledForwardsVisitedString("*".repeat(str.length()),net.minecraft.text.Style.EMPTY));
            chave.setEditable(!remoto);addDrawableChild(chave);
            var consent=botao(x,y+72,w,"Autorizar envio de texto à OpenAI: "+sn(rascunho.permitirNuvem),()->{rascunho.permitirNuvem=!rascunho.permitirNuvem;atualizar();});consent.active=!remoto;
            int metade=(w-4)/2;
            botao(x,y+94,metade,"Site OpenAI API",()->Util.getOperatingSystem().open("https://platform.openai.com/api-keys"));
            botao(x+metade+4,y+94,w-metade-4,"Baixar Ollama",()->Util.getOperatingSystem().open("https://ollama.com/download"));
            var apagar=botao(x,y+116,w,"Apagar chave salva",()->{if(chave!=null)chave.setText("");rascunho.chaveApi="";atualizar();});apagar.active=!remoto;
        }
        int rodape=height-26,metade=(w-8)/3;
        botao(x,rodape,metade,pagina==0?"IA >":"< Jogo",()->{capturar();pagina=1-pagina;atualizar();});
        botao(x+metade+4,rodape,metade,"Salvar",()->{capturar();if(ConfigLaCraft.salvar(rascunho)){close();}else{aviso="Erro ao salvar.";atualizar();}});
        botao(x+2*(metade+4),rodape,w-2*(metade+4),"Cancelar",this::close);
    }
    @Override public void resize(net.minecraft.client.MinecraftClient mc,int w,int h){capturar();super.resize(mc,w,h);}
    @Override public void render(DrawContext ctx,int mx,int my,float delta){
        renderBackground(ctx);ctx.drawCenteredTextWithShadow(textRenderer,title,width/2,8,0x9BD8FF);
        super.render(ctx,mx,my,delta);
        if(!aviso.isEmpty())ctx.drawCenteredTextWithShadow(textRenderer,aviso,width/2,height-39,0xFF7777);
        else if(height>=215)ctx.drawCenteredTextWithShadow(textRenderer,client!=null&&client.world!=null&&!client.isInSingleplayer()?"Servidor remoto controla IA e matéria.":(pagina==1?"API pode cobrar; use somente com autorização.":"Ajuste as opções e clique em Salvar."),width/2,height-39,0xBBBBBB);
    }
    @Override public void close(){if(client!=null)client.setScreen(anterior);}
}
