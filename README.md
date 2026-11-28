# La Craft — Voz da Criação
Minecraft Java 1.20.1, Fabric, Java 17. Versão experimental 0.7.0.
## Instalação
Baixe o JAR em Actions > execução bem-sucedida > Artifacts. Instale Fabric API 0.92.2+1.20.1 e Simple Voice Chat 2.5.26. Instale os três JARs no cliente e servidor.
## Uso
- G: sugar matéria dos blocos naturais próximos da mira.
- F: gravar até 15 segundos do próprio microfone do Simple Voice Chat. O reconhecimento Vosk em português funciona localmente, sem API paga; o modelo é embutido pelo workflow. Apenas a frase reconhecida é enviada ao servidor para criar o pedido. O áudio é descartado.
- /lacraft casa, /lacraft castelo, /lacraft foguete, /lacraft capivara, /lacraft burros, /lacraft arco, /lacraft armadura, /lacraft torre: pedidos de teste por texto.
- /vozteste: fonte de partículas.
- /lacancelar: cancelar pedido pendente.
Troque a tecla vanilla de mão secundária para outra tecla. Configure o microfone do Simple Voice Chat. Teste em um mundo separado: a matéria remove blocos reais do mundo.
## Limitações
Catálogo procedural padrão; LLM de construções opcional. Reconhecimento local experimental; entidades customizadas, texturas em tempo real e foguete funcional ainda não disponíveis. No modo de ativação por voz do Simple Voice Chat, fale segurando F; no modo push-to-talk, segure também a tecla do chat de voz. A fala pode ser ouvida normalmente pelos outros jogadores pelo Simple Voice Chat. O saldo é persistente; os pedidos pendentes não são. A compilação e os testes no jogo devem ser verificados separadamente.

Modelo: vosk-model-small-pt-0.3 (Apache 2.0), https://alphacephei.com/vosk/models. A primeira utilização extrai o modelo para config/lacraft/voz. Windows/Linux x64 são os alvos; outras plataformas não verificadas.

## IA generativa de estruturas (opcional)
O mod funciona sem serviços externos para os pedidos do catálogo. Para estruturas inéditas, um administrador pode instalar Ollama e um modelo local de texto e iniciar o Minecraft/servidor com a variável de ambiente LACRAFT_OLLAMA_MODEL definida para o nome desse modelo local. O mod usa apenas 127.0.0.1:11434; não configure modelos de nuvem. Sem essa configuração não faz requisições HTTP. Em multiplayer, avise os jogadores antes de ativar: as frases reconhecidas serão processadas pelo serviço local do servidor. A IA retorna caixas JSON que são expandidas em até 1500 voxels de uma paleta validada; o custo é calculado pelo servidor. Não executa scripts ou comandos. O reconhecimento por voz não depende do Ollama.

## Configurações no jogo
Instale Mod Menu 7.2.2 para Fabric 1.20.1. Abra Mods > La Craft > botão de configurações. Há duas páginas (Jogo / IA). Coleta de matéria, sugador e velocidades são regras do servidor; em multiplayer remoto o cliente não pode alterá-las. Servidor dedicado: configure config/lacraft.json na própria instalação. A chave da API fica local em texto simples nesse arquivo (não compartilhe ou envie ao GitHub); a interface mascara o campo. É possível usar OPENAI_API_KEY como variável de ambiente. O provedor OpenAI é opcional, requer autorização explícita de envio de texto e pode cobrar; não transmite áudio. Use com autorização do responsável/titular da conta. Ollama e catálogo não exigem chave. O modelo OpenAI padrão é gpt-4o-mini. Não há promessa de acesso gratuito à API.

## Compatibilidade Simple Voice Chat
A versão do mod de voz inclui o prefixo Minecraft (por exemplo 1.20.1-2.6.22), por isso o Fabric não deve comparar diretamente com 2.5.0. O metadado exige a presença do voicechat e do Minecraft 1.20.1; use uma edição Fabric compatível com Minecraft 1.20.1. A API usada na compilação é 2.5.0; a integração em execução com 2.6.22 ainda precisa ser testada.

Reconhecimento: aceita modelo antigo com final.mdl na raiz e modelo novo com am/final.mdl; converte PCM de 48 kHz para 16 kHz. O teste de compilação carrega o modelo e processa áudio silencioso, mas não comprova precisão de fala nem integração do microfone no Minecraft.

## Sons
Sons espaciais do Minecraft ao absorver matéria (orbe), iniciar construção (farol), montar blocos (ametista), concluir desejo (nível) ou interromper construção (dispensador). Configurações > Jogo > Sons permite desligar os efeitos localmente; volume geral na categoria Jogadores. Os sons são enviados apenas em lotes para evitar excesso de áudio.

## Desejos expandidos
Chat: escreva `quero voar`, `casa`, `quero um piglin gigante`, `quero uma espada mágica`, `quero um laser`, `quero velocidade` ou `quero item minecraft:diamond`. IDs de mods instalados podem ser usados com `quero item mod:item` ou `quero invocar mod:mob`, se existirem nos registros; não há garantia de compatibilidade de cada mod. A opção Chat permite interpretar todas as mensagens (desligada por padrão). O voo dura cinco minutos em sobrevivência; o cetro gera um feixe visual de luz com botão direito. Gigantes têm escala visual experimental, colisão normal. A IA opcional agora interpreta itens, mobs, poderes e estruturas; não cria classes Java inéditas. Transcrição OpenAI opcional na página Chat: envia a gravação à API somente quando também autorizado o envio à nuvem e uma chave está configurada; pode cobrar e requer autorização do responsável/titular. Vosk continua padrão offline.

## Motor de artefatos — primeira etapa
Artefatos registrados com NBT persistente, cor de renderização e até cinco efeitos combináveis (cura, respirar, velocidade, salto, invisibilidade, resistência, levitação breve, brilho, nuvem, música). Experimente: `quero um cristal verde de cura e velocidade`. Clique direito para usar. Não são ainda modelos/texturas inéditos. Confirmação de voz editável ativa por padrão, desligável em Chat. Com LLM configurado, todos os pedidos passam pela IA; falhas não materializam um desejo diferente automaticamente. Sem LLM, o catálogo e o compositor limitado continuam disponíveis.

## La Craft Infinite — pacote experimental
O workflow também resolve os projetos listados em `modpack/projetos.json`, com dependências obrigatórias, versão Minecraft/Fabric, hashes e verificação de conflitos declarados. Importar o `.mrpack` pelo Prism Launcher ou Modrinth App; não redistribuímos JARs de terceiros sem verificar as licenças. A IA recebe uma lista limitada de IDs reais relevantes dos registros de itens, entidades e blocos. Isso não implementa APIs específicas dos mods nem garante combinação de habilidades. O pacote ainda necessita teste de inicialização e multiplayer.

## 0.8.0 experimental
Residências com portas, janelas, cama, baú, bancada, fornalha, iluminação, telhado e opção de dois quartos; quatro estilos/níveis de detalhe. O executor usa estados de blocos explícitos. Ainda requer teste de portas e camas em jogo. Piscina, garagem, múltiplos andares e edição de construções não implementados. Chat aceita pedidos residenciais longos. IA revisa uma resposta inválida uma vez; sem substituição automática por desejo diferente. Voz até 30 s, medidor, normalização e rejeição de silêncio; não é novo modelo de reconhecimento. Menu Extras inclui qualidade, volume e relatório sem dados privados. Testes de planta e áudio sintético não equivalem a teste em jogo.

### 0.8.2 experimental
Ações de horário e clima autorizadas, chuva de itens/mobs com orçamento e cancelamento, voo temporizado, planos de até oito ações sequenciais, residências de até quatro quartos e catálogo com categoria técnica dos itens. Não é um motor universal completo; sem Whisper, modelos originais, tsunami, furacão ou teste em jogo comprovado. Indium obrigatório no pacote; expansão adicional sujeita ao resolvedor.

### 0.8.3 — Whisper local opcional
O menu Chat permite ativar Whisper local. Requer um whisper-server do projeto whisper.cpp instalado separadamente, com modelo multilíngue, idioma pt e escuta somente em 127.0.0.1:8080. Documentação oficial: https://github.com/ggml-org/whisper.cpp/tree/master/examples/server . O mod envia WAV PCM mono 16 kHz ao endpoint local /inference e, se indisponível, usa Vosk com aviso. Não instala o servidor nem o modelo automaticamente; não foi validado com gravações reais. A captura permanece via Simple Voice Chat.

### 0.8.4 experimental — voz
Reparo conservador de texto UTF-8 lido como Windows-1252/Latin-1, microfone direto Java Sound (padrão do Windows, opção na página Chat), indicação de preparação, 250 ms de captura após soltar F e fallback Simple Voice Chat. Sem gravação em disco. Não garante reconhecimento perfeito; testar com microfone real.

### 0.9.0 experimental
Planta modular de um andar com até 12 ambientes, contagens de banheiros e quartos separadas, piscina elevada, cozinha, sala, garagem e jardim; mobiliário de blocos vanilla. Prévia isométrica giratória de amostra dos voxels reais na HUD e contador de blocos. Bloqueio do item Automobility sem componentes, ainda sem adaptador de montagem. Pins de Sodium/Indium e correção do resolvedor. Não é geração 3D universal e não foi validada em jogo.

### 0.9.1 experimental
Automobility 0.4.2: carro com dados de frame, wheels e engine de acordo com AutomobileItem/AutomobilePrefab oficiais, escolha de cor. Fenômenos visuais de onda e redemoinho temporários, sem destruição. Desfazer a última construção da sessão apenas se os blocos continuam intactos e os inventários vazios. Sem garantia de execução em jogo; geração 3D universal ainda não implementada.

### 0.9.2 experimental
Perfil FPS e prévia opcional; minimapas e efeitos pesados removidos do manifesto; Revelationary e Modonomicon obrigatórios; política de spawn seletivo para criaturas especiais em ServerWorld e tentativa de interceptação na geração de chunks. Entidades autorizadas recebem tag persistente. Ollama passa a planejar também residências e aceita arquitetura por caixas com vãos e mobiliário; não executa fallback silencioso quando a IA falha. Ainda não é 1.0.0: não há testes de cliente, geração universal de modelos/texturas/animações ou garantia de interceptar todos os caminhos de spawn de outros mods.

### 0.9.3 experimental
Correções explícitas de transcrição comuns, busca por IDs reais via /lacraftbuscar, resolução direta de poucos itens/mobs simples sem API, Ollama como padrão em novas instalações. Aviso de matéria pendente. Fenômenos com movimento de entidades e partículas limitadas: onda, redemoinho, tremor, gravidade e meteoros, sem dano permanente ao terreno. Perfil retira interfaces opcionais e alguns utilitários redundantes; conteúdo de mundo preservado. Testes reais de FPS, voz e animação continuam pendentes.

### 0.9.4 — auditoria experimental
O pipeline inspeciona os JARs oficiais do manifesto, SHA-512, tamanho, fabric.mod.json, bibliotecas aninhadas, arquivos de configuração de mixins e predicados de versão do Fabric. O pacote expandido só é publicado após a auditoria. A análise estática não executa os mods nem comprova compatibilidade em jogo. HUD e partículas de absorção corrigidas, testes adicionais de arquitetura e transcrição. Não é a versão final.

### 0.9.5 LEVE — experimental
Perfil reduzido: remove projetos opcionais de biomas/dimensões, estruturas, tecnologia e decoração redundante. Preserva La Craft, voz, Automobility, Immersive Aircraft, Mythic Mounts, Mutant Monsters, chefes, RPG Series, Simply Swords, Handcrafted, Macaw's Furniture, otimizações e dependências fixadas. A remoção de conteúdo pode afetar mundos antigos: faça uma cópia integral do mundo antes de abrir e prefira um mundo novo. Mods de terceiros ainda podem adicionar alguma geração própria. A redução de FPS não foi medida. Projetos retirados: betterend, betternether, terralith, yungs-better-dungeons, yungs-better-strongholds, yungs-better-mineshafts, ad-astra, create-fabric, techreborn, naturalist, fabric-seasons, botania, bewitchment, spectrum, hex-casting, origins, toms-storage, travelersbackpack, waystones, chipped, macaws-doors, macaws-windows, macaws-roofs, macaws-bridges, decorative-blocks, adorn, another-furniture, soulslike-weaponry, enhanced-block-entities, worldedit.

Auditoria 0.9.4 detectou requisitos incompatíveis de Fabric Loader em BetterEnd (metadados inválidos), Create, Tech Reborn, Friends & Foes, Kotlin e Ranged Weapon API. O perfil leve remove os primeiros conteúdos e fixa versões anteriores dos três últimos candidatos para manter o Loader 0.16.10, sujeitas à auditoria. A auditoria do perfil leve cobre o cliente; servidor dedicado não foi validado.

### 0.9.6 — modelos declarativos experimentais
Planos originais de até 24 peças BlockDisplay com materiais e movimentos, incluindo animação decorativa persistente por tags. /lalimparmodelos remove peças próprias carregadas nas proximidades. Validação de intenção para macaco, ônibus e construções; consulta de IDs do catálogo; nova tentativa inclui motivo de rejeição; altura arquitetônica até 48, orçamento 16000 blocos. Não são mobs funcionais nem veículos dirigíveis, não há geração de texturas próprias. Ainda não testado dentro do cliente Minecraft.

Correção adicional 0.9.6: pedidos simples de cajado/cetro/varinha consultam primeiro itens reais registrados de terceiros, priorizando Wizards e excluindo partes e blocos. Sem correspondência, informa a ausência em vez de fabricar artefato genérico. Pedidos complexos continuam no planejador; habilidades específicas ainda não verificadas automaticamente.

### 0.9.7 experimental — artefatos com visual generativo
O planejador pode definir até 16 cuboides, cores independentes, mapa de pixels 8x8 e animação por peça. O cliente usa renderer dinâmico, texturas geradas em cache limitado, e NBT persiste o projeto. Pedidos originais rejeitam artefatos sem visual na validação principal; itens existentes conservam seus recursos. Novas magias de repulsão e atração de itens e efeitos de status. Não há geração ilimitada de comportamentos ou malhas arbitrárias, nem teste visual dentro do Minecraft. Texturas com cache cheio usam material de fallback; recarga ainda compartilhada pelo tipo de item. 

### 0.9.8 experimental — catálogo e eventos combináveis
Índice de recursos registrados com traduções pt_br/en_us dos mods e busca por nome exato e termos. Não é um índice de embeddings; não infere poderes de terceiros. Chuva mista declarativa com até 8 grupos/32 aparições alternadas, e fila espera evento ativo. Verificação de marcadores de quartos, banheiros, porta dupla, vidro preto e escadas; não prova a planta inteira nem acessibilidade. Ollama segue externo. Motor de veículos e criaturas universais, texturas avançadas e efeitos de terreno ainda pendentes. Sem teste em cliente.

### 0.9.9 experimental
O planejador pode solicitar até duas pesquisas adicionais no índice imutável de recursos registrados, por termos escolhidos pela IA. Resultados são validados por categoria. Limite de desejo por voz/comando ampliado para 768 caracteres; chat vanilla continua limitado pelo Minecraft. Validação de pares de camas e portas; tentativa de uso de blocos decorativos simples dos mods de móveis com validação de registro e restrições. Contexto Ollama de 6144 tokens pode aumentar consumo de RAM. Não é versão 1.0.0; falta teste visual, modelos vivos e veículos inéditos funcionais, arquitetura completa e IA embutida. 

### 0.9.10 experimental
Entidade procedural própria, textura e animações por peça, dados persistentes, caminhada e montaria terrestre experimental. Colisão e tamanho ainda fixos, sem voo, física de rodas ou IA complexa. Novo workflow de GameTest no Windows em servidor de desenvolvimento com Fabric e Voice Chat; não representa teste gráfico, do modpack completo ou do executável. Não é 1.0.0.

### 0.9.11 experimental — projeto por ambientes
Novo formato planta2: a IA escolhe retângulos de ambientes, funções e ligações; o motor rejeita interiores sobrepostos, ambientes desconectados, piso superior sem apoio e suítes ausentes quando solicitadas. Até dois andares, escada entre halls alinhados, portas orientadas, entrada dupla e janelas pretas. Piscinas e jardins vinculados ao projeto. Ainda não verifica navegação física completa, decoração realista ou qualidade visual; não é a versão final. Mantém o GameTest de servidor Windows e os testes anteriores.

### 0.9.12 experimental
Início automático do serviço Ollama já instalado na pasta oficial padrão do Windows, apenas loopback; sem instalação ou download automático de modelos. Entidades/veículos terrestres com dimensões, velocidade e geometria declarativas persistentes, textura de 8x8 ou 16x16 e animação de roda. Colisão horizontal quadrada conservadora, sem suspensão ou voo; não equivale a veículo completo. Novo GameTest de persistência e dimensões. Ainda exige teste gráfico e instalação real. Não é 1.0.0.

### 0.9.13 experimental
Integração JNI java-llama.cpp 4.2.0 com Qwen2.5 1.5B Instruct Q4_K_M (Apache-2.0, download inicial 1,12 GB, SHA-256 fixo, CPU), modo INTEGRADA para instalações novas. Usuários antigos podem selecionar INTEGRADA no menu. Nenhum Ollama ou API obrigatório nesse modo. Ainda falta testar inferência real no Windows. Fenômenos avançados com detritos temporários e onda/vórtice de BlockDisplay, sem alteração permanente de terreno; destruição real não implementada. Teste gráfico smoke em Linux/Xvfb captura tela de configuração, não é teste completo do modpack ou do hardware do usuário. Não é 1.0.0.
