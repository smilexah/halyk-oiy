/* ════════════════════════════════════════════════════════════
   Halyk Maqsat — First-run flow
   Salary push (lockscreen) → tap → wizard (4 steps):
     1/4 Обязательные платежи — единый счёт
     2/4 Дети · Жанека — счёт + дневной лимит
     3/4 Дети · Аминош — счёт + дневной лимит
     4/4 Свободные деньги — распределение
   → Face ID → success → onboarded → «Мои финансы» (manage).
   Self-contained; uses app tokens + #success overlay + budget.js hooks.
   ════════════════════════════════════════════════════════════ */
(function(){
  const q  = s => document.querySelector(s);
  const money = n => Math.round(n).toLocaleString('ru-RU');
  const parseAmt = v => parseInt(String(v).replace(/\D/g,'')) || 0;
  const round100 = n => Math.round(n/100)*100;

  const SALARY = 320000;
  const MONTH = 'июнь';

  const MANDATORY = [
    { id:'util', name:'Коммуналка', sub:'свет · газ · вода',     emoji:'🏠', amount:33000, on:true,
      why:'Списания 1-го числа 8 месяцев подряд' },
    { id:'tax',  name:'Wi-fi',     sub:'Интернет',          emoji:'🏛️', amount:9000,  on:true,
      why:'Годовой налог — коплю помесячно' },
    { id:'subs', name:'Подписки',   sub:'Netflix · Spotify · iCloud', emoji:'🔁', amount:6500, on:true,
      why:'3 автоплатежа распознаны по истории карты' },
  ];

  const KIDS = [
    { id:'zhan', name:'Жанека', role:'дочь · студентка', emoji:'👩🏻', topup:20000, limit:2000, skip:false,
      why:'Каждый месяц вы переводите 20 000 ₸ контакту Жанека. Откройте счёт, с которого она сможет снимать деньги сама, а вы — управлять лимитами.' },
    { id:'amin', name:'Аминош', role:'дочь · школа', emoji:'👧🏻', topup:12000, limit:1000, skip:false,
      why:'Аминош вы тоже даёте карманные + кружок танцев. Откройте счёт с дневным лимитом — она тратит сама, а превышение вы подтверждаете.' },
  ];

  const STEPS = ['mandatory','kid0','kid1','summary'];
  const TOTAL = STEPS.length;
  let step = 0;

  const sumMandatory = () => MANDATORY.filter(c=>c.on).reduce((s,c)=>s+c.amount,0);
  const sumKids = () => KIDS.filter(k=>!k.skip).reduce((s,k)=>s+k.topup,0);
  const freePool = () => SALARY - sumMandatory() - sumKids();

  // free-money split (мягкие ориентиры)
  const SPLIT = [
    { id:'food', name:'Еда и продукты', pct:0.35, color:'#006B4F' },
    { id:'tran', name:'Транспорт',      pct:0.15, color:'#77D9B2' },
    { id:'fun',  name:'Развлечения',    pct:0.20, color:'#F1A400' },
    { id:'free', name:'Свободные деньги',pct:0.30, color:'#D2D2D2' },
  ];
  const splitAmt = s => round100(freePool()*s.pct);

  /* ── CSS ─────────────────────────────────────────────────── */
  function injectCSS(){
    if(q('#frStyle')) return;
    const css = `
    /* lockscreen */
    .fr-lock{ position:absolute; inset:0; z-index:120; display:none; flex-direction:column;
      padding:54px 18px calc(20px + env(safe-area-inset-bottom)); color:#fff;
      background:linear-gradient(160deg,#0b3a2c 0%,#0f5a41 38%,#1b7d5c 64%,#3aa17a 100%); overflow:hidden; }
    .fr-lock.show{ display:flex; }
    .fr-lock::before{ content:""; position:absolute; inset:0; opacity:.55;
      background:
        radial-gradient(120% 60% at 20% 78%, rgba(255,255,255,.16), transparent 60%),
        radial-gradient(90% 50% at 85% 88%, rgba(255,255,255,.12), transparent 60%); }
    .fr-lk-status{ position:relative; z-index:1; display:flex; align-items:center; gap:6px; font-size:12px; font-weight:600; opacity:.9; }
    .fr-lk-clock{ position:relative; z-index:1; text-align:center; margin-top:26px; }
    .fr-lk-clock .t{ font-size:74px; font-weight:700; letter-spacing:-.03em; line-height:1; text-shadow:0 2px 18px rgba(0,0,0,.18); }
    .fr-lk-clock .d{ font-size:16px; font-weight:600; opacity:.92; margin-top:8px; }
    .fr-push{ position:relative; z-index:1; margin-top:30px; background:rgba(255,255,255,.82);
      -webkit-backdrop-filter:blur(18px); backdrop-filter:blur(18px); border-radius:22px; padding:14px 15px;
      box-shadow:0 12px 40px rgba(0,0,0,.22); color:#13201b; cursor:pointer; border:1px solid rgba(255,255,255,.5);
      transition:transform .18s ease; }
    .fr-lock.show .fr-push{ animation:frPushIn .5s cubic-bezier(.2,1,.3,1) both; }
    @keyframes frPushIn{ from{ transform:translateY(-14px) scale(.96); } to{ transform:none; } }
    .fr-push:active{ transform:scale(.975); }
    .fr-push .ph{ display:flex; align-items:center; gap:8px; margin-bottom:8px; }
    .fr-push .plogo{ width:22px; height:22px; border-radius:7px; background:var(--green); display:grid; place-items:center; flex-shrink:0; }
    .fr-push .plogo svg{ width:13px; height:13px; }
    .fr-push .pn{ font-size:11px; font-weight:800; letter-spacing:.06em; color:#3a4a44; text-transform:uppercase; }
    .fr-push .pt{ margin-left:auto; font-size:11px; color:#6a7a73; font-weight:600; }
    .fr-push .ptitle{ font-size:15.5px; font-weight:800; letter-spacing:-.01em; }
    .fr-push .pbody{ font-size:13px; line-height:1.4; color:#34433d; margin-top:3px; font-weight:500; }
    .fr-push .pcta{ display:inline-flex; align-items:center; gap:6px; margin-top:10px; font-size:12px; font-weight:800; color:var(--green); }
    .fr-lk-hint{ position:relative; z-index:1; margin-top:auto; text-align:center; font-size:11.5px; font-weight:700; letter-spacing:.14em; opacity:.85; }
    .fr-lk-hint .ar{ display:block; margin-top:5px; animation:frUp 1.6s ease-in-out infinite; }
    @keyframes frUp{ 0%,100%{ transform:translateY(0); opacity:.6 } 50%{ transform:translateY(-4px); opacity:1 } }
    .fr-lk-dots{ position:relative; z-index:1; display:flex; justify-content:center; gap:7px; margin-top:14px; }
    .fr-lk-dots i{ width:6px; height:6px; border-radius:50%; background:rgba(255,255,255,.5); }

    /* wizard shell */
    .fr-wiz{ position:absolute; inset:0; z-index:115; background:var(--bg); display:none; flex-direction:column; }
    .fr-wiz.show{ display:flex; animation:frWizIn .32s ease both; }
    @keyframes frWizIn{ from{ opacity:0; transform:translateY(10px); } to{ opacity:1; transform:none; } }
    .fr-top{ padding:calc(14px + env(safe-area-inset-top)) 18px 6px; flex-shrink:0; background:var(--bg); }
    .fr-prog{ display:flex; gap:5px; }
    .fr-prog i{ flex:1; height:4px; border-radius:3px; background:var(--line); overflow:hidden; }
    .fr-prog i.done{ background:var(--green); }
    .fr-prog i.cur::after{ content:""; display:block; height:100%; width:55%; background:var(--green); border-radius:3px; }
    .fr-trow{ display:flex; align-items:center; justify-content:space-between; margin-top:13px; }
    .fr-x{ width:36px; height:36px; border-radius:50%; border:none; background:var(--line2); color:var(--text);
      font-size:17px; cursor:pointer; display:grid; place-items:center; }
    .fr-x:active{ transform:scale(.92); }
    .fr-step{ font-size:13px; font-weight:800; color:var(--text2); background:var(--line2); padding:6px 12px; border-radius:20px; }
    .fr-body{ flex:1; overflow-y:auto; padding:8px 18px 14px; -webkit-overflow-scrolling:touch; }
    .fr-foot{ flex-shrink:0; padding:12px 18px calc(14px + env(safe-area-inset-bottom)); background:var(--bg);
      box-shadow:0 -8px 22px rgba(0,0,0,.05); }
    [data-theme="dark"] .fr-foot{ box-shadow:0 -8px 22px rgba(0,0,0,.3); }
    .fr-ghost{ width:100%; margin-top:9px; border:none; background:transparent; color:var(--text2);
      font-family:var(--ff); font-size:13px; font-weight:700; cursor:pointer; padding:6px; }
    .fr-ghost:active{ opacity:.6; }
    .fr-swipe{ text-align:center; font-size:10.5px; font-weight:800; letter-spacing:.13em; color:var(--text2); margin-top:8px; opacity:.7; }

    .fr-h1{ font-size:25px; font-weight:800; letter-spacing:-.025em; line-height:1.18; margin:6px 0 0; text-wrap:pretty; }
    .fr-salary{ display:flex; align-items:center; gap:11px; background:var(--green-soft); border-radius:15px; padding:14px 15px; margin-top:20px; }
    .fr-salary .ic{ width:34px; height:34px; border-radius:10px; background:var(--green); display:grid; place-items:center; font-size:16px; flex-shrink:0; }
    .fr-salary .k{ font-size:14px; font-weight:800; }
    .fr-salary .v{ margin-left:auto; font-size:17px; font-weight:800; color:var(--pos); letter-spacing:-.02em; font-variant-numeric:tabular-nums; }

    .fr-mlist{ margin-top:14px; display:flex; flex-direction:column; gap:10px; }
    .fr-mcard{ background:var(--card); border-radius:16px; box-shadow:var(--shadow); padding:13px 14px; transition:opacity .2s; }
    .fr-mcard.off{ opacity:.45; }
    .fr-mc-top{ display:flex; align-items:center; gap:12px; }
    .fr-mc-ic{ width:44px; height:44px; border-radius:13px; background:var(--green-soft); display:grid; place-items:center; font-size:21px; flex-shrink:0; }
    .fr-mc-i{ flex:1; min-width:0; }
    .fr-mc-i .a{ font-size:14.5px; font-weight:800; letter-spacing:-.01em; }
    .fr-mc-i .b{ font-size:11.5px; color:var(--text2); font-weight:600; margin-top:2px; }
    .fr-mc-amt{ font-size:16px; font-weight:800; letter-spacing:-.02em; font-variant-numeric:tabular-nums; white-space:nowrap; }
    .fr-check{ width:28px; height:28px; border-radius:50%; border:2px solid var(--line); background:transparent; cursor:pointer;
      display:grid; place-items:center; flex-shrink:0; transition:all .15s; color:#fff; font-size:15px; }
    .fr-check.on{ background:var(--green); border-color:var(--green); }
    .fr-mc-why{ display:flex; gap:7px; align-items:center; margin-top:9px; padding-top:9px; border-top:1px solid var(--line2);
      font-size:11px; color:var(--text2); font-weight:500; }
    .fr-remain{ display:flex; align-items:center; gap:8px; justify-content:flex-end; margin-top:14px; }
    .fr-remain .chip{ background:var(--line2); border-radius:22px; padding:9px 15px; font-size:13px; font-weight:700; color:var(--text2); }
    .fr-remain .chip b{ color:var(--text); font-weight:800; font-variant-numeric:tabular-nums; }

    /* kid step */
    .fr-person{ display:flex; flex-direction:column; align-items:center; margin-top:22px; }
    .fr-person .av{ width:84px; height:84px; border-radius:50%; background:var(--green-soft); display:grid; place-items:center;
      font-size:42px; box-shadow:0 6px 18px rgba(var(--accent-glow),.18); position:relative; }
    .fr-person .av::after{ content:""; position:absolute; right:2px; bottom:2px; width:26px; height:26px; border-radius:50%;
      background:var(--green); border:3px solid var(--bg); background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='13' height='13' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='2.4' stroke-linecap='round' stroke-linejoin='round'%3E%3Crect x='2' y='5' width='20' height='14' rx='3'/%3E%3Cpath d='M2 10h20'/%3E%3C/svg%3E"); background-repeat:no-repeat; background-position:center; }
    .fr-person .nm{ font-size:18px; font-weight:800; margin-top:11px; letter-spacing:-.01em; }
    .fr-person .rl{ font-size:12px; font-weight:600; color:var(--text2); margin-top:2px; }
    .fr-field{ background:var(--card); border-radius:16px; box-shadow:var(--shadow); padding:14px 15px; margin-top:14px; }
    .fr-field > label{ font-size:12px; font-weight:700; color:var(--text2); }
    .fr-input{ display:flex; align-items:center; gap:6px; background:var(--line2); border-radius:12px; padding:13px 15px; margin-top:8px; }
    .fr-input input{ flex:1; border:none; background:transparent; outline:none; font-family:var(--ff); font-size:19px; font-weight:800;
      color:var(--text); letter-spacing:-.01em; font-variant-numeric:tabular-nums; min-width:0; }
    .fr-input .cur{ font-size:15px; font-weight:700; color:var(--text2); }
    .fr-chips{ display:flex; flex-wrap:wrap; gap:8px; margin-top:11px; }
    .fr-chip{ border:1.5px solid var(--line); background:transparent; border-radius:11px; padding:9px 14px; cursor:pointer;
      font-family:var(--ff); font-size:13px; font-weight:700; color:var(--text2); transition:all .15s; }
    .fr-chip.on{ background:var(--green); border-color:var(--green); color:#fff; }
    .fr-chip:active{ transform:scale(.95); }
    .fr-impact{ background:var(--card); border-radius:16px; box-shadow:var(--shadow); padding:14px 15px; margin-top:14px; }
    .fr-impact > label{ font-size:12px; font-weight:700; color:var(--text2); }
    .fr-seg{ display:flex; height:16px; border-radius:9px; overflow:hidden; margin-top:10px; background:var(--line2); }
    .fr-seg i{ height:100%; }
    .fr-leg{ display:grid; grid-template-columns:1fr 1fr; gap:7px 12px; margin-top:12px; }
    .fr-leg .lr{ display:flex; align-items:center; gap:7px; font-size:11.5px; font-weight:600; color:var(--text); }
    .fr-leg .dot{ width:9px; height:9px; border-radius:50%; flex-shrink:0; }
    .fr-leg .lr .v{ margin-left:auto; color:var(--text2); font-weight:700; font-variant-numeric:tabular-nums; }

    /* summary step */
    .fr-pool{ background:var(--card); border-radius:18px; box-shadow:var(--shadow); padding:17px 17px 16px; margin-top:20px; }
    .fr-pool .hd{ display:flex; align-items:baseline; justify-content:space-between; }
    .fr-pool .hd .v{ font-size:26px; font-weight:800; letter-spacing:-.025em; font-variant-numeric:tabular-nums; }
    .fr-pool .hd .k{ font-size:13px; font-weight:700; color:var(--text2); }
    .fr-pool .fr-seg{ height:30px; border-radius:13px; margin-top:14px; }
    .fr-pool .fr-legend{ margin-top:14px; display:flex; flex-direction:column; gap:9px; }
    .fr-pool .lr{ display:flex; align-items:center; gap:9px; font-size:13.5px; font-weight:700; }
    .fr-pool .lr .dot{ width:11px; height:11px; border-radius:50%; flex-shrink:0; }
    .fr-pool .lr .v{ margin-left:auto; font-weight:700; font-variant-numeric:tabular-nums; }
    .fr-pool .lr .pct{ color:var(--text2); font-weight:600; font-size:12px; width:42px; text-align:right; }
    .fr-freecard{ display:flex; align-items:center; gap:13px; background:var(--accent-grad); border-radius:16px; padding:15px 16px;
      margin-top:14px; color:#fff; box-shadow:0 8px 22px rgba(var(--accent-glow),.26); position:relative; overflow:hidden; }
    .fr-freecard::after{ content:""; position:absolute; right:-26px; bottom:-32px; width:120px; height:120px; border-radius:50%; background:rgba(255,255,255,.12); }
    .fr-freecard .ft{ flex:1; position:relative; z-index:1; }
    .fr-freecard .ft .a{ font-size:14px; font-weight:800; }
    .fr-freecard .ft .b{ font-size:12px; opacity:.92; margin-top:2px; font-weight:500; }
    .fr-freecard .fi{ width:42px; height:42px; border-radius:13px; background:rgba(255,255,255,.2); display:grid; place-items:center; font-size:20px; position:relative; z-index:1; flex-shrink:0; }

    /* face id */
    .fr-face{ position:absolute; inset:0; z-index:130; background:rgba(8,16,13,.72); -webkit-backdrop-filter:blur(8px); backdrop-filter:blur(8px);
      display:none; flex-direction:column; align-items:center; justify-content:center; gap:20px; color:#fff; }
    .fr-face.show{ display:flex; }
    .fr-face .ring{ width:128px; height:128px; border-radius:34px; border:3px solid rgba(255,255,255,.25); position:relative; display:grid; place-items:center; overflow:hidden; }
    .fr-face .ring .ico{ font-size:58px; }
    .fr-face .ring .scan{ position:absolute; left:8px; right:8px; height:3px; border-radius:3px;
      background:linear-gradient(90deg,transparent,#3aa17a,transparent); box-shadow:0 0 12px #3aa17a; animation:frScan 1.1s ease-in-out infinite; }
    @keyframes frScan{ 0%,100%{ top:14px } 50%{ top:108px } }
    .fr-face .ring.ok{ border-color:#3aa17a; }
    .fr-face h3{ font-size:17px; font-weight:800; }
    .fr-face p{ font-size:12.5px; opacity:.8; font-weight:500; margin-top:-12px; }
    `;
    const st = document.createElement('style'); st.id='frStyle'; st.textContent = css;
    document.head.appendChild(st);
  }

  /* ── DOM build ───────────────────────────────────────────── */
  let lockEl, wizEl, faceEl, built=false;
  function build(){
    if(built) return;
    built = true;
    injectCSS();
    const app = q('#app');

    lockEl = document.createElement('div');
    lockEl.className = 'fr-lock'; lockEl.id = 'frLock';
    lockEl.innerHTML = `
      <div class="fr-lk-status">🔒 заблокировано</div>
      <div class="fr-lk-clock"><div class="t">10:42</div><div class="d">Понедельник, 2 июня</div></div>
      <div class="fr-push" id="frPush">
        <div class="ph">
          <div class="plogo"><svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18M5 21V10l7-5 7 5v11M9 21v-6h6v6"/></svg></div>
          <span class="pn">Halyk Bank</span>
          <span class="pt">сейчас</span>
        </div>
        <div class="ptitle">Зарплата! 🎉</div>
        <div class="pbody">Деньги уже на счету. Нажмите — распределю на главное, чтобы не уйти в минус к концу месяца.</div>
        <div class="pcta">Распределить с агентом <span>→</span></div>
      </div>
      <div class="fr-lk-dots"><i></i><i></i><i></i></div>
      <div class="fr-lk-hint">СВАЙП, ЧТОБЫ ОТКРЫТЬ<span class="ar">⌃</span></div>`;
    app.appendChild(lockEl);

    wizEl = document.createElement('div');
    wizEl.className = 'fr-wiz'; wizEl.id = 'frWiz';
    wizEl.innerHTML = `
      <div class="fr-top">
        <div class="fr-prog" id="frProg"></div>
        <div class="fr-trow">
          <button class="fr-x" id="frClose" aria-label="Закрыть">✕</button>
          <span class="fr-step" id="frStep">1/${TOTAL}</span>
        </div>
      </div>
      <div class="fr-body" id="frBody"></div>
      <div class="fr-foot" id="frFoot"></div>`;
    app.appendChild(wizEl);

    faceEl = document.createElement('div');
    faceEl.className = 'fr-face'; faceEl.id = 'frFace';
    faceEl.innerHTML = `
      <div class="ring" id="frFaceRing"><div class="scan"></div><div class="ico">🙂</div></div>
      <div><h3 id="frFaceH">Подтвердите по Face ID</h3><p>Открываем счета и пополняем</p></div>`;
    app.appendChild(faceEl);

    q('#frPush').addEventListener('click', startFirstRun);
    lockEl.addEventListener('click', e => { if(e.target===lockEl) startFirstRun(); });
    q('#frClose').addEventListener('click', closeWizard);
  }

  /* ── lockscreen control ──────────────────────────────────── */
  function showLock(){ build(); lockEl.classList.add('show'); }
  function hideLock(){ if(lockEl) lockEl.classList.remove('show'); }

  /* ── wizard ──────────────────────────────────────────────── */
  function startFirstRun(){
    build(); hideLock();
    wizEl.classList.add('show');
    step = 0;
    // brief analyzing splash
    q('#frProg').innerHTML = Array.from({length:TOTAL}).map(()=>`<i></i>`).join('');
    q('#frStep').style.visibility = 'hidden';
    q('#frBody').innerHTML = `
      <div style="text-align:center;padding:56px 10px;">
        <div style="width:76px;height:76px;border-radius:22px;margin:0 auto;background:var(--green-soft);display:grid;place-items:center;font-size:36px;">🧠</div>
        <h2 style="font-size:20px;font-weight:800;letter-spacing:-.02em;margin-top:18px;">Готовлю план распределения…</h2>
        <p style="font-size:13px;color:var(--text2);font-weight:500;margin-top:8px;line-height:1.5;">Смотрю обязательные платежи, регулярные переводы детям и средние траты за 3 месяца.</p>
      </div>`;
    q('#frFoot').innerHTML = '';
    setTimeout(()=>{ q('#frStep').style.visibility=''; renderStep(0); }, 1500);
  }

  function closeWizard(){ if(wizEl) wizEl.classList.remove('show'); }

  function renderProg(){
    const cells = Array.from({length:TOTAL}).map((_,i)=>{
      if(i<step) return `<i class="done"></i>`;
      if(i===step) return `<i class="cur"></i>`;
      return `<i></i>`;
    }).join('');
    q('#frProg').innerHTML = cells;
    q('#frStep').textContent = (step+1)+'/'+TOTAL;
  }

  function renderStep(i){
    step = i; renderProg();
    const body = q('#frBody'); const foot = q('#frFoot');
    body.scrollTop = 0;
    const kind = STEPS[i];
    if(kind==='mandatory') return stepMandatory(body, foot);
    if(kind==='kid0') return stepKid(body, foot, 0);
    if(kind==='kid1') return stepKid(body, foot, 1);
    if(kind==='summary') return stepSummary(body, foot);
  }
  const next = () => renderStep(Math.min(step+1, TOTAL-1));

  /* step 1 — обязательные */
  function stepMandatory(body, foot){
    body.innerHTML = `
      <h1 class="fr-h1">Все важные счета — под контролем. Настройте один раз и забудьте.</h1>
      <div class="fr-salary"><span class="ic">💳</span><span class="k">Зарплата</span><span class="v">${money(SALARY)} ₸</span></div>
      <p style="font-size:12.5px;color:var(--text2);font-weight:600;line-height:1.5;margin:16px 2px 0;">Обязательные платежи я объединю на <b style="color:var(--text)">один счёт</b> — дальше они спишутся сами. Снимите галочку, если что-то платить не нужно.</p>
      <div class="fr-mlist">${MANDATORY.map(mcard).join('')}</div>
      <div class="fr-remain"><div class="chip" id="frRemainChip"></div></div>`;
    foot.innerHTML = `<button class="btn btn-primary" id="frCta">Открыть счёт и пополнить</button>
      <div class="fr-swipe">СВАЙП ВВЕРХ ⌃</div>`;
    body.querySelectorAll('.fr-check').forEach(b => b.addEventListener('click', () => {
      const c = MANDATORY.find(x=>x.id===b.dataset.id); c.on=!c.on;
      b.classList.toggle('on', c.on);
      b.closest('.fr-mcard').classList.toggle('off', !c.on);
      b.innerHTML = c.on ? '✓' : '';
      remainChip();
    }));
    q('#frCta').addEventListener('click', next);
    remainChip();
  }
  function mcard(c){
    return `<div class="fr-mcard ${c.on?'':'off'}">
      <div class="fr-mc-top">
        <div class="fr-mc-ic">${c.emoji}</div>
        <div class="fr-mc-i"><div class="a">${c.name}</div><div class="b">${c.sub}</div></div>
        <div class="fr-mc-amt">${money(c.amount)} ₸</div>
        <button class="fr-check ${c.on?'on':''}" data-id="${c.id}">${c.on?'✓':''}</button>
      </div>
      <div class="fr-mc-why"><span>🤖</span><span>${c.why}</span></div>
    </div>`;
  }
  function remainChip(){
    const after = SALARY - sumMandatory();
    const el = q('#frRemainChip'); if(el) el.innerHTML = `Остаток: <b>${money(after)} ₸</b>`;
  }

  /* step 2/3 — дети */
  function stepKid(body, foot, idx){
    const k = KIDS[idx];
    const limits = [1000,2000,5000,10000];
    body.innerHTML = `
      <h1 class="fr-h1">${k.why}</h1>
      <div class="fr-person"><div class="av">${k.emoji}</div><div class="nm">${k.name}</div><div class="rl">${k.role}</div></div>
      <div class="fr-field">
        <label>Сумма пополнения</label>
        <div class="fr-input"><input id="frTopup" inputmode="numeric" value="${money(k.topup)}"><span class="cur">₸</span></div>
      </div>
      <div class="fr-field">
        <label>Дневной лимит трат</label>
        <div class="fr-input"><input id="frLimit" inputmode="numeric" value="${money(k.limit)}"><span class="cur">₸</span></div>
        <div class="fr-chips">${limits.map(l=>`<button class="fr-chip ${l===k.limit?'on':''}" data-l="${l}">${money(l)} ₸</button>`).join('')}</div>
      </div>
      <div class="fr-impact" id="frImpact"></div>`;
    foot.innerHTML = `<button class="btn btn-primary" id="frCta">Открыть счёт и пополнить</button>
      <button class="fr-ghost" id="frSkip">Не сейчас — остальное в депозит</button>`;

    const topup = q('#frTopup'), limit = q('#frLimit');
    topup.addEventListener('focus', ()=>{ topup.value=String(k.topup); topup.select(); });
    topup.addEventListener('input', ()=>{ k.topup=parseAmt(topup.value); drawImpact(); });
    topup.addEventListener('blur', ()=>{ topup.value=money(k.topup); });
    limit.addEventListener('focus', ()=>{ limit.value=String(k.limit); limit.select(); });
    limit.addEventListener('input', ()=>{ k.limit=parseAmt(limit.value); syncChips(idx); });
    limit.addEventListener('blur', ()=>{ limit.value=money(k.limit); });
    body.querySelectorAll('.fr-chip').forEach(ch => ch.addEventListener('click', () => {
      k.limit = parseInt(ch.dataset.l); limit.value = money(k.limit); syncChips(idx);
    }));
    q('#frCta').addEventListener('click', ()=>{ k.skip=false; next(); });
    q('#frSkip').addEventListener('click', ()=>{ k.skip=true; next(); });
    drawImpact();
  }
  function syncChips(idx){
    const k = KIDS[idx];
    document.querySelectorAll('#frBody .fr-chip').forEach(ch => ch.classList.toggle('on', parseInt(ch.dataset.l)===k.limit));
  }
  function drawImpact(){
    const el = q('#frImpact'); if(!el) return;
    const segs = SPLIT.map(s=>({...s, amt:splitAmt(s)}));
    el.innerHTML = `<label>Влияние на свободный бюджет</label>
      <div class="fr-seg">${segs.map(s=>`<i style="flex:${Math.max(s.amt,1)};background:${s.color}"></i>`).join('')}</div>
      <div class="fr-leg">${segs.map(s=>`<div class="lr"><span class="dot" style="background:${s.color}"></span>${s.name}<span class="v">${money(s.amt)}</span></div>`).join('')}</div>`;
  }

  /* step 4 — свободные деньги */
  function stepSummary(body, foot){
    const pool = freePool();
    const segs = SPLIT.map(s=>({...s, amt:splitAmt(s)}));
    const freeAmt = segs.find(s=>s.id==='free').amt;
    body.innerHTML = `
      <h1 class="fr-h1">Обязательное — оплачено. Управляйте свободными деньгами.</h1>
      <div class="fr-pool">
        <div class="hd"><span class="v">${money(pool)} ₸</span><span class="k">всего свободно</span></div>
        <div class="fr-seg">${segs.map(s=>`<i style="flex:${s.amt};background:${s.color}"></i>`).join('')}</div>
        <div class="fr-legend">${segs.map(s=>`<div class="lr"><span class="dot" style="background:${s.color}"></span>${s.name}<span class="pct">${Math.round(s.pct*100)}%</span><span class="v">${money(s.amt)} ₸</span></div>`).join('')}</div>
      </div>
      <div class="fr-freecard">
        <div class="fi">📈</div>
        <div class="ft"><div class="a">Свободно ${money(freeAmt)} ₸</div><div class="b">Куда направим? Депозит 16,5% · копилка на цель</div></div>
      </div>
      <p style="font-size:12px;color:var(--text2);font-weight:500;line-height:1.5;margin:16px 4px 0;text-align:center;">Это мягкие ориентиры по категориям — отдельные счета не создаются. Если выйдете за лимит, я пришлю уведомление, но платёж пройдёт.</p>`;
    foot.innerHTML = `<button class="btn btn-primary" id="frCta">Распределить и подтвердить</button>
      <div class="fr-swipe">FACE ID НА СЛЕДУЮЩЕМ ШАГЕ</div>`;
    q('#frCta').addEventListener('click', runFace);
  }

  /* ── Face ID → success ───────────────────────────────────── */
  function runFace(){
    build();
    const ring = q('#frFaceRing'), h = q('#frFaceH');
    ring.classList.remove('ok'); ring.querySelector('.ico').textContent='🙂';
    if(ring.querySelector('.scan')) ring.querySelector('.scan').style.display='';
    h.textContent = 'Подтвердите по Face ID';
    faceEl.classList.add('show');
    setTimeout(()=>{
      ring.classList.add('ok'); ring.querySelector('.ico').textContent='✅';
      const sc = ring.querySelector('.scan'); if(sc) sc.style.display='none';
      h.textContent = 'Готово';
    }, 1500);
    setTimeout(()=>{ faceEl.classList.remove('show'); showSuccess(); }, 2150);
  }

  function showSuccess(){
    const opened = [];
    opened.push({ n:'Единый счёт · обязательное', v:sumMandatory() });
    KIDS.filter(k=>!k.skip).forEach(k => opened.push({ n:`Счёт «${k.name}» · лимит ${money(k.limit)} ₸/день`, v:k.topup }));
    const total = sumMandatory()+sumKids();
    const sum = q('#successSum');
    if(sum){
      sum.innerHTML =
        `<div class="sr hd"><span class="sk">Открыто и пополнено · ${MONTH}</span><span class="sv">${opened.length}</span></div>` +
        opened.map(o=>`<div class="sr"><span class="sk">${o.n}</span><span class="sv">${money(o.v)} ₸</span></div>`).join('') +
        `<div class="sr"><span class="sk">Свободные деньги</span><span class="sv">${money(freePool())} ₸</span></div>` +
        `<div class="sr tot"><span>Списано из зарплаты</span><span>${money(total)} ₸</span></div>`;
    }
    const hd = q('#successHd'); if(hd) hd.textContent = 'Счета открыты и пополнены';
    const tx = q('#successText'); if(tx) tx.textContent = 'Обязательные платежи и счета детям настроены. Дальше я веду бюджет сам — управляйте им в «Мои финансы».';
    try { localStorage.setItem('halyk_onboarded','1'); } catch(e){}
    closeWizard();
    const ov = q('#success'); if(ov) ov.classList.add('show');
  }

  /* completion → manage view (called when user taps «Готово» on success) */
  function finishToManage(){
    const ov = q('#success'); if(ov) ov.classList.remove('show');
    // switch to «Мои финансы» subtab
    document.querySelectorAll('#homeSub button').forEach(x => x.dataset.on = x.dataset.sub==='fin' ? '1' : '0');
    document.querySelectorAll('#home .subpane').forEach(p => p.classList.toggle('on', p.dataset.sub==='fin'));
    const badge = document.querySelector('#homeSub button[data-sub="fin"] .nbz'); if(badge) badge.remove();
    const sc = q('#screens'); if(sc) sc.scrollTop = 0;
    if(window.budgetGoManage) window.budgetGoManage();
  }

  /* ── reset (replay) ──────────────────────────────────────── */
  function resetFirstRun(){
    try { localStorage.removeItem('halyk_onboarded'); } catch(e){}
    MANDATORY.forEach(c=>c.on=true);
    KIDS.forEach((k,i)=>{ k.skip=false; k.topup = i===0?20000:12000; k.limit = i===0?2000:1000; });
    if(window.budgetShowIntro) window.budgetShowIntro();
    showLock();
  }

  /* ── boot ────────────────────────────────────────────────── */
  function boot(){
    build();
    // Always land on the salary push lockscreen as the default entry point.
    showLock();
    // hook success «Готово» to land on manage (only meaningful right after first run)
    const done = q('#successDone');
    if(done) done.addEventListener('click', () => {
      if((()=>{ try { return localStorage.getItem('halyk_onboarded')==='1'; } catch(e){ return false; } })()
         && wizEl && getComputedStyle(wizEl).display!=='flex') {
        // landed from first run
      }
      finishToManage();
    });
  }
  window.startFirstRun = startFirstRun;
  window.resetFirstRun = resetFirstRun;
  if(document.readyState!=='loading') boot();
  else document.addEventListener('DOMContentLoaded', boot);
})();
