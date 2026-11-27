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
