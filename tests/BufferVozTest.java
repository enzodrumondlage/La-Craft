import br.com.vozdacriacao.BufferVoz;
import br.com.vozdacriacao.Pedido;
public class BufferVozTest {
    public static void main(String[] args){
        BufferVoz.receber(new short[]{9});
        if(BufferVoz.tamanho()!=0)throw new AssertionError();
        BufferVoz.iniciar();BufferVoz.receber(new short[]{1,2,3});
        if(BufferVoz.finalizar().length!=3||BufferVoz.tamanho()!=0)throw new AssertionError();
        BufferVoz.iniciar();BufferVoz.receber(new short[BufferVoz.MAX+1]);
        if(BufferVoz.finalizar().length!=BufferVoz.MAX)throw new AssertionError();
        if(Pedido.interpretar("Estátua de capivara").tipo()!=Pedido.Tipo.CAPIVARA)throw new AssertionError();
        if(Pedido.interpretar("zzz")!=null)throw new AssertionError();
        System.out.println("Testes locais OK");
    }
}
