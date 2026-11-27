package br.com.vozdacriacao;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.vosk.Model;
import org.vosk.Recognizer;
import com.google.gson.JsonParser;

public final class ReconhecimentoTest {
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("lacraft-teste-voz");
        try{
            Path p;
            try(InputStream in=ReconhecedorVoz.class.getResourceAsStream("/vozdacriacao/modelo-pt.zip")){
                if(in==null)throw new AssertionError("ZIP não encontrado");
                p=ReconhecedorVoz.prepararModelo(temp,in);
            }
            try(Model modelo=new Model(p.toString());Recognizer r=new Recognizer(modelo,16000f)){
                short[] silencio=new short[16000];r.acceptWaveForm(silencio,silencio.length);
                if(!JsonParser.parseString(r.getFinalResult()).getAsJsonObject().has("text"))throw new AssertionError("Resposta inválida");
            }
            System.out.println("Modelo Vosk carregado e áudio PCM processado");
        }finally{
            try(var walk=Files.walk(temp)){for(Path p:walk.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
