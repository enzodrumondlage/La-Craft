package br.com.vozdacriacao;
import java.nio.*;
import java.nio.charset.*;
/** Repara apenas sequências de UTF-8 interpretadas como página de código legada. */
final class TextoVoz {
    static String corrigir(String s){
        if(s==null)return "";
        if(!s.contains("Ã")&&!s.contains("Â")&&!s.contains("â"))return s;
        for(Charset legado:new Charset[]{Charset.forName("windows-1252"),StandardCharsets.ISO_8859_1}){
            try{
                ByteBuffer bytes=legado.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(s));
                String decoded=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(bytes).toString();
                return decoded;
            }catch(CharacterCodingException ignored){}
        }
        return s;
    }
}
