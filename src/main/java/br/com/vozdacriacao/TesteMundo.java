package br.com.vozdacriacao;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.nbt.NbtCompound;
public final class TesteMundo implements FabricGameTest {
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void detritos(TestContext c){
        var o=new net.minecraft.util.math.Vec3d(0,0,0);
        var a=DetritosVirtuais.posicao(o,"onda",1,12,0);var b=DetritosVirtuais.posicao(o,"onda",1,12,20);
        c.assertTrue(!a.equals(b),"Onda animada");c.assertTrue(a.length()<10&&b.length()<10,"Limites da onda");c.complete();
    }
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE)
    public void veiculoOriginal(TestContext c){
        String p="{\"x\":2,\"y\":0,\"z\":2,\"sx\":4,\"sy\":4,\"sz\":4,\"cor\":\"#336699\"}";
        var spec=new SerSpec(true,false,"["+p+","+p+","+p+"]","veiculo",.32f,1.8f,1.2f,2.8f);
        var e=c.spawnEntity(SerLaCraft.TIPO,1,2,1);e.aplicar(spec);
        c.assertTrue(e.veiculo()&&Math.abs(e.getWidth()-2.8f)<.01f,"Dimensões e modo de veículo");
        NbtCompound n=new NbtCompound();e.writeCustomDataToNbt(n);
        var outro=SerLaCraft.TIPO.create(c.getWorld());c.assertTrue(outro!=null,"Tipo registrado");outro.readCustomDataFromNbt(n);
        c.assertTrue(outro.veiculo()&&Math.abs(outro.velocidade()-.32f)<.001f&&Math.abs(outro.comprimento()-2.8f)<.01f,"Dados do veículo persistentes");outro.discard();c.complete();
    }
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
