# La Craft — Voz da Criação
Minecraft Java 1.20.1, Fabric, Java 17. Versão experimental 0.3.0.
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
Catálogo procedural, não LLM generativo. Reconhecimento local experimental; entidades customizadas, texturas em tempo real e foguete funcional ainda não disponíveis. No modo de ativação por voz do Simple Voice Chat, fale segurando F; no modo push-to-talk, segure também a tecla do chat de voz. A fala pode ser ouvida normalmente pelos outros jogadores pelo Simple Voice Chat. O saldo é persistente; os pedidos pendentes não são. A compilação e os testes no jogo devem ser verificados separadamente.

Modelo: vosk-model-small-pt-0.3 (Apache 2.0), https://alphacephei.com/vosk/models. A primeira utilização extrai o modelo para config/lacraft/voz. Windows/Linux x64 são os alvos; outras plataformas não verificadas.
