package br.com.vozdacriacao;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.nbt.NbtCompound;
public final class TesteMundo implements FabricGameTest {
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void entidadeOriginal(TestContext c){
        String p="{\"x\":2,\"y\":0,\"z\":2,\"sx\":4,\"sy\":4,\"sz\":4,\"cor\":\"#336699\"}";
        var spec=new SerSpec(true,true,"["+p+","+p+","+p+"]");
        var ser=c.spawnEntity(SerLaCraft.TIPO,1,2,1);ser.aplicar(spec);
        NbtCompound n=new NbtCompound();ser.writeCustomDataToNbt(n);
        var outro=SerLaCraft.TIPO.create(c.getWorld());c.assertTrue(outro!=null,"Tipo registrado");
        outro.readCustomDataFromNbt(n);c.assertTrue(outro.montavel()&&outro.visual().equals(spec.visual()),"Visual e montaria persistentes");
        c.assertTrue(ser.getMaxHealth()==24f,"Atributos registrados");outro.discard();c.complete();
    }
}
