# CLAUDE.md — Blue ON

Memória permanente do projeto. Qualquer sessão que arranque neste repositório
deve ler este ficheiro e ter em conta o contexto, as convenções técnicas e o
ecossistema de skills descritos abaixo — tanto nas tarefas em curso como nas
novas que surjam, e sobretudo ao **fabricar conteúdo** (treino, nutrição,
marketing, redes, trabalhos, análises).

Idioma de trabalho: **Português (PT-PT)**. O dono do projeto é o **João**
(Bluefitness Club · Leiria).

---

## 1. O que é o Blue ON

**Blue ON** é uma **PWA** (Progressive Web App) de **treino, nutrição e
acompanhamento personalizado** para os atletas do Bluefitness. É uma app
estática, sem passo de build, servida diretamente como ficheiros.

- App real e canónica: **`blueonapp.html`** (single-file, ~5900 linhas: HTML +
  CSS + JS inline).
- `index.html` → apenas um redirect para `blueonapp.html`.
- Backend: **Firebase** (Auth + Firestore) para dados dos atletas.
- Integrações: **Strava** (via `WEARABLES_API`, wearables/atividades),
  **TheMealDB** (imagens de ingredientes), **WhatsApp** (`wa.me`).
- PWA: `manifest.json` + `sw.js` (service worker), instalável, com tema
  claro/escuro ("Jarvis blue").

### Ficheiros versionados
```
blueonapp.html        # A app (única fonte de verdade)
index.html            # Redirect → blueonapp.html
manifest.json         # PWA manifest
sw.js                 # Service worker
README.md
apple-touch-icon.png icon-192.png icon-512.png icon-maskable-512.png
```

> **Nota histórica:** existiu um `blue-on-app.html` (com hífens) que era uma
> versão antiga e órfã. Foi removido — o `blueonapp.html` é a sua evolução e
> contém todas as funções e IDs desse ficheiro. **Não recriar** o ficheiro
> antigo nem duplicar a app.

---

## 2. Sistemas internos da app: JARVIS e HULK

O Blue ON não é só um catálogo de treinos — tem uma camada de **inteligência de
coaching**. Ao mexer na app ou ao produzir conteúdo para ela, respeitar estes
sistemas e a sua linguagem.

### JARVIS — coaching proativo (HUD)
Funções-chave em `blueonapp.html`: `jarvisProactive()`, `jarvisBriefingText()`,
`jarvisSpeak()`, componente `JarvisCard`, estilos `.jarvis` / `.jv-*`.

O JARVIS lê o estado do atleta e gera mensagens acionáveis:
- **Tendência de peso** (últimas 2-3 avaliações) cruzada com o objetivo
  (emagrecer vs. hipertrofia) → sugere ajuste calórico e de proteína.
- **BPI** (Blue Performance Index) — série de score; alerta se sobe/desce.
- **Check-in** (sono, dor) → ajusta volume, recuperação, evita impacto.
- **Adesão** (sessões feitas nos últimos 14 dias vs. plano).

Tom do JARVIS: direto, técnico, motivador, orientado à ação. Emojis
funcionais (⚖️ 🔥 💪 📈 📉 😴 🩹) e `tone` (`ok` / `warn` / `danger`).

### HULK — camada de comando
"HULK" é o sistema de comando/força do ecossistema do João (par do JARVIS).
Quando o João pedir para "os dois trabalharem para o mesmo" (simbiose), o
objetivo é que a app (Blue ON) e a camada de comando (HULK/JARVIS) partilhem o
mesmo contexto e produzam conteúdo coerente e alinhado.

---

## 3. Convenções técnicas (não quebrar)

- **Single-file:** todo o CSS e JS vivem inline em `blueonapp.html`. Não
  extrair para ficheiros separados sem pedido explícito.
- **Tema:** aplicado cedo via `data-theme` (`auto`/`light`/`dark`) lido de
  `localStorage['blueon_theme']` para evitar flash. Variáveis CSS em `:root`.
- **Service worker (`sw.js`):**
  - Sempre que se altera o app shell, fazer **bump da `VERSION`**
    (`blueon-vX.Y.Z-...`) para forçar atualização nos dispositivos.
  - `IMG_HOSTS` = allowlist de hosts de imagem cacheados. Manter alinhada com
    o que a app **realmente usa** (atualmente só `themealdb.com`). Não deixar
    hosts mortos.
  - Navegação: network-first com fallback ao cache. Assets: cache-first.
- **Firebase:** a `apiKey` no cliente é pública por design (não é segredo). A
  segurança depende **100% das Firestore Security Rules** — nunca assumir que o
  cliente é confiável.
- **XSS:** há muitas atribuições `innerHTML =`. Qualquer conteúdo vindo do
  utilizador/Firestore deve ser tratado/escapado antes de ir para `innerHTML`.

### Deploy
- **Netlify** = deploy canónico e funcional (deploy previews por PR). ✅
- **Cloudflare Workers** — servido como **static assets** via `wrangler.toml`
  (`[assets] directory = "."`, sem script `main`). A app é 100% estática, por
  isso não há entry-point de Worker. Se o deploy voltar a falhar, verificar no
  dashboard Cloudflare se o projeto tem um *build command*/*root directory*
  custom que entre em conflito com esta config.

---

## 4. Ecossistema de skills do João (usar ao fabricar conteúdo)

O João tem skills personalizadas. Nas tarefas de conteúdo, **considerar e
ativar a skill certa automaticamente** (não fazer conteúdo genérico):

| Skill | Quando usar |
|-------|-------------|
| **bluefitness-plano-treino** | Criar/rever/otimizar planos de treino (alunos, BullFitness pessoal, Blue ON). Biomecânica, hipertrofia, emagrecimento, populações especiais, saúde e segurança. **Nunca planos genéricos.** |
| **conselho-estrategico-seis-vozes** | Decisões e conteúdo estratégico: marketing, branding, vendas, modelos de negócio, funis, automações IA, dashboards, HULK/JARVIS. 6 vozes (Marcos, Carol, Ricardo, Diana, Pedro, Luana). Em tarefas complexas mostrar as vozes; termina com decisão + prioridade + próximos passos + riscos + métrica. |
| **tori-trades** | Trading técnico ativo (linhas de tendência, breakout, stop-loss). Curto prazo. Nunca inventar preços; marcar incerteza. |
| **investimentos-dalio** | Investimento de **longo prazo** (Ray Dalio): carteira, alocação, rebalanceamento, gestão de risco. Não é trading. |
| **revisao-academica-apa** | Rever trabalhos académicos contra matriz/enunciado, APA 6/7, Mendeley. Nunca inventar fontes/DOI. |

Skills genéricas de marketing/conteúdo também aplicáveis ao Blue ON:
`content-strategy`, `copywriting`, `social`, `cro`, `marketing-psychology`,
`customer-research`, `pricing`.

### Regra de ouro do conteúdo
Ao produzir qualquer conteúdo para o João:
1. Identificar o domínio (treino / estratégia / trading / investimento /
   académico / marketing) e **ativar a skill correspondente**.
2. Manter a **voz Blue ON / JARVIS**: técnica, direta, acionável, PT-PT.
3. Ancorar no contexto real (Bluefitness Leiria, atletas, objetivos) — evitar
   generalidades.
4. Nunca inventar dados, fontes, preços ou métricas; marcar incerteza.

---

## 5. Git & fluxo

- Desenvolver em branches (nunca push direto para `main` sem permissão).
- Ao alterar o app shell, lembrar do bump de versão do service worker.
- Após push, abrir PR draft; a CI relevante é o **Netlify preview** (a falha do
  Cloudflare Workers é pré-existente e de infraestrutura).
