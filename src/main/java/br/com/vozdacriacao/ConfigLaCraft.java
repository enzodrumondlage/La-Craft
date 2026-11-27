package br.com.vozdacriacao;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.slf4j.LoggerFactory;

/** Configuração local da instalação. O servidor decide as regras de jogo. */
public final class ConfigLaCraft {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path ARQUIVO=FabricLoader.getInstance().getConfigDir().resolve("lacraft.json");
    private static volatile ConfigLaCraft atual=carregar();
    public boolean coletarMateria=true;
    public boolean hud=true;
    public boolean particulas=true;
    public boolean sons=true;
    public boolean chatDesejos=true;
    public boolean chatTodos=false;
    public boolean vozNuvem=false;
    public boolean whisperLocal=false;
    public boolean confirmarVoz=true;
    public int qualidadeCasa=4;
    public int volumeSons=80;
    public boolean voz=true;
    public boolean sugar=true;
    public int velocidadeSugador=3;
    public int velocidadeConstrucao=8;
    public String provedor="CATALOGO";
    public String modelo="qwen2.5:3b";
    public String chaveApi="";
    public boolean permitirNuvem=false;
    public static ConfigLaCraft obter(){return atual;}
    public ConfigLaCraft copia(){return GSON.fromJson(GSON.toJson(this),ConfigLaCraft.class);}
    private void validar(){
        qualidadeCasa=Math.max(1,Math.min(4,qualidadeCasa));volumeSons=Math.max(0,Math.min(100,volumeSons));
        velocidadeSugador=Math.max(1,Math.min(6,velocidadeSugador));
        velocidadeConstrucao=Math.max(2,Math.min(16,velocidadeConstrucao));
        if(!java.util.Set.of("CATALOGO","OLLAMA","OPENAI").contains(provedor==null?"":provedor))provedor="CATALOGO";
        if(modelo==null||!modelo.matches("[A-Za-z0-9_.:/-]{1,80}"))modelo="qwen2.5:3b";
        if(chaveApi==null||chaveApi.length()>512||chaveApi.chars().anyMatch(Character::isWhitespace))chaveApi="";
    }
    private static ConfigLaCraft carregar(){
        try{
            if(Files.isRegularFile(ARQUIVO)){
                ConfigLaCraft c=GSON.fromJson(Files.readString(ARQUIVO),ConfigLaCraft.class);
                if(c!=null){c.validar();return c;}
            }
        }catch(Exception e){LoggerFactory.getLogger("LaCraft").warn("Não foi possível ler configuração; usando padrões.");}
        ConfigLaCraft c=new ConfigLaCraft();
        String legado=System.getenv("LACRAFT_OLLAMA_MODEL");
        if(legado!=null&&!legado.isBlank()){c.provedor="OLLAMA";c.modelo=legado;c.validar();}
        return c;
    }
    public static synchronized boolean salvar(ConfigLaCraft c){
        c=c.copia();c.validar();
        try{
            Files.createDirectories(ARQUIVO.getParent());
            Path tmp=Files.createTempFile(ARQUIVO.getParent(),"lacraft-",".tmp");
            try{
                Files.writeString(tmp,GSON.toJson(c),StandardCharsets.UTF_8);
                try{Files.setPosixFilePermissions(tmp,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));}catch(UnsupportedOperationException ignored){}
                Files.move(tmp,ARQUIVO,StandardCopyOption.REPLACE_EXISTING);
            }finally{Files.deleteIfExists(tmp);}
            atual=c;return true;
        }catch(Exception e){LoggerFactory.getLogger("LaCraft").warn("Não foi possível salvar configuração.");return false;}
    }
    public static String chave(ConfigLaCraft c){String env=System.getenv("OPENAI_API_KEY");return !c.chaveApi.isBlank()?c.chaveApi:(env==null?"":env.trim());}
}
