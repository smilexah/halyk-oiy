# Maqsat — исходники прототипа (очищено для порта на React + Vite + TS)

Из архива оставлен **только код**. Всё остальное (папки `scrap/`, `screenshots/`,
`Архив/`, `Архив (2)/`, все `*.png` и `screen.png`) — это мокапы/скриншоты,
на них **нигде в коде нет ссылок**, для переписывания они не нужны и удалены.

## Что это за прототип

Это **два независимых HTML-приложения**, которые делят один визуальный язык
(дизайн-токены + тёмная/светлая темы + акцентный цвет gold/green).

### Приложение A — «Halyk · Умный бюджет» (`index.html`)
Точка входа всего прототипа.

| Файл | Что внутри | Что экспортирует в `window` |
|---|---|---|
| `index.html` | строки **11–782** — весь CSS в `<style>` (дизайн-токены `:root`, оси `[data-theme]`/`[data-accent]`, компоненты). строки **784–1056** — разметка. строки **1057–1448** — inline-JS: навигация, меню + общие хелперы `showToast` (1319), `openSheet` (1328), `closeSheets` (1329) | — |
| `budget.js` | «Мои финансы» / AI-бюджет: flow `onboard → analyzing → manage` | `initBudget`, `budgetGoManage`, `budgetShowIntro` |
| `firstrun.js` | First-run: push о зарплате → мастер из 4 шагов → Face ID → success | `startFirstRun`, `resetFirstRun` |

### Приложение B — «Maqsat & Family» (`Maqsat & Family.html`)
Открывается ссылкой из приложения A.

| Файл | Что внутри | Что экспортирует в `window` |
|---|---|---|
| `Maqsat & Family.html` | разметка семейного раздела | — |
| `family.css` | стили этого раздела (708 строк) | — |
| `family.js` | ядро: данные `MEMBERS`, синк темы из localStorage, общие хелперы `openSheet`/`closeSheets`/`showToast`/`showSuccess` | — |
| `distribute.js` | сессия распределения зарплаты (режимы `first` / `power`) | `openDistribute` |
| `goaldetail.js` | детальный экран цели (`paris` — семейная, `turkey` — личная) | `openGoalDetail` |

## Общее состояние (через `localStorage`)
- `halyk-theme` — `light` / `dark`
- `halyk-accent` — `gold` / `green`
- `halyk_onboarded` — `'1'` после прохождения first-run

Приложение B читает `halyk-theme` и `halyk-accent`, чтобы подхватить тему,
выбранную в приложении A.

## Ориентир для порта на React + Vite + TS
- **Дизайн-токены** (CSS-переменные из `:root` в `index.html`) → один глобальный
  `tokens.css` + `ThemeProvider`, который выставляет `data-theme` / `data-accent`
  на корне. Это убирает дублирование стилей между двумя приложениями.
- **Глобальные хелперы** `window.showToast` / `openSheet` / `closeSheets` /
  `showSuccess` → один `ToastContext` + хук `useSheet()` вместо классов `.show`
  на DOM-узлах.
- **Шаблоны через `innerHTML`** (их много в `budget.js`/`distribute.js`/
  `goaldetail.js`) → React-компоненты; данные (`MANDATORY`, `MEMBERS`, `GOALS`,
  `SCEN`) выносятся в типизированные модули `*.ts`.
- **Связка `window.initBudget`/`openDistribute`/`openGoalDetail`** → роутинг
  (react-router) или условный рендер по состоянию вместо ручного навешивания
  на глобал.
- **`localStorage`-флаги** → `useLocalStorage`-хук / общий стор (zustand/context).

Логику и UI можно один-в-один читать из этих файлов — это полная,
самодостаточная реализация прототипа.
