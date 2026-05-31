/* ════════════════════════════════════════════════════════════
   Halyk Maqsat — AI-рекомендации (сторис)
   4 ленты, сгенерированные по частым транзакциям пользователя.
   Открывается кнопкой «Посмотреть рекомендации [4]» в «Мои финансы».
   Instagram-style: сегментные прогресс-бары, тап влево/вправо,
   удержание — пауза, ✕ — закрыть. window.openRecoStories()
   ════════════════════════════════════════════════════════════ */
(function(){
  const DUR = 7000; // мс на одну ленту

  // Ленты-рекомендации по частым тратам папы
  const STORIES = [
    {
      id:'magnum',
      grad:'linear-gradient(165deg,#13A981 0%,#00815F 55%,#005A42 100%)',
      icon:'🛒', eyebrow:'ЧАСТЫЕ ТРАТЫ · ПРОДУКТЫ',
      stat:'14 покупок в Magnum за месяц',
      big:'88 000', unit:'₸', bigsub:'тратите на продукты',
      title:'Платите картой Halyk в Magnum',
      body:'AI заметил: продукты — ваша самая частая трата. Включите кешбэк и возвращайте <b>5% бонусами</b> с каждой покупки.',
      pill:'≈ 4 400 ₸ возвращается каждый месяц',
      cta:'Включить Magnum-кешбэк'
    },
    {
      id:'taxi',
      grad:'linear-gradient(165deg,#F8BE3C 0%,#F1A400 48%,#D98600 100%)',
      icon:'🚕', eyebrow:'ЧАСТЫЕ ТРАТЫ · ТРАНСПОРТ',
      stat:'21 поездка Yandex Go в этом месяце',
      big:'21 000', unit:'₸', bigsub:'на такси',
      title:'Подписка Halyk + Yandex Go',
      body:'Вы ездите почти каждый день. С подпиской возвращаем <b>10%</b> за каждую поездку — окупается уже с 4-й.',
      pill:'≈ 2 100 ₸/мес экономии',
      cta:'Оформить подписку',
      dark:true
    },
    {
      id:'subs',
      grad:'linear-gradient(165deg,#1C4D8F 0%,#123B73 52%,#0C2A55 100%)',
      icon:'🔁', eyebrow:'AI НАШЁЛ СКРЫТЫЕ ПЛАТЕЖИ',
      stat:'3 автоплатежа: Netflix · Spotify · iCloud',
      big:'78 000', unit:'₸', bigsub:'в год уходит незаметно',
      title:'Соберите подписки в Halyk',
      body:'Один счёт вместо трёх: видите все списания, отключаете в один тап и получаете <b>3% кешбэка</b>.',
      pill:'6 500 ₸/мес под контролем',
      cta:'Управлять подписками'
    },
    {
      id:'fuel',
      grad:'linear-gradient(165deg,#0E5A43 0%,#0A4534 60%,#063425 100%)',
      icon:'⛽', eyebrow:'ЧАСТЫЕ ТРАТЫ · АВТО',
      stat:'4 заправки на Helios за месяц',
      big:'26 000', unit:'₸', bigsub:'на топливо',
      title:'Карта Halyk × Helios',
      body:'Заправляетесь всегда на Helios. Совместная карта даёт <b>7% на топливо</b> и приоритет на АЗС-партнёрах.',
      pill:'≈ 1 800 ₸/мес · 21 600 ₸/год',
      cta:'Заказать карту'
    }
  ];

  let root, idx=0, timer=null, startT=0, elapsed=0, paused=false;

  function injectCSS(){
    if(document.getElementById('rsCSS')) return;
    const s=document.createElement('style'); s.id='rsCSS';
    s.textContent=`
    .rs{ position:absolute; inset:0; z-index:140; display:none; flex-direction:column;
      color:#fff; overflow:hidden; font-family:var(--ff); }
    .rs.show{ display:flex; animation:rsIn .28s ease both; }
    @keyframes rsIn{ from{ opacity:0; transform:scale(1.03); } to{ opacity:1; transform:none; } }
    .rs::before{ content:""; position:absolute; inset:0; opacity:.5; pointer-events:none;
      background:radial-gradient(120% 55% at 18% 12%, rgba(255,255,255,.18), transparent 60%),
                 radial-gradient(90% 45% at 92% 95%, rgba(0,0,0,.22), transparent 60%); }
    .rs-bars{ position:relative; z-index:5; display:flex; gap:5px; padding:calc(12px + env(safe-area-inset-top)) 14px 0; }
    .rs-bars .seg{ flex:1; height:3px; border-radius:3px; background:rgba(255,255,255,.32); overflow:hidden; }
    .rs-bars .seg i{ display:block; height:100%; width:0; border-radius:3px; background:#fff; }
    .rs-bars .seg.done i{ width:100%; }
    .rs-bars .seg.active i{ transition:width linear; }
    .rs-head{ position:relative; z-index:5; display:flex; align-items:center; gap:9px; padding:13px 16px 0; }
    .rs-brand{ display:flex; align-items:center; gap:8px; font-size:12.5px; font-weight:800; letter-spacing:-.01em; }
    .rs-brand .bot{ width:26px; height:26px; border-radius:8px; background:rgba(255,255,255,.2); display:grid; place-items:center; font-size:14px; }
    .rs-brand .cnt{ font-weight:600; opacity:.8; font-size:11.5px; }
    .rs-x{ margin-left:auto; width:32px; height:32px; border:none; border-radius:50%; background:rgba(255,255,255,.16);
      color:#fff; font-size:16px; cursor:pointer; display:grid; place-items:center; -webkit-tap-highlight-color:transparent; }
    .rs-x:active{ transform:scale(.9); }

    .rs-slide{ position:relative; z-index:2; flex:1; display:flex; flex-direction:column; justify-content:center;
      padding:0 26px; gap:0; }
    .rs-icon{ width:74px; height:74px; border-radius:22px; background:rgba(255,255,255,.16); display:grid; place-items:center;
      font-size:38px; box-shadow:0 10px 30px rgba(0,0,0,.18); }
    .rs-eyebrow{ margin-top:24px; font-size:11px; font-weight:900; letter-spacing:.12em; opacity:.82; }
    .rs-stat{ margin-top:9px; font-size:14.5px; font-weight:600; opacity:.95; line-height:1.35; }
    .rs-big{ margin-top:20px; font-size:60px; font-weight:800; letter-spacing:-.035em; line-height:.95; font-variant-numeric:tabular-nums; }
    .rs-big span{ font-size:30px; opacity:.8; margin-left:4px; letter-spacing:0; }
    .rs-bigsub{ font-size:13px; font-weight:600; opacity:.8; margin-top:6px; }
    .rs-title{ margin-top:26px; font-size:23px; font-weight:800; letter-spacing:-.02em; line-height:1.18; text-wrap:balance; }
    .rs-body{ margin-top:11px; font-size:14px; font-weight:500; line-height:1.5; opacity:.94; max-width:340px; }
    .rs-body b{ font-weight:800; }
    .rs-pill{ display:inline-flex; align-items:center; gap:8px; align-self:flex-start; margin-top:20px;
      background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.28); border-radius:30px;
      padding:9px 15px; font-size:13px; font-weight:800; letter-spacing:-.01em; }
    .rs-pill .dt{ width:7px; height:7px; border-radius:50%; background:#fff; box-shadow:0 0 0 4px rgba(255,255,255,.22); }

    .rs-foot{ position:relative; z-index:5; padding:14px 18px calc(18px + env(safe-area-inset-bottom)); }
    .rs-cta{ width:100%; border:none; border-radius:15px; padding:16px; cursor:pointer; font-family:var(--ff);
      font-size:15px; font-weight:800; letter-spacing:-.01em; background:#fff; transition:transform .12s;
      box-shadow:0 10px 26px rgba(0,0,0,.2); -webkit-tap-highlight-color:transparent; }
    .rs-cta:active{ transform:scale(.98); }
    .rs-skip{ display:block; width:100%; margin-top:10px; background:none; border:none; color:rgba(255,255,255,.82);
      font-family:var(--ff); font-size:12.5px; font-weight:700; cursor:pointer; -webkit-tap-highlight-color:transparent; }

    /* tap zones (под хедером/футером/CTA) */
    .rs-tap{ position:absolute; top:64px; bottom:120px; width:34%; z-index:3; background:none; border:none;
      cursor:pointer; -webkit-tap-highlight-color:transparent; }
    .rs-tap.left{ left:0; }
    .rs-tap.right{ right:0; left:auto; width:66%; }

    .rs-anim{ animation:rsContent .4s cubic-bezier(.2,.9,.3,1) both; }
    @keyframes rsContent{ from{ opacity:0; transform:translateY(12px); } to{ opacity:1; transform:none; } }
    `;
    document.head.appendChild(s);
  }

  function build(){
    injectCSS();
    root=document.createElement('div');
    root.className='rs'; root.id='rsRoot';
    root.innerHTML=`
      <div class="rs-bars" id="rsBars"></div>
      <div class="rs-head">
        <div class="rs-brand"><span class="bot">🤖</span>AI-рекомендации<span class="cnt" id="rsCnt"></span></div>
        <button class="rs-x" id="rsClose" aria-label="Закрыть">✕</button>
      </div>
      <button class="rs-tap left"  id="rsPrev" aria-label="Назад"></button>
      <button class="rs-tap right" id="rsNext" aria-label="Дальше"></button>
      <div class="rs-slide" id="rsSlide"></div>
      <div class="rs-foot">
        <button class="rs-cta" id="rsCta"></button>
        <button class="rs-skip" id="rsSkip">Пропустить всё</button>
      </div>`;
    document.getElementById('app').appendChild(root);

    root.querySelector('#rsClose').addEventListener('click', close);
    root.querySelector('#rsSkip').addEventListener('click', close);
    root.querySelector('#rsPrev').addEventListener('click', ()=>go(idx-1));
    root.querySelector('#rsNext').addEventListener('click', ()=>go(idx+1));
    root.querySelector('#rsCta').addEventListener('click', onCta);
    // hold-to-pause on the slide content
    const slide=root.querySelector('#rsSlide');
    slide.addEventListener('pointerdown', pause);
    slide.addEventListener('pointerup', resume);
    slide.addEventListener('pointerleave', resume);
  }

  function renderBars(){
    const wrap=root.querySelector('#rsBars');
    wrap.innerHTML=STORIES.map((_,i)=>`<div class="seg ${i<idx?'done':''} ${i===idx?'active':''}"><i></i></div>`).join('');
  }

  function renderSlide(){
    const st=STORIES[idx];
    root.style.background=st.grad;
    root.querySelector('#rsCnt').textContent=`· ${idx+1} из ${STORIES.length}`;
    const slide=root.querySelector('#rsSlide');
    slide.innerHTML=`
      <div class="rs-anim">
        <div class="rs-icon">${st.icon}</div>
        <div class="rs-eyebrow">${st.eyebrow}</div>
        <div class="rs-stat">${st.stat}</div>
        <div class="rs-big">${st.big}<span>${st.unit}</span></div>
        <div class="rs-bigsub">${st.bigsub}</div>
        <div class="rs-title">${st.title}</div>
        <div class="rs-body">${st.body}</div>
        <div class="rs-pill"><span class="dt"></span>${st.pill}</div>
      </div>`;
    const cta=root.querySelector('#rsCta');
    cta.textContent=st.cta;
    cta.style.color=st.dark?'#7A4F00':'var(--green-d)';
  }

  function startTimer(remaining){
    const seg=root.querySelector('#rsBars .seg.active i');
    const total=DUR;
    elapsed = total - remaining;
    startT=performance.now();
    if(seg){
      // jump to current elapsed, then animate to full over remaining
      seg.style.transition='none';
      seg.style.width=(elapsed/total*100)+'%';
      // force reflow
      void seg.offsetWidth;
      seg.style.transition=`width ${remaining}ms linear`;
      seg.style.width='100%';
    }
    clearTimeout(timer);
    timer=setTimeout(()=>go(idx+1), remaining);
  }

  function go(n){
    if(n<0) n=0;
    if(n>=STORIES.length){ close(); return; }
    idx=n; paused=false;
    renderBars(); renderSlide();
    startTimer(DUR);
  }

  function pause(){
    if(paused) return;
    paused=true;
    clearTimeout(timer);
    const seg=root.querySelector('#rsBars .seg.active i');
    if(seg){
      const w=getComputedStyle(seg).width;
      seg.style.transition='none';
      seg.style.width=w; // freeze
    }
    elapsed += performance.now()-startT;
  }
  function resume(){
    if(!paused) return;
    paused=false;
    startTimer(Math.max(400, DUR-elapsed));
  }

  function onCta(){
    const st=STORIES[idx];
    clearTimeout(timer);
    if(window.showToast) window.showToast(`✅ ${st.cta} — подключаю`);
    // mild delay then move on
    setTimeout(()=>go(idx+1), 250);
  }

  function close(){
    clearTimeout(timer);
    root.classList.remove('show');
  }

  function open(){
    if(!root) build();
    idx=0; paused=false; elapsed=0;
    root.classList.add('show');
    go(0);
  }

  window.openRecoStories = open;
  window.recoStoriesCount = STORIES.length;
})();
