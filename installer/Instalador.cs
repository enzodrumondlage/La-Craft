using System;
using System.IO;
using System.IO.Compression;
using System.Collections;
using System.Collections.Generic;
using System.Net;
using System.Reflection;
using System.Security.Cryptography;
using System.Web.Script.Serialization;
using System.Linq;

class Instalador {
    const string Versao="0.9.13";
    class Mod { public string Nome, Url, Hash; public long Tamanho; }
    class Pacote : IDisposable {
        public MemoryStream Bytes;
        public ZipArchive Mrpack;
        public List<Mod> Mods=new List<Mod>();
        public ZipArchiveEntry Proprio;
        public void Dispose(){if(Mrpack!=null)Mrpack.Dispose();if(Bytes!=null)Bytes.Dispose();}
    }
    static string NomeSeguro(string s) {
        if(String.IsNullOrWhiteSpace(s)||s!=Path.GetFileName(s)||s.IndexOfAny(Path.GetInvalidFileNameChars())>=0||s.EndsWith(".")||s.EndsWith(" ")||!s.EndsWith(".jar",StringComparison.OrdinalIgnoreCase)||s.Length>180)throw new IOException("Nome JAR invalido: "+s);
        return s;
    }
    static string Hex(byte[] a){return BitConverter.ToString(a).Replace("-","").ToLowerInvariant();}
    static string Hash(string p){using(var h=SHA512.Create())using(var s=File.OpenRead(p))return Hex(h.ComputeHash(s));}
    static bool Valido(string p,Mod m){return File.Exists(p)&&new FileInfo(p).Length==m.Tamanho&&Hash(p)==m.Hash;}
    static Pacote Ler(){
        var p=new Pacote();
        try{
            using(var s=Assembly.GetExecutingAssembly().GetManifestResourceStream("modpack.zip"))
            using(var outer=new ZipArchive(s,ZipArchiveMode.Read)){
                var entrada=outer.Entries.Single(x=>x.FullName.EndsWith(".zip",StringComparison.OrdinalIgnoreCase)||x.FullName.EndsWith(".mrpack",StringComparison.OrdinalIgnoreCase));
                using(var x=entrada.Open()){
                    p.Bytes=new MemoryStream();x.CopyTo(p.Bytes);p.Bytes.Position=0;
                }
            }
            using(var z=new ZipArchive(p.Bytes,ZipArchiveMode.Read,true)){
                var interno=z.Entries.SingleOrDefault(x=>x.FullName.EndsWith(".mrpack",StringComparison.OrdinalIgnoreCase));
                if(interno!=null){
                    var ms=new MemoryStream();using(var s=interno.Open())s.CopyTo(ms);ms.Position=0;
                    p.Bytes=ms;
                }else p.Bytes.Position=0;
            }
            p.Mrpack=new ZipArchive(p.Bytes,ZipArchiveMode.Read,true);
            var js=new JavaScriptSerializer(){MaxJsonLength=4000000};
            Dictionary<string,object> index;
            using(var reader=new StreamReader(p.Mrpack.GetEntry("modrinth.index.json").Open()))index=js.Deserialize<Dictionary<string,object>>(reader.ReadToEnd());
            var deps=(Dictionary<string,object>)index["dependencies"];
            if((string)deps["minecraft"]!="1.20.1"||!deps.ContainsKey("fabric-loader"))throw new IOException("Versao de modpack incorreta");
            var nomes=new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            foreach(var obj in (IEnumerable)index["files"]){
                var d=(Dictionary<string,object>)obj;var path=(string)d["path"];
                if(!path.StartsWith("mods/"))throw new IOException("Caminho invalido");
                var nome=NomeSeguro(path.Substring(5));if(!nomes.Add(nome))throw new IOException("JAR duplicado");
                var env=(Dictionary<string,object>)d["env"];
                if(env.ContainsKey("client")&&(string)env["client"]=="unsupported")continue;
                var h=(string)((Dictionary<string,object>)d["hashes"])["sha512"];
                var url=(string)((IEnumerable)d["downloads"]).Cast<object>().First();
                var u=new Uri(url);long tam=Convert.ToInt64(d["fileSize"]);
                if(u.Scheme!="https"||u.Host!="cdn.modrinth.com"||h.Length!=128||h.Any(c=>!"0123456789abcdef".Contains(c))||tam<=0||tam>500000000L)throw new IOException("Manifesto invalido");
                p.Mods.Add(new Mod{Nome=nome,Url=url,Hash=h,Tamanho=tam});
            }
            p.Proprio=p.Mrpack.Entries.Single(e=>e.FullName.StartsWith("overrides/mods/")&&e.FullName.EndsWith(".jar"));
            if(NomeSeguro(p.Proprio.FullName.Substring("overrides/mods/".Length))!="la-craft-"+Versao+".jar"||p.Mods.Count<50)throw new IOException("Pacote incompleto");
            return p;
        }catch{p.Dispose();throw;}
    }
    static void Instalar(Pacote p,string raiz){
        raiz=Path.GetFullPath(raiz);
        if(!Directory.Exists(raiz))throw new IOException("Pasta do Minecraft inexistente");
        string mods=Path.Combine(raiz,"mods"),stage=Path.Combine(raiz,"lacraft-stage-"+Guid.NewGuid().ToString("N")),backup=Path.Combine(raiz,"mods-backup-"+DateTime.Now.ToString("yyyyMMdd-HHmmss")+"-"+Guid.NewGuid().ToString("N").Substring(0,6));
        Directory.CreateDirectory(stage);
        bool moved=false,done=false;
        try{
            int i=0;
            using(var web=new WebClient())foreach(var m in p.Mods){
                string alvo=Path.Combine(stage,m.Nome),antigo=Path.Combine(mods,m.Nome);
                Console.WriteLine("[{0}/{1}] {2}",++i,p.Mods.Count,m.Nome);
                if(Valido(antigo,m))File.Copy(antigo,alvo);
                else{web.DownloadFile(m.Url,alvo);}
                if(!Valido(alvo,m))throw new IOException("SHA-512 ou tamanho incorreto: "+m.Nome);
            }
            string proprio=Path.Combine(stage,NomeSeguro(p.Proprio.FullName.Substring("overrides/mods/".Length)));
            using(var src=p.Proprio.Open())using(var dst=File.Create(proprio))src.CopyTo(dst);
            using(var jar=ZipFile.OpenRead(proprio)){if(jar.GetEntry("fabric.mod.json")==null)throw new IOException("JAR proprio invalido");}
            if(Directory.Exists(mods)){Directory.Move(mods,backup);moved=true;}
            try{Directory.Move(stage,mods);done=true;}catch{if(moved&&!Directory.Exists(mods))Directory.Move(backup,mods);throw;}
            Console.WriteLine("Instalado. "+(moved?"Backup: "+backup:"Sem pasta antiga."));
        }finally{if(!done&&Directory.Exists(stage))Directory.Delete(stage,true);}
    }
    static int Main(string[] args){
        try{
            ServicePointManager.SecurityProtocol=SecurityProtocolType.Tls12;
            using(var p=Ler()){
                Console.WriteLine("La Craft Infinite "+Versao+" LEVE - EXPERIMENTAL");
                Console.WriteLine("Pacote lido: "+p.Mods.Count+" mods de cliente + La Craft.");
                if(args.Length==1&&args[0]=="--verify"){Console.WriteLine("Manifesto e pacote embutido verificados.");return 0;}
                if(args.Length==1&&args[0]=="--ci-install"){
                    string raiz=Path.Combine(Path.GetTempPath(),"lacraft-ci-"+Guid.NewGuid().ToString("N"));
                    Directory.CreateDirectory(Path.Combine(raiz,"mods"));Directory.CreateDirectory(Path.Combine(raiz,"saves"));
                    File.WriteAllText(Path.Combine(raiz,"mods","anterior.txt"),"backup");File.WriteAllText(Path.Combine(raiz,"saves","sentinela.txt"),"mundo");
                    try{Instalar(p,raiz);
                        if(!File.Exists(Path.Combine(raiz,"mods","la-craft-"+Versao+".jar"))||!File.Exists(Path.Combine(raiz,"saves","sentinela.txt"))||!Directory.GetDirectories(raiz,"mods-backup-*").Any(x=>File.Exists(Path.Combine(x,"anterior.txt"))))throw new IOException("Teste de preservacao falhou");
                        Console.WriteLine("Instalacao real em pasta de teste e backup aprovados.");return 0;
                    }finally{Directory.Delete(raiz,true);}
                }
                string padrao=@"D:\Backup\.tlauncher\legacy\Minecraft";
                Console.WriteLine("Feche o Minecraft antes de instalar. Requer Fabric 1.20.1 e Java 17.");
                Console.WriteLine("Pasta Minecraft (Enter usa "+padrao+"):");
                string raizUsuario=Console.ReadLine();if(String.IsNullOrWhiteSpace(raizUsuario))raizUsuario=padrao;
                raizUsuario=raizUsuario.Trim().Trim('"');if(Path.GetFileName(raizUsuario).Equals("mods",StringComparison.OrdinalIgnoreCase))raizUsuario=Path.GetDirectoryName(raizUsuario);
                Console.WriteLine("Os mods antigos serao movidos para backup; mundos serao preservados. Confirmar (S/N)?");
                if(!String.Equals(Console.ReadLine(),"S",StringComparison.OrdinalIgnoreCase))return 0;
                Instalar(p,raizUsuario);
                Console.WriteLine("No menu Mods > La Craft > IA, selecione INTEGRADA. O modelo de 1,12 GB baixa no primeiro pedido.");
                Console.WriteLine("Versao experimental. Teste em mundo novo.");Console.ReadLine();return 0;
            }
        }catch(Exception e){Console.Error.WriteLine("ERRO: "+e.Message);if(args.Length==0)Console.ReadLine();return 1;}
    }
}
