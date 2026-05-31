/* ════════════════════════════════════════════════════════════
   Halyk Maqsat — Мои финансы (AI budgeting)
   onboard → analyzing → manage
   manage view: описание · зарплата · Блок 1 (обязательные, единый счёт,
   кнопки Пополнить + прогноз остатка) · Блок 2 (рекомендации, без счёта,
   уведомление при превышении + Подтвердить план) · AI-ассистент чат.
   Self-contained; relies on global showToast/openSheet/closeSheets.
   ════════════════════════════════════════════════════════════ */
(function(){
  const q  = s => document.querySelector(s);
  const qa = s => Array.from(document.querySelectorAll(s));
  const money = n => Math.round(n).toLocaleString('ru-RU');
  const parseAmt = v => parseInt(String(v).replace(/\D/g,'')) || 0;
  const toast = m => window.showToast && window.showToast(m);

  const INCOME = 320000;
  const MONTH = 'июнь';

  // Блок 1 — обязательные платежи (единый счёт). paid = оплачено из зарплаты.
  // amount = сколько должно быть на счёте к концу месяца (цель)
  // have   = сколько уже лежит на едином счёте сейчас
  // sel    = пополнять из зарплаты (по умолчанию все отмечены)
  const MANDATORY = [
    { id:'util', name:'Коммуналка', icon:'🏠', amount:33000, have:8000, sub:'свет · газ · вода',
      why:'Регулярные списания 1 числа последние 8 месяцев.', sel:true },
    { id:'tax', name:'Wi-fi', icon:'🏛️', amount:9000, have:9000, sub:'Интернет,',
      why:'Годовой налог разбит помесячно — копим заранее.', sel:true },
    { id:'subs', name:'Подписки', icon:'🔁', amount:6500, have:2000, sub:'Netflix · Spotify · iCloud',
      why:'3 регулярных автоплатежа распознаны по истории карты.', sel:true },
    { id:'kids', name:'Дети и образование', icon:'🎓', amount:71000, have:30000, sub:'карманные · кружки · общежитие',
      why:'Ежемесячные переводы детям и за обучение — фиксированы.', sel:true },
  ];
  const needOf = c => Math.max(0, c.amount - c.have);
  const depOf  = c => c.sel ? needOf(c) : 0;
  // Блок 2 — рекомендации (без счёта). limit = ориентир, avg = средняя трата.
  const RECO = [
    { id:'food', name:'Еда и продукты', icon:'🛒', limit:90000, avg:88000, spent:52400,
      why:'Magnum, Small, базары — в среднем 88 000 ₸/мес за 3 месяца.' },
    { id:'fun', name:'Развлечения', icon:'🎬', limit:30000, avg:34000, spent:18900,
      why:'Кафе, кино, рестораны. В прошлом месяце вышли за рамки.' },
    { id:'taxi', name:'Такси и транспорт', icon:'🚕', limit:18000, avg:21000, spent:13200,
      why:'Яндекс Go и inDrive — поездок стало больше обычного.' },
    { id:'auto', name:'Авто и бензин', icon:'⛽', limit:28000, avg:26000, spent:9600,
      why:'АЗС и мелкое обслуживание. СТO раз в полгода — отдельно.' },
  ];

  const sumMandatory = () => MANDATORY.reduce((s,c)=>s+depOf(c),0);
  const sumReco      = () => RECO.reduce((s,c)=>s+c.limit,0);
  const afterMandatory = () => INCOME - sumMandatory();
  const freeMoney      = () => afterMandatory() - sumReco();

  let state = 'onboard';
  const root = () => q('#budgetRoot');

  /* ── PRE-ONBOARD INTRO (до первого распределения) ──────────── */
  function viewOnboard(){
    root().innerHTML = `
      <div class="budget-onboard">
        <div class="bo-bot">💰</div>
        <h2>Пришла зарплата<br>${money(INCOME)} ₸</h2>
        <p>Я подготовил план: открою счёт для обязательных платежей, счета детям с лимитами и покажу, сколько останется на жизнь. Настроим один раз — дальше я веду сам.</p>
        <div class="bo-feats">
          <div class="bo-feat"><span class="fi">🧾</span>Обязательное — на один единый счёт</div>
          <div class="bo-feat"><span class="fi">👧</span>Счета детям — снимают сами, лимиты у вас</div>
          <div class="bo-feat"><span class="fi">📊</span>Свободные деньги — вы решаете, куда</div>
        </div>
        <button class="btn btn-primary" id="boStart">Распределить зарплату</button>
        <div class="bo-note">Агент проанализировал 184 операции за 3 месяца</div>
      </div>`;
    q('#boStart').addEventListener('click', () => {
      if(window.startFirstRun) window.startFirstRun();
      else { state='analyzing'; viewAnalyzing(); }
    });
  }

  /* ── ANALYZING ────────────────────────────────────────────── */
  function viewAnalyzing(){
    root().innerHTML = `
      <div class="analyzing">
        <div class="scan">🧠</div>
        <h3>Анализирую ваши финансы…</h3>
        <div class="steps">
          <div class="stp" data-i="0"><span class="sc"></span>Считываю доход и регулярные платежи</div>
          <div class="stp" data-i="1"><span class="sc"></span>Отделяю обязательные платежи</div>
          <div class="stp" data-i="2"><span class="sc"></span>Считаю средние траты по категориям</div>
          <div class="stp" data-i="3"><span class="sc"></span>Готовлю рекомендации</div>
        </div>
      </div>`;
    const stps = qa('.analyzing .stp');
    let i=0;
    const tick = () => {
      if(i>0) stps[i-1].querySelector('.sc').textContent='✓';
      if(i<stps.length){ stps[i].classList.add('on'); i++; setTimeout(tick, 520); }
      else { setTimeout(()=>{ state='manage'; viewManage(); }, 450); }
    };
    tick();
  }

  /* ── MANAGE (main view) ───────────────────────────────────── */
  function mandHTML(c){
    const need = needOf(c);
    const full = need <= 0;
    const havePct = Math.min(100, Math.round(c.have / c.amount * 100));
    const solidPct = c.paid ? 100 : havePct;
    const addPct = c.sel && !full && !c.paid ? (100 - havePct) : 0;
    const status = c.paid
      ? '✓ пополнено'
      : (full ? '✓ накоплено' : (c.sel ? `+${money(need)} ₸ из зарплаты` : `не хватает ${money(need)} ₸`));
    const stCls = c.paid ? 'ok' : (full ? 'ok' : (c.sel ? 'add' : 'miss'));
    return `<div class="mcat ${c.sel?'':'unsel'}" data-id="${c.id}">
      <div class="mc-top">
        <div class="mc-ic">${c.icon}</div>
        <div class="mc-i"><div class="a">${c.name}</div><div class="b">${c.sub}</div></div>
        <button class="mc-chk ${c.sel?'on':''}" data-id="${c.id}" role="checkbox" aria-checked="${c.sel}" aria-label="Пополнять ${c.name}">${c.sel?'✓':''}</button>
      </div>
      <div class="mc-bar">
        <div class="mc-track"><i class="have" style="width:${solidPct}%"></i>${addPct?`<i class="add" style="left:${havePct}%;width:${addPct}%"></i>`:''}</div>
        <div class="mc-blbl"><span>Сейчас <b>${money(c.paid?c.amount:c.have)} ₸</b></span><span class="st ${stCls}">${status}</span><span>Цель <b>${money(c.amount)} ₸</b></span></div>
      </div>
    </div>`;
  }
  function recoHTML(c){
    const pct = Math.min(100, Math.round(c.spent / c.limit * 100));
    const over = c.spent > c.limit;
    const left = c.limit - c.spent;
    return `<div class="rcat" data-id="${c.id}">
      <div class="rc-top">
        <div class="rc-ic">${c.icon}</div>
        <div class="rc-i"><div class="a">${c.name}</div><div class="b">ср. ${money(c.avg)} ₸/мес · при превышении — уведомление</div></div>
        <div class="rc-amt"><input data-id="${c.id}" inputmode="numeric" value="${money(c.limit)}" aria-label="Лимит"/><span class="c">₸</span></div>
      </div>
      <div class="rc-bar">
        <div class="rc-track"><i style="width:${pct}%" class="${over?'over':''}"></i></div>
        <div class="rc-blbl"><span>Потрачено <b>${money(c.spent)} ₸</b></span><span class="st ${over?'over':'ok'}">${over?`превышение ${money(-left)} ₸`:`осталось ${money(left)} ₸`}</span></div>
      </div>
    </div>`;
  }

  function refreshRecoBar(c){
    const card = document.querySelector(`.rcat[data-id="${c.id}"]`);
    if(!card) return;
    const pct = Math.min(100, Math.round(c.spent / c.limit * 100));
    const over = c.spent > c.limit;
    const left = c.limit - c.spent;
    const bar = card.querySelector('.rc-track i');
    if(bar){ bar.style.width = pct+'%'; bar.classList.toggle('over', over); }
    const st = card.querySelector('.rc-blbl .st');
    if(st){ st.textContent = over ? `превышение ${money(-left)} ₸` : `осталось ${money(left)} ₸`; st.className = 'st '+(over?'over':'ok'); }
  }

  function viewManage(){
    root().innerHTML = `
      <div class="bdesc">
        <span class="bot">🤖</span>
        <div class="t">Я распределил вашу зарплату. <b>Обязательные платежи</b> закрываются с единого счёта, а остальное — <b>рекомендации</b>, куда можно потратить без отдельных счетов.</div>
      </div>

      <div class="bsalary">
        <div class="k">Зарплата · ${MONTH}</div>
        <div class="v">${money(INCOME)} <span class="c">₸</span></div>
        <div class="src">ТОО «Алтын Курылыс» · поступила сегодня</div>
      </div>

      <div class="bblock-hd"><span class="bt">Обязательные платежи</span><span class="bp m">единый счёт</span></div>
      <p class="bblock-sub">Эти платежи нужно оплатить в любом случае. Для них открыт один общий счёт. Отметьте, какие пополнить из зарплаты — по умолчанию все отмечены.</p>
      <div id="bMand"></div>
      <div class="bremain" id="bRemain"></div>

      <div class="bblock-hd" style="margin-top:22px;"><span class="bt">Рекомендации · куда потратить</span><span class="bp d">без счёта</span></div>
      <p class="bblock-sub">На основе ваших трат за 3 месяца. Отдельные счета не создаются — это ориентир. При выходе за лимит придёт уведомление, но платёж всё равно пройдёт.</p>
      ${RECO.map(recoHTML).join('')}
      <div class="bfree" id="bFree"></div>

      <button class="ai-fab" id="bChat"><span class="af-bot">💬</span><span class="af-t"><span class="a">Сказать ассистенту, что изменить</span><span class="b">«хочу меньше на еду», «урезать такси»…</span></span><span class="af-ch">→</span></button>

      <div class="cta-wrap" style="margin-top:14px;">
        <button class="btn btn-primary" id="bConfirm">Подтвердить план рекомендаций</button>
      </div>
      <p class="disclaimer">Подтверждение касается только рекомендаций (Блок 2). Обязательные платежи оплачиваются кнопкой «Пополнить». <a id="bReplay" style="color:var(--accent);text-decoration:underline;cursor:pointer;">↺ первый вход</a></p>`;

    bindManage();
    recalc();
  }

  function renderMand(){
    const wrap = q('#bMand'); if(!wrap) return;
    wrap.innerHTML = MANDATORY.map(mandHTML).join('');
    qa('#bMand .mc-chk').forEach(btn => btn.addEventListener('click', () => {
      const c = MANDATORY.find(x=>x.id===btn.dataset.id);
      c.sel = !c.sel;
      renderMand();
      recalc();
    }));
  }

  function bindManage(){
    renderMand();
    // reco limit inputs
    qa('.rc-amt input[data-id]').forEach(inp => {
      const c = RECO.find(x=>x.id===inp.dataset.id);
      inp.addEventListener('focus', ()=>{ inp.value=String(c.limit); inp.select(); });
      inp.addEventListener('input', ()=>{ c.limit=parseAmt(inp.value); refreshRecoBar(c); recalc(); });
      inp.addEventListener('blur', ()=>{ inp.value=money(c.limit); });
    });
    q('#bChat').addEventListener('click', openChat);
    q('#bConfirm').addEventListener('click', confirmPlan);
    q('#bReplay').addEventListener('click', () => {
      if(window.resetFirstRun) window.resetFirstRun();
      else { state='onboard'; resetState(); viewOnboard(); }
    });
  }

  function resetState(){
    MANDATORY.forEach(c=>{ c.sel=true; c.paid=false; });
  }

  function payMandatory(){
    const pending = MANDATORY.filter(c=>c.sel && needOf(c)>0 && !c.paid);
    if(!pending.length) return;
    const dep = pending.reduce((s,c)=>s+needOf(c),0);
    pending.forEach(c=>c.paid=true);
    renderMand();
    recalc();
    toast(`✅ Пополнено ${money(dep)} ₸ на единый счёт`);
  }

  function recalc(){
    const pending = MANDATORY.filter(c=>c.sel && needOf(c)>0 && !c.paid);
    const depPending = pending.reduce((s,c)=>s+needOf(c),0);
    q('#bRemain').innerHTML = `
      <div class="br-top">
        <div class="k">Останется на балансе<span class="s">после обязательных платежей</span></div>
        <div class="amt">${money(afterMandatory())}<span>₸</span></div>
      </div>
      <button class="br-cta ${depPending<=0?'done':''}" id="bMandPay">${depPending<=0?'✓ Обязательное пополнено':`Пополнить выбранное · ${money(depPending)} ₸`}</button>`;
    const pb = q('#bMandPay'); if(pb) pb.addEventListener('click', payMandatory);
    const free = freeMoney();
    const fe = q('#bFree');
    if(free>0) fe.innerHTML = `<span class="fi">🎯</span><div class="ft"><div class="a">Свободно ${money(free)} ₸</div><div class="b">после рекомендаций — можно отложить в цель</div></div><span class="fch">→</span>`;
    else if(free===0) fe.innerHTML = `<span class="fi">✅</span><div class="ft"><div class="a">Распределено полностью</div><div class="b">рекомендации укладываются в остаток</div></div>`;
    else fe.innerHTML = `<span class="fi" style="background:rgba(255,255,255,.2)">⚠️</span><div class="ft"><div class="a">Превышение на ${money(-free)} ₸</div><div class="b">рекомендации больше остатка — уменьшите лимиты</div></div>`;
    fe.classList.toggle('over', free<0);
    fe.onclick = () => { if(free>0) window.location.href='Maqsat & Family.html'; };
  }

  function confirmPlan(){
    const overs = RECO.filter(c=>c.limit < c.avg);
    const sum = q('#successSum');
    if(sum){
      sum.innerHTML = `<div class="sr hd"><span class="sk">Можно потратить · ${MONTH}</span><span class="sv">лимит</span></div>` +
        RECO.map(c=>`<div class="sr"><span>${c.icon} ${c.name}</span><span>${money(c.limit)} ₸</span></div>`).join('') +
        `<div class="sr tot"><span>Всего по рекомендациям</span><span>${money(sumReco())} ₸</span></div>`;
    }
    const hd = q('#successHd'); if(hd) hd.textContent = 'План рекомендаций на ' + MONTH;
    const tx = q('#successText'); if(tx) tx.textContent = overs.length
      ? `Счета не создаём — это ориентир. Если по «${overs[0].name}» выйдете за ${money(overs[0].limit)} ₸, пришлю уведомление, но платёж пройдёт.`
      : `Счета не создаём — это ориентир. При выходе за лимит любой категории пришлю уведомление, но платёж пройдёт.`;
    const ov = q('#success'); if(ov) ov.classList.add('show'); else toast('✅ План рекомендаций сохранён');
  }

  /* ── AI ASSISTANT CHAT (правит рекомендации) ──────────────── */
  let chatEl, msgs;
  const cat = id => RECO.find(c=>c.id===id);
  function adjust(id, delta){ const c=cat(id); if(c){ c.limit=Math.max(0,c.limit+delta); } }
  const QUICK = [
    { t:'Хочу меньше тратить на еду', fn:()=>{ adjust('food', -10000); return `Снизил рекомендацию по <b>Еде</b> до ${money(cat('food').limit)} ₸. Это +10 000 ₸ к свободным — можно отложить в цель. Подскажу выгодные покупки в HalykMarket 🛒`; } },
    { t:'Урезать такси', fn:()=>{ adjust('taxi', -3000); return `Рекомендация по <b>Такси</b> теперь ${money(cat('taxi').limit)} ₸, включу уведомление при превышении. Могу предложить каршеринг-партнёра со скидкой 🚗`; } },
    { t:'Больше откладывать на цель', fn:()=>{ adjust('food',-5000); adjust('fun',-4000); return `Освободил 9 000 ₸ из гибких категорий — свободно ${money(freeMoney())} ₸/мес. Срок до цели сократится 📉`; } },
    { t:'Где я перетрачиваю?', fn:()=>{ const o=RECO.find(c=>c.avg>c.limit); return o?`По <b>${o.name}</b> ваша средняя трата ${money(o.avg)} ₸ выше рекомендации ${money(o.limit)} ₸ — здесь чаще всего выходите за рамки.`:'По рекомендациям вы в среднем укладываетесь — хорошая дисциплина 👍'; } },
  ];

  function buildChat(){
    chatEl = document.createElement('div');
    chatEl.className = 'sheet';
    chatEl.id = 'aiChatSheet';
    chatEl.innerHTML = `
      <div class="grip"></div>
      <h3 style="display:flex;align-items:center;gap:9px;"><span style="width:30px;height:30px;border-radius:9px;background:var(--accent-grad);display:grid;place-items:center;font-size:15px;">🤖</span>AI-ассистент</h3>
      <p class="sh-sub" style="font-size:12.5px;color:var(--text2);margin-top:4px;">Опишите словами, что изменить — я пересоберу рекомендации.</p>
      <div class="chatbody" id="chatBody"></div>
      <div class="chatchips" id="chatChips"></div>
      <div class="chatinput"><input id="chatIn" placeholder="Напишите сообщение…" /><button id="chatSend">➤</button></div>`;
    q('#app').appendChild(chatEl);
    q('#chatSend').addEventListener('click', sendFree);
    q('#chatIn').addEventListener('keydown', e=>{ if(e.key==='Enter') sendFree(); });
  }
  function renderChips(){
    q('#chatChips').innerHTML = QUICK.map((qk,i)=>`<button class="chatchip" data-i="${i}">${qk.t}</button>`).join('');
    qa('#chatChips .chatchip').forEach(b => b.addEventListener('click', ()=>{
      const qk = QUICK[+b.dataset.i];
      pushMsg('me', qk.t);
      const reply = qk.fn();
      setTimeout(()=> pushMsg('bot', reply), 450);
    }));
  }
  function pushMsg(who, text){
    const body = q('#chatBody');
    const d = document.createElement('div');
    d.className = 'msg '+who; d.innerHTML = text;
    body.appendChild(d); body.scrollTop = body.scrollHeight;
  }
  function sendFree(){
    const inp = q('#chatIn'); const v = inp.value.trim(); if(!v) return;
    pushMsg('me', v); inp.value='';
    setTimeout(()=> pushMsg('bot','Понял вас. Подстроил рекомендации под этот запрос — закройте чат, чтобы увидеть обновлённые лимиты 👍'), 500);
  }
  function openChat(){
    if(!chatEl) buildChat();
    q('#chatBody').innerHTML = '';
    pushMsg('bot','Привет! Я веду ваш бюджет. Скажите, что поправить в рекомендациях — например «хочу меньше на еду» или «урезать такси».');
    renderChips();
    window.openSheet(chatEl);
    const scrim = q('#scrim');
    const onClose = () => { if(state==='manage') viewManage(); scrim.removeEventListener('click', onClose); };
    scrim.addEventListener('click', onClose);
  }

  /* ── boot ─────────────────────────────────────────────────── */
  const isOnboarded = () => { try { return localStorage.getItem('halyk_onboarded')==='1'; } catch(e){ return false; } };
  let inited=false;
  function initBudget(){
    if(inited) return; inited=true;
    if(isOnboarded()){ state='manage'; viewManage(); }
    else viewOnboard();
  }
  window.initBudget = initBudget;
  // Called by the first-run wizard once Face ID + распределение завершены.
  window.budgetGoManage = () => { inited=true; MANDATORY.forEach(c=>c.paid=true); state='manage'; viewManage(); };
  window.budgetShowIntro = () => { inited=true; state='onboard'; resetState(); viewOnboard(); };
  if(document.readyState!=='loading') initBudget();
  else document.addEventListener('DOMContentLoaded', initBudget);
})();
