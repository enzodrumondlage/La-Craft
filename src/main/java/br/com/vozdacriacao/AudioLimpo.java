package br.com.vozdacriacao;
/** Remove componente DC e normaliza volume com ganho limitado; não inventa áudio. */
final class AudioLimpo {
    static boolean preparar(short[] pcm){
        if(pcm.length<4800)return false;
        double media=0;for(short s:pcm)media+=s;media/=pcm.length;
        double energia=0,pico=0;for(short s:pcm){double x=s-media;energia+=x*x;pico=Math.max(pico,Math.abs(x));}
        double rms=Math.sqrt(energia/pcm.length);
        if(rms<45||pico<150)return false;
        double ganho=Math.min(4,Math.min(3600/rms,30000/pico));
        for(int i=0;i<pcm.length;i++)pcm[i]=(short)Math.max(-32768,Math.min(32767,Math.round((pcm[i]-media)*ganho)));
        return true;
    }
}
