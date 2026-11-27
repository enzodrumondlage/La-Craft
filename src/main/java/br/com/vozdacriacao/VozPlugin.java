package br.com.vozdacriacao;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
/** Captura apenas o microfone local durante a gravação explícita. */
public final class VozPlugin implements VoicechatPlugin {
    @Override public String getPluginId(){return "vozdacriacao";}
    @Override public void registerEvents(EventRegistration r){
        r.registerEvent(ClientSoundEvent.class,e->BufferVoz.receber(e.getRawAudio()));
    }
}
