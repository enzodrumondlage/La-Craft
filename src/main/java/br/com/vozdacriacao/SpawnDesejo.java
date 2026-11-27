package br.com.vozdacriacao;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import java.util.Set;
/** Política conservadora para criaturas especiais; não altera registros ou biomas. */
public final class SpawnDesejo {
    private static final Set<String> MODS=Set.of("bosses_of_mass_destruction","bossesofmassdestruction","adventurez","mutantmonsters","mutant_monsters","mythicmounts","thegraveyard","creeperoverhaul","endermanoverhaul","iceandfire","dragonmounts","titans","thetitans","titan");
    public static boolean bloquear(Entity e){
        if(!ConfigLaCraft.obter().criaturasPorDesejo||!(e instanceof MobEntity)||e.getCommandTags().contains("lacraft_autorizado"))return false;
        var id=Registries.ENTITY_TYPE.getId(e.getType());
        if(id.getNamespace().equals("minecraft"))return false;
        String path=id.getPath();
        return MODS.contains(id.getNamespace())||path.contains("titan")||path.contains("dragon")||path.contains("boss")||path.contains("mutant");
    }
    public static boolean invocar(ServerWorld w,Entity e){e.addCommandTag("lacraft_autorizado");return w.spawnEntity(e);}
}
