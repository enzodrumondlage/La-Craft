"""Auditoria estática dos JARs reais, incluindo bibliotecas aninhadas; não substitui teste no jogo."""
import concurrent.futures, hashlib, io, json, pathlib, subprocess, sys, tempfile, urllib.request, zipfile
ROOT=pathlib.Path(__file__).resolve().parent
cfg=json.loads((ROOT/"projetos.json").read_text(encoding="utf-8"))
pack=pathlib.Path(sys.argv[1])
out=pathlib.Path("dist");out.mkdir(exist_ok=True)
def fetch(url,hash512=None,size=None):
    if not url.startswith(("https://cdn.modrinth.com/","https://maven.fabricmc.net/")): raise ValueError("Origem não autorizada: "+url)
    req=urllib.request.Request(url,headers={"User-Agent":"LaCraft-Audit/1.0"})
    with urllib.request.urlopen(req,timeout=90) as r: data=r.read((size+1) if size is not None else 32_000_001)
    if size is not None and len(data)!=size: raise ValueError("Tamanho incorreto: "+url)
    if hash512 and hashlib.sha512(data).hexdigest()!=hash512: raise ValueError("SHA-512 incorreto: "+url)
    return data
def inspect(data,source,root=True,depth=0):
    if depth>8: raise ValueError("JAR aninhado demais: "+source)
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        names=set(z.namelist())
        if "fabric.mod.json" not in names:
            if root: raise ValueError("fabric.mod.json ausente: "+source)
            return []
        m=json.loads(z.read("fabric.mod.json"))
        result=[{"source":source,"root":root,"id":m["id"],"version":m["version"],"environment":m.get("environment","*"),"provides":m.get("provides",[]),"depends":m.get("depends",{}),"breaks":m.get("breaks",{})}]
        for child in m.get("jars",[]):
            path=child["file"]
            if path not in names: raise ValueError("JAR interno ausente: "+source+"/"+path)
            result.extend(inspect(z.read(path),source+"!"+path,False,depth+1))
        for mixin in m.get("mixins",[]):
            path=mixin if isinstance(mixin,str) else mixin["config"]
            if path not in names: raise ValueError("Mixin ausente: "+source+"/"+path)
        return result
with zipfile.ZipFile(pack) as z:
    index=json.loads(z.read("modrinth.index.json"))
    own=[(n,z.read(n)) for n in z.namelist() if n.startswith("overrides/mods/") and n.endswith(".jar")]
    if len(own)!=1: raise ValueError("Esperado exatamente um JAR próprio")
def one(f):
    return inspect(fetch(f["downloads"][0],f["hashes"]["sha512"],f["fileSize"]),f["path"])
allmods=[]; failures=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
    futures={pool.submit(one,f):f["path"] for f in index["files"]}
    for future in concurrent.futures.as_completed(futures):
        try:
            allmods.extend(future.result()); print("JAR verificado:",futures[future],flush=True)
        except Exception as exc: failures.append(futures[future]+": "+str(exc))
for n,data in own: allmods.extend(inspect(data,n))
report={"manifest_entries":len(index["files"]),"root_jars":sum(m["root"] for m in allmods),"metadata":allmods,"errors":failures,"limitations":"Auditoria estática; não testa mixins em execução, FPS, renderização ou lógica dos mods."}
rows=[]
for side in ("client","server"):
    active=[m for m in allmods if m["environment"] in ("*",side)]
    byid={}
    for m in active:
        for id in [m["id"]]+m["provides"]: byid.setdefault(id,[]).append(m["version"])
    byid.update({"minecraft":[cfg["minecraft"]],"java":["17"],"fabricloader":[cfg["loader"]]})
    roots={}
    for m in active:
        if m["root"]: roots.setdefault(m["id"],[]).append(m["source"])
    for id,sources in roots.items():
        if len(sources)>1: failures.append(side+" ID duplicado "+id+": "+str(sources))
    for m in active:
        for kind in ("depends","breaks"):
            for id,rule in m[kind].items():
                rules=rule if isinstance(rule,list) else [rule]
                rows.append("\t".join([side+":"+m["id"],id,kind,"|".join(rules),"|".join(sorted(set(byid.get(id,[]))))]))
    if side=="client":
        for id in ("structure_pool_api","revelationary","modonomicon","sodium","indium","fabric-api","porting_lib"):
            if id not in byid: failures.append("Biblioteca obrigatória ausente: "+id)
with tempfile.TemporaryDirectory() as tmp:
    tmp=pathlib.Path(tmp)
    inp=tmp/"predicados.tsv";inp.write_text("\n".join(rows),encoding="utf-8")
    loader=tmp/"fabric-loader.jar";loader.write_bytes(fetch("https://maven.fabricmc.net/net/fabricmc/fabric-loader/"+cfg["loader"]+"/fabric-loader-"+cfg["loader"]+".jar"))
    result=subprocess.run(["java","-cp",str(loader),str(ROOT/"AuditarVersao.java"),str(inp)],capture_output=True,text=True)
    print(result.stdout);print(result.stderr,file=sys.stderr)
    report["predicados"]=result.stdout.splitlines();report["predicate_exit_code"]=result.returncode
    if result.returncode: failures.append("Falha na validação de predicados do Fabric Loader")
(out/"auditoria-jars.json").write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding="utf-8")
print("Auditoria:",report["root_jars"],"JARs principais;",len(allmods),"metadados incluindo internos.")
if failures:
    for error in failures: print("ERRO:",error,file=sys.stderr)
    sys.exit(1)
