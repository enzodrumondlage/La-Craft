# La Craft — Voz da Criação
Minecraft Java 1.20.1, Fabric, Java 17. Versão experimental 0.5.3.
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
