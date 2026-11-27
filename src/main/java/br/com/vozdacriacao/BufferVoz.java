package br.com.vozdacriacao;
import java.util.Arrays;
/** Captura local de PCM mono, limitada a 15 segundos. */
public final class BufferVoz {
    public static final int TAXA=48000, MAX=15*TAXA;
    private static final short[] dados=new short[MAX];
    private static int tamanho;
    private static boolean ativo;
    private BufferVoz(){}
    public static synchronized void iniciar(){cancelar();ativo=true;}
    public static synchronized void receber(short[] pcm){
        if(!ativo||pcm==null)return;
        int n=Math.min(pcm.length,MAX-tamanho);
        System.arraycopy(pcm,0,dados,tamanho,n); tamanho+=n;
    }
    public static synchronized int tamanho(){return tamanho;}
    public static synchronized short[] finalizar(){short[] resultado=Arrays.copyOf(dados,tamanho);cancelar();return resultado;}
    public static synchronized void cancelar(){ativo=false;Arrays.fill(dados,0,tamanho,(short)0);tamanho=0;}
}
