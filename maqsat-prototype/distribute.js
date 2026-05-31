/* ════════════════════════════════════════════════════════════
   Maqsat & Family — salary distribution session (Папа)
   Two modes:
     • first  — onboarding: agent learns the family, step-by-step
     • power  — "ярый": agent knows everyone, NBA, dense, fast
   Card mechanic: Открыть → Привязать квитанции → Пополнить
   + running counter (decrements) + Face ID commit.
   ════════════════════════════════════════════════════════════ */
(function(){
  const $  = s => document.querySelector(s);
  const $$ = s => Array.from(document.querySelectorAll(s));
  const fmt = window.fmt || (n => Math.round(n).toLocaleString('ru-RU'));
  const parseAmt = window.parseAmt || (v => parseInt(String(v).replace(/\D/g,'')) || 0);
  const toast = (m) => window.showToast && window.showToast(m);
  const pl = (n,a,b,c) => { const m=n%100,d=n%10; if(m>=11&&m<=14)return c; if(d===1)return a; if(d>=2&&d<=4)return b; return c; };

  const U = '🧾';
  const rc = (e,a) => ({ e, a, on:true });

  /* ── Scenario library ─────────────────────────────────────── */
  const SCEN = {
    first: {
      salary: 223000,
      cap: 'Демо · как агент ведёт нового пользователя',
      subt: 'Первая зарплата · агент изучает траты',
      hero: {
        greet: 'Похоже, это ваша <b>первая зарплата</b> в Maqsat',
        tagline: 'Я изучил траты за 3 месяца и подобрал план. Деньги останутся на вашем счёте — спишутся только после Face ID.',
        pill: 'Проанализировано 184 операции · 3 месяца',
        cta: 'Разобрать зарплату вместе →',
        nba: false,
      },
      intro: 'Привет, Асхат! Это ваша первая зарплата здесь. Я нашёл <b>3 регулярных платежа</b> — откроем под них карты-конверты. По одной: <b>открыть → привязать квитанции → пополнить</b>.',
      startOpened: false,
      bulk: false,
      cards: [
        { id:'util', group:'Обязательные платежи', icon:'🏠', name:'Коммуналка и квитанции', num:'7781',
          type:'deposit', rate:'депозитная · 14%', def:32590,
          receipts:[ rc('💡','Свет · АлматыЭнерго'), rc('🔥','Газ · QazaqGaz'), rc('💧','Вода · Су Арнасы') ] },
        { id:'fuel', group:'Обязательные платежи', icon:'⛽', gold:true, name:'Бензин', num:'7902',
          type:'deposit', rate:'депозитная · 14%', def:27000, receipts:null },
        { id:'auto', group:'Обязательные платежи', icon:'🔧', name:'Авто-резерв · СТО', num:'4419',
          type:'savings', rate:'сберегательная · 16.5%', def:16000,
          receipts:[ rc('🔧','СТО — диагностика · раз в 6 мес'), rc('🛢️','Замена масла'), rc('🛡️','Страховка ОСГПО') ] },
        { id:'dorm', group:'Агент уточняет', kind:'clarify', icon:'🎓', amount:80000,
          name:'TOO SDU DORM', sub:'Регулярно · 80 000 ₸ каждый месяц',
          question:'Вижу регулярный платёж <b>80 000 ₸</b> на «TOO SDU DORM» каждый месяц. Что это за расход?',
          chips:[
            { label:'🏠 Общежитие сына', result:'Общежитие · сын Жанеке' },
            { label:'🏢 Аренда жилья', result:'Аренда жилья' },
            { label:'✏️ Другое', result:'Другой регулярный платёж' },
          ] },
        { id:'choice', group:'Остаток зарплаты', kind:'choice',
          question:'Останется <b id="choiceLeft">147 410 ₸</b>. Пополнить годовые расходы или создадим цель — куда-нибудь съездить?',
          options:[
            { e:'📑', a:'Годовые расходы', b:'Налог на землю и авто, страховка',
              add:{ id:'annual', group:'Годовые расходы', icon:'📑', name:'Годовой резерв', num:'5540',
                    type:'savings', rate:'сберегательная · 16.5%', def:90000,
                    receipts:[ rc('🌍','Налог на землю'), rc('🚗','Налог на авто'), rc('🛡️','Страховка КАСКО') ] } },
            { e:'🏖️', a:'Финансовая цель', b:'Накопить на поездку',
              add:{ id:'trip', group:'Цель', icon:'🏖️', gold:true, name:'Цель · поездка', num:'2026',
                    type:'savings', rate:'сберегательная · 16.5%', def:120000, receipts:null, goal:true } },
          ] },
      ],
    },

    power: {
      salary: 470000,
      cap: 'Демо · агент уже знает семью и привычки',
      subt: 'Зарплата пришла · план готов как обычно',
      hero: {
        greet: 'Доброе утро, <b>Асхат</b> 👋',
        tagline: 'Зарплата пришла. План распределения готов как обычно — плюс одно предложение. Подтвердите по Face ID.',
        pill: 'NBA готов · 10 карт · 3 ребёнка',
        cta: 'Распределить как обычно →',
        nba: true,
      },
      intro: 'План на этот месяц готов. Карты уже открыты — осталось пополнить. Можно <b>пополнить всё разом</b> или поправить любую сумму.',
      startOpened: true,
      bulk: true,
      nba: {
        flag:'NBA · проактивное предложение',
        title:'Год назад вы летали в Баку в эти даты ✈️',
        body:'Сейчас билеты дешевле на 12%. Заложить остаток на семейную цель «Баку 2026»?',
        yes:'Создать цель «Баку»', no:'Не сейчас',
        add:{ id:'baku', group:'Семейная цель', icon:'🏖️', gold:true, name:'Баку 2026', num:'2026',
              type:'savings', rate:'сберегательная · 16.5%', def:200410, receipts:null, goal:true },
      },
      cards: [
        { id:'util', group:'Обязательные платежи', icon:'🏠', name:'Коммуналка и квитанции', num:'7781',
          type:'deposit', rate:'депозитная · 14%', def:32590,
          receipts:[ rc('💡','Свет'), rc('🔥','Газ'), rc('💧','Вода') ] },
        { id:'fuel', group:'Обязательные платежи', icon:'⛽', name:'Бензин', num:'7902',
          type:'deposit', rate:'депозитная · 14%', def:27000, receipts:null },
        { id:'auto', group:'Обязательные платежи', icon:'🔧', name:'Авто-резерв · СТО', num:'4419',
          type:'savings', rate:'сберегательная · 16.5%', def:16000,
          receipts:[ rc('🔧','Диагностика'), rc('🛢️','Замена масла') ] },
        { id:'pkt-zh', group:'Карманные детям', icon:'👧🏻', name:'Жанеке · карманные', num:'6201',
          type:'deposit', rate:'детская · лимит', def:20000, receipts:null },
        { id:'pkt-am', group:'Карманные детям', icon:'🧒🏻', name:'Аминош · карманные', num:'6202',
          type:'deposit', rate:'детская · лимит', def:12000, receipts:null },
        { id:'pkt-md', group:'Карманные детям', icon:'👦🏻', name:'Мадеке · карманные', num:'6203',
          type:'deposit', rate:'детская · лимит', def:15000, receipts:null },
        { id:'dance', group:'Образование и кружки', icon:'💃', gold:true, name:'Танцы · Аминош', num:'4810',
          type:'deposit', rate:'депозитная · 14%', def:24000, receipts:[ rc('💃','Студия «Алма» · абонемент') ] },
        { id:'eng', group:'Образование и кружки', icon:'🇬🇧', name:'Английский · Мадеке', num:'4811',
          type:'deposit', rate:'депозитная · 14%', def:16000, receipts:[ rc('🇬🇧','Курсы · ежемесячно') ] },
        { id:'dorm', group:'Образование и кружки', icon:'🎓', name:'Общежитие · Жанеке', num:'4812',
          type:'deposit', rate:'депозитная · 14%', def:80000, receipts:[ rc('🎓','TOO SDU DORM · авто') ] },
        { id:'save', group:'Накопления', icon:'💎', gold:true, name:'Saving plan', num:'9001',
          type:'savings', rate:'сберегательная · 16.5%', def:27000, receipts:null },
      ],
    },
  };

  let MODE = 'first';
  let PLAN = [];
  const cur = () => SCEN[MODE];

  /* ── Working-state init ───────────────────────────────────── */
  function buildPlan(){
    const s = cur();
    PLAN = s.cards.map(c => {
      const o = Object.assign({}, c);
      if(o.receipts) o.receipts = o.receipts.map(r => Object.assign({}, r));
      o.amount = o.def != null ? o.def : 0;
      if(o.kind === 'clarify'){ o.status = 'clarify'; }
      else if(o.kind === 'choice'){ o.status = 'choice'; }
      else { o.status = s.startOpened ? 'opened' : 'new'; o.linked = !o.receipts; o.expanded = false; }
      return o;
    });
  }

  const fundable = () => PLAN.filter(c => !c.kind || c.kind === 'card');
  const remaining = () => cur().salary - fundable().filter(c => c.status === 'funded').reduce((s,c)=>s+c.amount,0);

  /* ── Action zone per fundable card (state machine) ────────── */
  function actHTML(c){
    if(c.status === 'new')
      return `<button class="dc-mini ghost" data-a="open">＋ Открыть карту</button>`;
    if(c.status === 'funded')
      return `<div class="dc-done"><span class="ck">✓</span>
        <span class="dt">Пополнено · ${fmt(c.amount)} ₸</span>
        <button class="cancel" data-a="cancel">Отменить</button></div>`;
    if(c.receipts && !c.linked){
      if(!c.expanded)
        return `<button class="dc-mini gold" data-a="expand">🧾 Привязать квитанции</button>`;
      const rows = c.receipts.map((r,i) => `
        <div class="rcpt${r.on?' on':''}" data-r="${i}"><span class="re">${r.e}</span>
          <div class="ri"><div class="a">${r.a}</div></div>
          <div class="rc"><svg viewBox="0 0 24 24" fill="none"><path d="M5 13l4 4L19 7" stroke-linecap="round" stroke-linejoin="round"/></svg></div>
        </div>`).join('');
      const anyOn = c.receipts.some(r => r.on);
      return `<div class="dc-receipts"><div class="dc-rlbl">Автосписания на эту карту</div>${rows}
        <button class="dc-mini prim" data-a="link" ${anyOn?'':'disabled'} style="margin-top:2px;">Привязать выбранное</button></div>`;
    }
    const over = c.amount > remaining();
    return `<button class="dc-mini prim" data-a="fund" ${over?'disabled':''}>${
      over ? 'Превышает остаток' : (c.goal ? 'Отложить в цель · ' : 'Пополнить · ') + fmt(c.amount) + ' ₸'}</button>`;
  }

  /* ── Render one card's inner (by kind) ────────────────────── */
  function cardInner(c){
    if(c.kind === 'clarify'){
      if(c.status === 'clarified'){
        return `<div class="dc-top">
            <div class="dc-ic">${c.icon}</div>
            <div class="dc-i"><div class="a">${c.name}</div><div class="b">${c.answer} · авто-оплата</div></div>
            <div class="dc-amt"><span style="font-size:15px;font-weight:800;">${fmt(c.amount)}</span><span class="c">₸</span></div>
          </div>
          <div class="dc-act"><div class="dc-done"><span class="ck">✓</span><span class="dt">Понятно — уже оплачивается автоматически</span></div></div>`;
      }
      const chips = c.chips.map((q,i)=>`<button class="qchip" data-q="${i}">${q.label}</button>`).join('');
      return `<div class="clar-q"><span class="cq-bot">🤖</span><span class="cq-tx">${c.question}</span></div>
        <div class="qchips">${chips}</div>`;
    }
    if(c.kind === 'choice'){
      if(c.status === 'chosen') return `<div class="ch-h"><span class="ch-bot">🤖</span>
        <span class="ch-tx">Добавил <b>«${c.chosenName}»</b> — пополните карту ниже 👇</span></div>`;
      const opts = c.options.map((o,i)=>`<button class="choiceopt" data-o="${i}">
        <span class="co-e">${o.e}</span><span class="co-a">${o.a}</span><span class="co-b">${o.b}</span></button>`).join('');
      return `<div class="ch-h"><span class="ch-bot">🤖</span><span class="ch-tx">${c.question}</span></div>
        <div class="choicegrid">${opts}</div>`;
    }
    // normal fundable card
    return `<div class="dc-top">
        <div class="dc-ic">${c.icon}</div>
        <div class="dc-i"><div class="a">${c.name}</div><div class="b">•••• ${c.num} · ${c.rate}</div></div>
        <div class="dc-amt"><input data-id="${c.id}" inputmode="numeric" value="${fmt(c.amount)}" aria-label="Сумма" ${c.status==='funded'?'disabled':''} /><span class="c">₸</span></div>
      </div>
      <div class="dc-act">${actHTML(c)}</div>`;
  }

  function cardClass(c){
    let k = 'distcard';
    if(c.kind === 'clarify') k += ' clarify';
    if(c.kind === 'choice') return 'choicecard';
    if(c.gold) k += ' t-gold';
    if(c.status === 'funded') k += ' funded';
    return k;
  }

  function renderCard(c){
    const el = $(`[data-id="${c.id}"]`);
    if(el){ el.className = cardClass(c); el.innerHTML = cardInner(c); bindCard(c); }
  }

  function bindCard(c){
    const card = $(`[data-id="${c.id}"]`); if(!card) return;
    // fundable actions
    card.querySelectorAll('.dc-act [data-a]').forEach(b => b.addEventListener('click', ()=>onAction(c,b.dataset.a)));
    card.querySelectorAll('[data-r]').forEach(row => row.addEventListener('click', ()=>{ c.receipts[+row.dataset.r].on = !c.receipts[+row.dataset.r].on; renderCard(c); }));
    // clarify chips
    card.querySelectorAll('.qchip').forEach(b => b.addEventListener('click', ()=>{
      const q = c.chips[+b.dataset.q]; c.answer = q.result; c.status='clarified'; toast('🤖 Записал, спасибо'); renderCard(c);
    }));
    // choice options
    card.querySelectorAll('.choiceopt').forEach(b => b.addEventListener('click', ()=>{
      const o = c.options[+b.dataset.o]; c.status='chosen'; c.chosenName=o.add.name;
      const card2 = Object.assign({}, o.add);
      if(card2.receipts) card2.receipts = card2.receipts.map(r=>Object.assign({},r));
      card2.amount = card2.def; card2.status = cur().startOpened?'opened':'new'; card2.linked = !card2.receipts; card2.expanded=false;
      const ci = PLAN.indexOf(c); PLAN.splice(ci+1,0,card2);
      toast('➕ Карта добавлена'); render(); updateCounter();
    }));
    // amount input (fundable only)
    const inp = card.querySelector('.dc-amt input[data-id]');
    if(inp){
      inp.addEventListener('focus', ()=>{ inp.value=String(c.amount); inp.select(); });
      inp.addEventListener('input', ()=>{ c.amount=parseAmt(inp.value); const a=card.querySelector('.dc-act'); if(a){a.innerHTML=actHTML(c); a.querySelectorAll('[data-a]').forEach(b=>b.addEventListener('click',()=>onAction(c,b.dataset.a)));} });
      inp.addEventListener('blur', ()=>{ inp.value=fmt(c.amount); });
    }
  }

  function onAction(c,a){
    if(a==='open'){ c.status='opened'; toast(`💳 Карта «${c.name}» открыта`); renderCard(c); }
    else if(a==='expand'){ c.expanded=true; renderCard(c); }
    else if(a==='link'){ c.linked=true; toast('🔗 Квитанции привязаны'); renderCard(c); }
    else if(a==='fund'){ if(c.amount>remaining())return; c.status='funded'; toast(`✅ ${c.goal?'Отложено':'Пополнено'} · ${fmt(c.amount)} ₸`); renderCard(c); updateCounter(); refreshFundable(); }
    else if(a==='cancel'){ c.status='opened'; renderCard(c); updateCounter(); refreshFundable(); }
  }

  function refreshFundable(){ fundable().forEach(c=>{ if(c.status==='opened'||c.status==='funded') renderCard(c); }); }

  /* ── Counter / summary ────────────────────────────────────── */
  function updateCounter(){
    const r = remaining(), moved = cur().salary - r;
    $('#distLeft').textContent = fmt(r);
    $('#distBarI').style.width = Math.max(0,Math.min(100,moved/cur().salary*100))+'%';
    $('#distMovedLbl').textContent = 'Распределено '+fmt(moved)+' ₸';
    $('#distCounter').classList.toggle('over', r<0);
    const cl = $('#choiceLeft'); if(cl) cl.textContent = fmt(r)+' ₸';
    const funded = fundable().filter(c=>c.status==='funded');
    $('#distCount').textContent = funded.length;
    $('#distTotal').textContent = fundable().length;
    $('#distConfirm').disabled = funded.length===0;
    $('#distSumm').textContent = funded.length
      ? `К списанию ${fmt(moved)} ₸ · остаток ${fmt(r)} ₸`
      : 'Откройте и пополните карты под цели';
  }

  /* ── Full list render (groups + intro + nba + bulk) ───────── */
  function render(){
    const s = cur();
    let html = `<div class="agent-intro"><span class="ai-bot">🤖</span><span class="ai-tx">${s.intro}
      <span class="tiny">Зарплата ${fmt(s.salary)} ₸ · остаётся на вашем счёте до Face ID</span></span></div>`;

    if(s.nba && !window.__nbaDismissed){
      const n = s.nba;
      html += `<div class="nbacard" id="nbaCard">
        <div class="nb-flag"><span class="live"></span>${n.flag}</div>
        <div class="nb-t">${n.title}</div>
        <div class="nb-b">${n.body}</div>
        <div class="nb-btns"><button class="yes" data-nba="yes">${n.yes}</button><button class="no" data-nba="no">${n.no}</button></div>
      </div>`;
    }

    if(s.bulk){
      html += `<button class="dc-mini prim" id="fundAll" style="padding:13px;">⚡ Пополнить всё разом</button>`;
    }

    let lastGroup = null;
    PLAN.forEach(c => {
      if(c.group && c.group !== lastGroup){ html += `<div class="dist-group">${c.group}</div>`; lastGroup = c.group; }
      html += `<div class="${cardClass(c)}" data-id="${c.id}">${cardInner(c)}</div>`;
    });

    html += `<div class="dist-note">Деньги физически остаются на счёте Асхата. Списание — только после Face ID. Виртуальный слой (middleware) с маской лимитов через API.</div>`;
    $('#distList').innerHTML = html;

    PLAN.forEach(bindCard);
    const na = $('#nbaCard');
    if(na) na.querySelectorAll('[data-nba]').forEach(b => b.addEventListener('click', ()=>onNBA(b.dataset.nba)));
    const fa = $('#fundAll'); if(fa) fa.addEventListener('click', fundAll);
  }

  function onNBA(v){
    const n = cur().nba;
    if(v==='yes'){
      const card = Object.assign({}, n.add); card.amount=card.def; card.status=cur().startOpened?'opened':'new'; card.linked=!card.receipts; card.expanded=false;
      PLAN.push(card); toast('🏖️ Цель «Баку 2026» добавлена');
    } else { toast('Хорошо, напомню позже'); }
    window.__nbaDismissed = true; render(); updateCounter();
  }

  function fundAll(){
    let r = remaining();
    fundable().forEach(c => {
      if(c.status!=='funded' && c.amount<=r){ c.status='funded'; r-=c.amount; }
    });
    toast('⚡ Карты пополнены'); render(); updateCounter();
  }

  /* ── Mode + hero ──────────────────────────────────────────── */
  function renderHero(){
    const h = cur().hero;
    $('#salHero').innerHTML = `
      ${h.nba ? '<div class="nba-flag">⚡ NBA готов</div>' : ''}
      <div class="sh-greet">${h.greet}</div>
      <div class="sh-amt"><span class="pos">+${fmt(cur().salary)}</span> <span class="c">₸</span></div>
      <div class="sh-top" style="margin-top:11px;">
        <span class="bot">🤖</span>
        <div class="sh-t"><div class="b">${h.tagline}</div></div>
      </div>
      <div class="agent-pill"><span class="live"></span>${h.pill}</div>
      <button class="btn btn-primary sh-cta" id="openDistribute">${h.cta}</button>`;
    $('#openDistribute').addEventListener('click', openDistribute);
  }

  function setMode(m){
    MODE = m;
    $$('#modeSwitch button').forEach(b => b.dataset.on = b.dataset.mode===m ? '1':'0');
    $('#modeCap').textContent = cur().cap;
    renderHero();
  }

  /* ── Open / close overlay ─────────────────────────────────── */
  function openDistribute(){
    window.__nbaDismissed = false;
    buildPlan();
    $('#distSubt').textContent = cur().subt;
    $('#distSalLbl').textContent = 'Зарплата '+fmt(cur().salary)+' ₸';
    render(); updateCounter();
    $('#distList').scrollTop = 0;
    $('#distribute').classList.add('show');
  }
  function closeDistribute(){ $('#distribute').classList.remove('show'); }

  /* ── Face ID → commit ─────────────────────────────────────── */
  function runFaceID(){
    const f = $('#faceid'); f.classList.remove('done');
    $('#fiText').textContent='Сканирование лица…';
    $('#fiSub').textContent='Подтвердите распределение зарплаты с помощью Face ID';
    f.classList.add('show');
    setTimeout(()=>{ f.classList.add('done'); $('#fiText').textContent='Лицо распознано'; $('#fiSub').textContent='Идентификация подтверждена'; },1900);
    setTimeout(()=>{ f.classList.remove('show'); commit(); },2850);
  }
  function commit(){
    const funded = fundable().filter(c=>c.status==='funded');
    const moved = funded.reduce((s,c)=>s+c.amount,0);
    closeDistribute();
    if(window.VCARDS && window.renderVCards){
      funded.forEach(c=>{ if(!window.VCARDS.find(v=>v.num===c.num))
        window.VCARDS.push({ name:c.name.split(' · ')[0], num:c.num, cls:c.gold?'gold':'', type:c.rate, bal:fmt(c.amount), linked:!!c.linked }); });
      window.renderVCards();
    }
    setTimeout(()=>{ window.showSuccess('Зарплата распределена',
      `${funded.length} ${pl(funded.length,'карта','карты','карт')} пополнены на ${fmt(moved)} ₸. На счёте осталось ${fmt(cur().salary-moved)} ₸.`); },320);
  }

  /* ── Wire ─────────────────────────────────────────────────── */
  function bind(){
    $$('#modeSwitch button').forEach(b => b.addEventListener('click', ()=>setMode(b.dataset.mode)));
    const x=$('#distClose'); if(x) x.addEventListener('click', closeDistribute);
    const conf=$('#distConfirm'); if(conf) conf.addEventListener('click', ()=>{ if(!conf.disabled) runFaceID(); });
    setMode('first');
  }

  window.openDistribute = openDistribute;
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded', bind);
  else bind();
})();
