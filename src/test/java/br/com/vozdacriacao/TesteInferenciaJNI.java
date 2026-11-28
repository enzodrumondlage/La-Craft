package br.com.vozdacriacao;
import com.google.gson.JsonParser;
public final class TesteInferenciaJNI {
    public static void main(String[] args)throws Exception{
        try{
            String r=IAIntegrada.gerar("Responda somente um objeto JSON com chave nome, tipo, id, quantidade. Para o item pedra use minecraft:stone.","Crie uma pedra no Minecraft.");
            var o=JsonParser.parseString(r).getAsJsonObject();
            if(!o.has("id")||!o.get("id").getAsString().equals("minecraft:stone"))throw new AssertionError("Modelo não identificou o recurso: "+r);
            System.out.println("Inferência JNI validada: "+r);
        }finally{IAIntegrada.fechar();}
    }
}
