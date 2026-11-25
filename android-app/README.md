# 📱 Saúde do Aparelho (Android)

App Android nativo (Kotlin + Jetpack Compose) que **identifica causas de travamento e de drenagem de bateria, avalia a saúde do aparelho e aplica correções** para otimizar o funcionamento.

Android 8.0 (API 26) ou superior.

## Funções

| Aba | O que faz |
|-----|-----------|
| **Início** | Nota de saúde de 0 a 100, resumo (bateria, temperatura, RAM, espaço), lista de problemas encontrados com botão de correção para cada um |
| **Bateria** | Saúde informada pelo sistema, capacidade estimada em relação à de fábrica, ciclos (Android 14+), temperatura, corrente, **velocidade real de descarga em %/h** (geral e com a tela apagada), apps que mais rodaram (na tela e em segundo plano) e apps liberados da otimização de bateria |
| **Desempenho** | RAM, armazenamento, temperatura e limitação térmica (*throttling*), tempo ligado sem reiniciar, CPU, animações, **histórico de travamentos e "app não responde" (ANR) por app** |
| **Apps** | Cache por app (com limpeza), apps sem uso há mais de 30 dias (com desinstalação) e apps mais usados |
| **Otimizar** | Otimização em 1 toque (com opções), atalhos de correção rápida, situação das permissões e recursos avançados |

### O que o diagnóstico detecta
- Bateria degradada, quente, com descarga rápida ou com consumo alto em repouso
- Apps rodando horas em segundo plano; excesso de apps sem restrição de bateria
- Tempo de tela longo, brilho alto manual, tema claro (em telas OLED), busca de Wi-Fi sempre ativa
- RAM esgotada, aquecimento ou limitação térmica, muitos dias sem reiniciar
- "Não manter atividades" ativado (causa de lentidão)
- Armazenamento quase cheio, cache acumulado, apps esquecidos
- Apps que travam com frequência e falhas do próprio sistema
- Atualização de segurança antiga

### Correções aplicadas
- **Automáticas:** encerrar processos em segundo plano, limpar arquivos temporários do próprio app, tempo de tela de 30s e brilho automático (com a permissão "Modificar configurações do sistema"), animações em 0,5x e desligar "Não manter atividades" (com a permissão via ADB).
- **Assistidas:** o app abre a tela exata do sistema: limpeza de cache de todos os apps (Android 11+), economia de bateria, otimização de bateria por app, armazenamento, tema escuro, atualização do sistema, detalhes do app e desinstalação.

> O Android não deixa um app comum mudar qualquer configuração nem fechar outros apps à força. Por isso, quando não é possível aplicar uma correção sozinho, o app leva você direto à tela certa do sistema. Nada é apagado ou desinstalado sem a sua confirmação.

## Como obter o APK

1. No GitHub, abra **Actions → Android APK** e entre na execução mais recente da branch
2. Baixe o artefato **SaudeDoAparelho-apk** (é um .zip)
3. Copie `app-release.apk` para o celular e instale (permita "instalar apps desconhecidos")

Ou compile localmente com o Android Studio (abra a pasta `android-app`) ou com `./gradlew assembleRelease`.

## Permissões

| Permissão | Para quê | Como conceder |
|-----------|----------|---------------|
| Acesso ao uso | Consumo por app, cache, apps sem uso | Tela de permissão (o app abre por você) |
| Modificar configurações do sistema | Tempo de tela e brilho | Tela de permissão |
| Gerenciar todos os arquivos | Exigida pelo Android só para o assistente de limpeza de cache | Tela de permissão |
| `WRITE_SECURE_SETTINGS` e `READ_LOGS` (opcionais) | Animações, "Não manter atividades" e histórico de travamentos | Uma única vez via ADB (abaixo) |

```bash
adb shell pm grant com.saito.saudeaparelho android.permission.WRITE_SECURE_SETTINGS
adb shell pm grant com.saito.saudeaparelho android.permission.READ_LOGS
```

## Limitações
- A capacidade da bateria é uma **estimativa** (contador de carga ÷ nível) e nem todo fabricante expõe a capacidade de fábrica.
- O Android não informa a terceiros o consumo exato de bateria por app; o app usa o tempo na tela e em segundo plano, que é o melhor indicador disponível.
- A taxa de descarga precisa de algumas horas de uso fora do carregador para aparecer (amostragem a cada ~15 min).
- Por usar `QUERY_ALL_PACKAGES`, `MANAGE_EXTERNAL_STORAGE` e outras permissões especiais, o app foi pensado para instalação direta, não para a Play Store.
