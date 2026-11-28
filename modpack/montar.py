"""Gera manifesto Modrinth e ZIP com JAR próprio; terceiros são baixados de suas fontes."""
import json, hashlib, pathlib, sys, urllib.request, urllib.parse, zipfile, time
ROOT=pathlib.Path(__file__).resolve().parent
CFG=json.loads((ROOT/"projetos.json").read_text(encoding="utf-8"))
MC=CFG["minecraft"]
API="https://api.modrinth.com/v2/"
CACHE={}
def get(path):
    if path not in CACHE:
        for attempt in range(3):
            try:
                req=urllib.request.Request(API+path,headers={"User-Agent":"LaCraftInfinite/0.9.7 (GitHub modpack builder)"})
                with urllib.request.urlopen(req,timeout=45) as r: CACHE[path]=json.load(r)
                break
            except (OSError,ValueError):
                if attempt==2: raise
                time.sleep(2*(attempt+1))
    return CACHE[path]
def compatible(v): return MC in v["game_versions"] and "fabric" in v["loaders"]
def versions(project):
    q=urllib.parse.urlencode({"loaders":json.dumps(["fabric"]),"game_versions":json.dumps([MC])})
    return [v for v in get("project/"+project+"/version?"+q) if compatible(v)]
chosen={}
# Bibliotecas escolhidas explicitamente devido às restrições observadas no Fabric Loader.
pins={get('project/'+slug)['id']:vid for slug,vid in CFG.get('pins',{}).items()}
exact={}
visiting=set()
def select(project,version=None):
    p=get("project/"+project)
    pid=p["id"]
    if pid in pins:
        if version and version!=pins[pid]: print("Ajuste de biblioteca:",p["slug"],version,"->",pins[pid])
        version=pins[pid]
    if version:
        v=get("version/"+version)
        if v["project_id"]!=pid or not compatible(v): raise ValueError("Dependência incompatível: "+pid+" "+version)
        if pid in exact and exact[pid]!=version: raise ValueError("Conflito de versões fixas: "+pid)
        exact[pid]=version
    elif pid in exact:
        v=get("version/"+exact[pid])
    elif pid in chosen: return
    else:
        vs=versions(pid)
        if not vs: raise ValueError("Sem versão Fabric "+MC+": "+p["slug"])
        v=next((x for x in vs if x["version_type"]=="release"),vs[0])
    if pid in chosen and chosen[pid][1]["id"]==v["id"]: return
    if pid in visiting: raise ValueError("Dependência circular com versões conflitantes")
    visiting.add(pid)
    chosen[pid]=(p,v)
    try:
        for dep in v["dependencies"]:
            if dep["dependency_type"]!="required": continue
            vid=dep.get("version_id")
            target=dep.get("project_id") or (get("version/"+vid)["project_id"] if vid else None)
            if not target: raise ValueError("Dependência externa sem projeto: "+str(dep))
            select(target,vid)
    finally: visiting.remove(pid)
for slug in CFG["projetos"]: select(slug)
def validar():
    # Revalidação após resolução; não publicar se houver conflito.
    for p,v in chosen.values():
        for dep in v["dependencies"]:
            vid=dep.get("version_id")
            target=dep.get("project_id") or (get("version/"+vid)["project_id"] if vid else None)
            if dep["dependency_type"]=="required":
                if target not in chosen or (vid and target not in pins and chosen[target][1]["id"]!=vid): raise ValueError("Dependência não satisfeita: "+p["slug"])
            if dep["dependency_type"]=="incompatible" and target in chosen and (not vid or chosen[target][1]["id"]==vid):
                raise ValueError("Conflito declarado: "+p["slug"]+" / "+target)

validar()
expansao=[]
for slug in CFG.get('expansao',[]):
    saved=chosen.copy(); saved_exact=exact.copy()
    try:
        select(slug)
        validar()
        for p,v in chosen.values():
            if not any(x['filename'].endswith('.jar') for x in v['files']): raise ValueError('JAR ausente: '+p['slug'])
        expansao.append({'slug':slug,'status':'incluido'})
    except Exception as e:
        chosen.clear(); chosen.update(saved); exact.clear(); exact.update(saved_exact); visiting.clear()
        expansao.append({'slug':slug,'status':'omitido','motivo':str(e)})
        print('OMITIDO:',slug,str(e))
validar()
files=[]; lock=[]; paths=set()
for p,v in sorted(chosen.values(),key=lambda a:a[0]["slug"]):
    jars=[x for x in v["files"] if x["filename"].endswith(".jar")]
    if not jars: raise ValueError("JAR ausente: "+p["slug"])
    a=next((x for x in jars if x["primary"]),jars[0])
    path="mods/"+a["filename"]
    if pathlib.PurePosixPath(a["filename"]).name!=a["filename"] or path in paths: raise ValueError("Nome duplicado ou inválido")
    paths.add(path)
    if not a["url"].startswith("https://cdn.modrinth.com/"): raise ValueError("Origem inesperada")
    env={side:("unsupported" if p.get(side+"_side")=="unsupported" else "required") for side in ("client","server")}
    files.append({"path":path,"hashes":{"sha1":a["hashes"]["sha1"],"sha512":a["hashes"]["sha512"]},"env":env,"downloads":[a["url"]],"fileSize":a["size"]})
    lock.append({"slug":p["slug"],"version":v["version_number"],"version_id":v["id"],"license":p.get("license",{}).get("id"),"file":a["filename"],"sha512":a["hashes"]["sha512"]})
    print(p["slug"],v["version_number"])
jar=pathlib.Path(sys.argv[1])
assert jar.is_file()
out=pathlib.Path("dist");out.mkdir(exist_ok=True)
index={"formatVersion":1,"game":"minecraft","versionId":"0.9.7-leve","name":"La Craft Infinite - TESTE","summary":"Pacote experimental; compatibilidade em jogo ainda não comprovada.","files":files,"dependencies":{"minecraft":MC,"fabric-loader":CFG["loader"]}}
mrpack=out/"La-Craft-Infinite-0.9.7-LEVE.mrpack"
with zipfile.ZipFile(mrpack,"w",zipfile.ZIP_DEFLATED) as z:
    z.writestr("modrinth.index.json",json.dumps(index,ensure_ascii=False,indent=2))
    z.write(jar,"overrides/mods/"+jar.name)
with zipfile.ZipFile(out/"La-Craft-Infinite-0.9.7-LEVE.zip","w",zipfile.ZIP_DEFLATED) as z:
    z.write(mrpack,mrpack.name)
    z.writestr("expansao.json",json.dumps(expansao,ensure_ascii=False,indent=2))
    z.writestr("mods-lock.json",json.dumps(lock,ensure_ascii=False,indent=2))
    z.writestr("LEIA-ME.txt","Importe o arquivo .mrpack no Prism Launcher ou Modrinth App. O launcher baixa os mods de suas fontes oficiais. Não extraia este ZIP diretamente em mods: ele contém um manifesto, não todos os JARs. Pacote experimental, não é 1.0.0. Use Java 17 e teste em um mundo novo. Não foi validado em jogo.")
print("Manifesto gerado:",len(files),"mods e dependências.")
