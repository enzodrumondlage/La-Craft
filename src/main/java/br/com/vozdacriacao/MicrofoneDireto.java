package br.com.vozdacriacao;
import javax.sound.sampled.*;
import java.util.Arrays;
/** Captura do dispositivo padrão do sistema somente enquanto o jogador grava explicitamente. */
final class MicrofoneDireto {
    private final short[] dados=new short[BufferVoz.MAX];
    private volatile boolean ativo=true, falhou=false;
    private int tamanho;
    private double nivel;
    static MicrofoneDireto iniciar(){
        var m=new MicrofoneDireto();
        Thread t=new Thread(m::capturar,"LaCraft-Microfone");t.setDaemon(true);t.start();return m;
    }
    private void capturar(){
        AudioFormat formato=new AudioFormat(48000f,16,1,true,false);
        byte[] bytes=new byte[1920];
        try(TargetDataLine line=AudioSystem.getTargetDataLine(formato)){
            line.open(formato,9600);
            if(!ativo)return;
            line.start();
            while(ativo){
                int n=line.read(bytes,0,bytes.length);
                if(n<=0)break;
                synchronized(this){
                    if(!ativo)break;
                    double soma=0;int k=0;
                    for(int i=0;i+1<n&&tamanho<dados.length;i+=2){
                        short v=(short)((bytes[i]&255)|(bytes[i+1]<<8));
                        dados[tamanho++]=v;soma+=(double)v*v;k++;
                    }
                    if(k>0)nivel=Math.sqrt(soma/k)/32768.0;
                    if(tamanho>=dados.length)break;
                }
            }
        }catch(Exception e){falhou=true;}
        finally{Arrays.fill(bytes,(byte)0);}
    }
    synchronized boolean pronto(){return tamanho>=4800;}
    boolean falhou(){return falhou;}
    synchronized double nivel(){return nivel;}
    synchronized short[] finalizar(){ativo=false;short[] r=Arrays.copyOf(dados,tamanho);Arrays.fill(dados,(short)0);tamanho=0;return r;}
    synchronized void cancelar(){ativo=false;Arrays.fill(dados,(short)0);tamanho=0;}
}
