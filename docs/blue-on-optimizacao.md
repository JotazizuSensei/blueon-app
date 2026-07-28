# Blue ON · Plano de Otimização da App

> Diagnóstico técnico e plano de melhoria da PWA Blue ON (`blueonapp.html`).
> Documento vivo — usado como backoffice/handoff para continuar o trabalho.
> Última atualização: 2026-07-28.

## Contexto da app

- **Entry point real:** `blueonapp.html` (~377 KB, ficheiro único, ~5886 linhas).
  Referenciado por `index.html` (redirect), `sw.js` (core assets) e `manifest.json`.
- **Estrutura interna:** CSS (linhas 18–619), JS principal (linhas 856–5884), Firebase via CDN.
- **Backend:** Firebase (Auth anónima + Firestore), SDK modular 10.12.0 carregado de `gstatic`.
- **Estado:** guardado em `localStorage` (`LS_KEY`) + sincronizado para Firestore.
- **PWA:** service worker `sw.js`, versão atual do cache `blueon-v3.6.1-app-publica-sem-codinome`.

## Diagnóstico técnico

| # | Achado | Impacto | Risco de mexer |
|---|---|---|---|
| 1 | Firebase SDK carregado do `gstatic` a cada arranque; **não** é cacheado pelo service worker (o SW faz `return` cedo para pedidos cross-origin não-imagem) | Cold-start mais lento; camada de sync não inicia offline | Médio |
| 2 | **Um único documento Firestore partilhado** (`pwa_state/main`) para todos os utilizadores anónimos → cada dispositivo escreve o *estado global inteiro*; `onSnapshot` propaga a todos; last-write-wins com debounce 800 ms | Dois gestores a editar em simultâneo podem sobrepor-se | Alto (arquitetural) |
| 3 | Versão do SW é **manual** (`blueon-v3.6.1`); se o HTML mudar sem bump, utilizadores podem ficar com shell em cache | Deploys "presos" em versão antiga | Baixo |
| 4 | 377 KB num só ficheiro, **sem minificação** | Parsing/transferência maiores | Baixo, mas mexe em tudo |
| 5 | **37 usos de `innerHTML`** com dados de utilizador | Superfície de XSS (baixa — dados próprios do ginásio) | Médio |

**Notas importantes:**
- A API key do Firebase **não é uma fuga de segurança** — web API keys do Firebase são públicas por design. A proteção real faz-se com Firestore Security Rules.
- Os "21 TODOs" detetados numa primeira passagem eram **falsos positivos** (a palavra portuguesa "todos"/"método"). O código **não** tem backlog de dívida técnica marcada.

---

## Plano por eixo (os 4 escolhidos)

### Eixo 1 — Performance & Offline `[✅ IMPLEMENTADO]`

Três mudanças concretas e de baixo risco:

**1.1 — Service worker passa a cachear o SDK do Firebase**
As URLs `https://www.gstatic.com/firebasejs/10.12.0/firebase-{app,auth,firestore}.js` são
versionadas e imutáveis → estratégia **cache-first** é segura e permanente.
`gstatic` envia `Access-Control-Allow-Origin: *`, portanto a resposta é cacheável (não-opaca).

No `sw.js`, no handler `fetch`, antes do `return` para cross-origin, adicionar:
```js
// SDK do Firebase (URLs versionadas, imutáveis) → cache-first permanente
if (url.hostname === 'www.gstatic.com' && url.pathname.includes('/firebasejs/')) {
  event.respondWith(
    caches.match(request).then((cached) => cached || fetch(request).then((response) => {
      if (response && response.status === 200) {
        const copy = response.clone();
        caches.open(RUNTIME_CACHE).then((c) => c.put(request, copy));
      }
      return response;
    }))
  );
  return;
}
```

**1.2 — Persistência offline do Firestore (IndexedDB)**
Em `firebaseBoot()` (`blueonapp.html` ~linha 5700), trocar `fs.getFirestore(app)` por
`initializeFirestore` com `persistentLocalCache`, com fallback defensivo:
```js
let db;
try {
  db = fs.initializeFirestore(app, {
    localCache: fs.persistentLocalCache({ tabManager: fs.persistentMultipleTabManager() })
  });
} catch (_) {
  db = fs.getFirestore(app);
}
```
Ganho: leitura de dados em cache offline + escritas enfileiradas para sincronizar ao voltar a rede.

**1.3 — Bump da versão do SW**
Sempre que `sw.js` ou o shell mudarem, subir `VERSION` (ex.: `blueon-v3.7.0-...`) para
invalidar caches antigos de forma limpa.

---

### Eixo 2 — Fiabilidade de dados / Sync `[ARQUITETURAL · REQUER DECISÃO]`

O documento partilhado `pwa_state/main` funciona para um modelo "um ginásio, uma fonte de
verdade gerida pelo gestor", mas tem risco de sobreposição (last-write-wins).

Opções (a decidir antes de implementar — **não mexer sem migração**):
- **A) Manter partilhado, mas mais robusto:** merge por campos em vez de escrever o blob
  inteiro; timestamps por atleta; deteção de conflito.
- **B) Documento por atleta:** `pwa_state/{atletaId}` + um doc de índice do gestor. Escala
  melhor, isola escritas, mas exige **migração** dos dados atuais e reescrita da camada de sync.
- **C) Regras de segurança Firestore:** hoje a auth é anónima e (presumivelmente) o acesso é
  aberto. Rever as Security Rules para limitar leitura/escrita.

> ⚠️ Qualquer mudança aqui pode afetar dados reais de atletas já em produção. Precisa de
> plano de migração e backup antes de tocar.

---

### Eixo 3 — Qualidade & Segurança do código `[HIGIENE · SUBSET SEGURO]`

- **`innerHTML` (37 usos):** auditar quais recebem texto livre do utilizador; nesses,
  passar a `textContent` ou sanitizar. Fazer em incrementos pequenos e testáveis — **não**
  um refactor massivo de uma vez numa app em produção.
- **Guardas de erro:** já existem 14 `try/catch`; garantir que caminhos de rede
  (Firestore, fetch de receitas/OFF) degradam com toast em vez de falhar em silêncio.
- **Organização:** o JS é um bloco de ~5000 linhas; a prazo, considerar dividir em módulos,
  mas isso é grande e deve vir depois dos ganhos de baixo risco.

---

### Eixo 4 — Novas funcionalidades `[REQUER SPEC]`

Em aberto — precisa de definição do João (o quê exatamente: treino, nutrição, dashboards,
relatórios, etc.). Assim que houver alvo concreto, desenho + implemento.

---

## Ordem sugerida de execução

1. ~~**Eixo 1** (1.1 → 1.2 → 1.3)~~ — ✅ **feito** (SW cacheia Firebase SDK; persistência IndexedDB do Firestore; `VERSION` → `blueon-v3.7.0-perf-offline`).
2. **Eixo 3** subset seguro (`innerHTML` de texto livre + degradação de rede).
3. **Eixo 2** — só depois de decidir opção A/B/C e ter backup/migração.
4. **Eixo 4** — quando houver spec.

## Estado atual do trabalho

- Branch: `claude/superpowers-zq24ts`
- PR: [#2](https://github.com/JotazizuSensei/blueon-app/pull/2) (draft) — remove `blue-on-app.html` duplicado.
- Este documento: ponto de partida para o backoffice de otimização.
