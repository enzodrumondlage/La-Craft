package br.com.vozdacriacao;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;
import net.minecraft.server.MinecraftServer;
import java.util.*;
public final class MateriaState extends PersistentState {
    public static final int LIMITE=100000;
    private final Map<UUID,Integer> saldos=new HashMap<>();
    public static MateriaState obter(MinecraftServer s){
        return s.getOverworld().getPersistentStateManager().getOrCreate(MateriaState::ler,MateriaState::new,"vozdacriacao_materia");
    }
    public int saldo(UUID id){return saldos.getOrDefault(id,0);}
    public void alterar(UUID id,int delta){saldos.put(id,Math.max(0,Math.min(LIMITE,saldo(id)+delta)));markDirty();}
    private static MateriaState ler(NbtCompound nbt){
        MateriaState e=new MateriaState(); NbtCompound d=nbt.getCompound("saldos");
        for(String k:d.getKeys())try{e.saldos.put(UUID.fromString(k),Math.max(0,Math.min(LIMITE,d.getInt(k))));}catch(IllegalArgumentException ex){}
        return e;
    }
    @Override public NbtCompound writeNbt(NbtCompound nbt){
        NbtCompound d=new NbtCompound();saldos.forEach((id,v)->d.putInt(id.toString(),v));nbt.put("saldos",d);return nbt;
    }
}
