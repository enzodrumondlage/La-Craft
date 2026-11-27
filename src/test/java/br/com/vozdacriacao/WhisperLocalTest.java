package br.com.vozdacriacao;
import java.nio.*;
public final class WhisperLocalTest{
 public static void main(String[] args){
  byte[] wav=WhisperLocal.wav16(new short[]{100,200,300,400,500,600});
  var b=ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
  if(wav.length!=48||b.getInt(24)!=16000||b.getInt(40)!=4||b.getShort(44)!=200||b.getShort(46)!=500)throw new AssertionError("WAV inválido");
  System.out.println("WAV 16 kHz aprovado");
 }
}
