/* ════════════════════════════════════════════════════════════
   Maqsat & Family — Goal Detail view (parametric)
   • paris  = семейная цель (внутри семейного кошелька)
   • turkey = личная цель Асхата (можно добавить не-членов семьи)
   target/collected · members & permissions · proactive rules · self top-up
   Relies on globals from family.js: showToast, showSuccess, openSheet, closeSheets
   ════════════════════════════════════════════════════════════ */
(function(){
  const q  = s => document.querySelector(s);
  const qa = s => Array.from(document.querySelectorAll(s));
  const money = n => Math.round(n).toLocaleString('ru-RU');
  const parseN = v => parseInt(String(v).replace(/\D/g,'')) || 0;
  const toast = m => window.showToast && window.showToast(m);

  const PERM_TAG = { owner:['owner','Владелец'], topup:['topup','Пополняет'], view:['view','Просмотр'], full:['full','Полный'] };
  const PERM_PI  = { owner:'👑', topup:'➕', view:'👁️', full:'🔁' };

  /* ── two goals ─────────────────────────────────────────── */
  const GOALS = {
    paris: {
      emoji:'🗼', name:'Париж 2027', titleSub:'Семейная цель · краудфандинг',
      heroSub:'Отпуск · 2 взрослых + 1 ребёнок · к июлю 2027',
      target:1200000, collected:740000, perMonth:'≈ 57 000 ₸', term:'8 мес.',
      ai:'По текущему темпу <b>≈ 57 000 ₸/мес</b> цель закроется <b>к 5 июля</b> — на 3 недели раньше срока. Хотите ускорить? Включите автопополнение для всех.',
      memberSec:'Кто пополняет',
      offer:'Скидка −8% на тур через маркетплейс банка + ставка 16.5%',
      memberSub:'Выберите из контактов. Семейный кошелёк — для родных.',
      members:[
        { av:'👨🏻', name:'Асхат',         perm:'owner', permLabel:'Владелец · полный доступ', amount:420000, you:true },
        { av:'👩🏻', name:'Динара',        perm:'topup', permLabel:'Может пополнять · кэшбэк',  amount:210000 },
        { av:'👵🏻', name:'Апа (бабушка)', perm:'topup', permLabel:'Может пополнять',           amount:110000 },
      ],
      rules:[
        { e:'🪙', who:'Асхат',  t:'округлять остатки от покупок в цель', on:true,  sub:'≈ 4 500 ₸ в этом месяце', pos:true },
        { e:'💸', who:'Динара', t:'весь кэшбэк — в цель',                on:true,  sub:'+8 400 ₸ в этом месяце',  pos:true },
        { e:'📅', who:'Все',    t:'автопополнение 1 числа',              on:false, sub:'по 25 000 ₸ с каждого' },
      ],
      contacts:[
        { av:'👴🏻', name:'Ата (дедушка)', phone:'+7 701 ··· 4408' },
        { av:'👩🏻‍🦰', name:'Гульнара (тётя)', phone:'+7 747 ··· 7731' },
        { av:'👧🏻', name:'Жанеке',          phone:'+7 700 ··· 5560' },
      ],
    },
    turkey: {
      emoji:'🏖️', name:'Турция 2026', titleSub:'Личная цель · можно добавить друзей',
      heroSub:'Отпуск с друзьями · к сентябрю 2026',
      target:900000, collected:540000, perMonth:'≈ 45 000 ₸', term:'8 мес.',
      ai:'Это <b>ваша личная цель</b> — копите сами или позовите друзей вскладчину. По текущему темпу <b>≈ 45 000 ₸/мес</b> закроете <b>к августу</b>.',
      memberSec:'Участники складчины',
      offer:'Раннее бронирование −10% и рассрочка 0% через маркетплейс банка',
      memberSub:'Добавьте кого угодно из контактов — не обязательно члена семьи.',
      members:[
        { av:'👨🏻', name:'Асхат',        perm:'owner', permLabel:'Владелец · полный доступ', amount:380000, you:true },
        { av:'🧔🏻', name:'Ерлан (друг)', perm:'topup', permLabel:'Может пополнять · вскладчину', amount:160000 },
      ],
      rules:[
        { e:'🪙', who:'Асхат', t:'округлять остатки от покупок в цель', on:true,  sub:'≈ 3 200 ₸ в этом месяце', pos:true },
        { e:'💳', who:'Асхат', t:'10% кэшбэка с карты — в цель',       on:true,  sub:'+2 600 ₸ в этом месяце',  pos:true },
        { e:'📅', who:'Асхат', t:'автопополнение 5 числа',             on:false, sub:'по 40 000 ₸' },
      ],
      contacts:[
        { av:'🧑🏻', name:'Дамир (друг)',  phone:'+7 705 ··· 1192' },
        { av:'👩🏻', name:'Сауле (коллега)', phone:'+7 778 ··· 3340' },
        { av:'🧔🏻', name:'Тимур (друг)',  phone:'+7 747 ··· 9981' },
      ],
    },
  };

  let key = 'paris';
  const G = () => GOALS[key];

  /* ── progress + header render ──────────────────────────── */
  function renderHead(){
    const g = G();
    q('#gdTitle').childNodes[0].nodeValue = g.name;
    q('#gdTitleSub').textContent = g.titleSub;
    q('#gdEmoji').textContent = g.emoji;
    q('#gdName').textContent = g.name;
    q('#gdHeroSub').textContent = g.heroSub;
    q('#gdStatMonth').textContent = g.perMonth;
    q('#gdStatTerm').textContent = g.term;
    q('#gdAiText').innerHTML = g.ai;
    q('#gdMembersSec').firstChild.nodeValue = g.memberSec + ' ';
    q('#gdOfferB').textContent = g.offer;
    q('#gdMemberSub').textContent = g.memberSub;
    q('#gdTopupTitle').textContent = `Пополнить «${g.name}»`;
  }
  function renderProgress(){
    const g = G();
    const pct = Math.min(100, Math.round(g.collected / g.target * 100));
    q('#gdBig').innerHTML = `${money(g.collected)} <span class="of">/ ${money(g.target)} ₸</span>`;
    q('#gdBar').style.width = pct + '%';
    q('#gdPct').textContent = pct + '% собрано';
    q('#gdLeft').textContent = 'осталось ' + money(Math.max(0, g.target - g.collected)) + ' ₸';
    q('#gdStatNow').textContent = money(g.collected) + ' ₸';
  }
  function renderMembers(){
    const g = G();
    q('#gdMembers').innerHTML = g.members.map(m => {
      const pi = PERM_PI[m.perm] || '➕';
      return `<div class="gd-mem">
        <div class="av">${m.av}</div>
        <div class="mi">
          <div class="mn">${m.name}${m.you?' <span class="permtag full">вы</span>':''}</div>
          <div class="perm"><span class="pi">${pi}</span>${m.permLabel}</div>
        </div>
        <div class="amt"><div class="a">${money(m.amount)} ₸</div><div class="b">внёс</div></div>
      </div>`;
    }).join('') + `
      <div class="gd-addmem" id="gdAddMem">
        <div class="ic">＋</div>
        <div class="it"><div class="a">Добавить из контактов</div><div class="b">${key==='turkey'?'Друзья и кто угодно · права на доступ':'Выдать права: просмотр · пополнять · снимать'}</div></div>
        <span class="ch">›</span>
      </div>`;
    q('#gdAddMem').addEventListener('click', openMemberSheet);
  }
  function renderRules(){
    q('#gdRules').innerHTML = G().rules.map((r,i) => `
      <div class="gd-rule">
        <div class="re">${r.e}</div>
        <div class="rt">
          <div class="a"><b>${r.who}:</b> ${r.t}</div>
          <div class="b ${r.pos?'pos':''}">${r.sub}</div>
        </div>
        <div class="toggle" data-on="${r.on?1:0}" data-i="${i}"></div>
      </div>`).join('');
    qa('#gdRules .toggle').forEach(t => t.addEventListener('click', () => {
      const i = +t.dataset.i; const rule = G().rules[i];
      rule.on = !rule.on; t.dataset.on = rule.on ? '1' : '0';
      toast(rule.on ? `✅ Правило включено: ${rule.t}` : `Правило выключено`);
    }));
  }

  /* ── open / close ──────────────────────────────────────── */
  function openGoalDetail(which){
    key = (which === 'turkey') ? 'turkey' : 'paris';
    renderHead(); renderProgress(); renderMembers(); renderRules();
    q('#gdBody') && (q('#gdBody').scrollTop = 0);
    const body = q('#goalDetail .gd-body'); if(body) body.scrollTop = 0;
    q('#goalDetail').classList.add('show');
  }
  function closeGoalDetail(){ q('#goalDetail').classList.remove('show'); }
  window.openGoalDetail = openGoalDetail;

  /* ── top-up sheet ──────────────────────────────────────── */
  function syncTopupBtn(){
    const v = parseN(q('#gdAmt').value);
    q('#gdTopupConfirm').textContent = 'Пополнить на ' + money(v) + ' ₸';
  }
  function openTopup(){
    q('#gdAmt').value = '25 000';
    qa('#gdChips .amtchip').forEach(c => c.dataset.on = c.dataset.v==='25000' ? '1' : '0');
    syncTopupBtn();
    window.openSheet('#gdTopupSheet');
  }

  /* ── add-member sheet ──────────────────────────────────── */
  let pickedContact = null, pickedPerm = 'view';
  function renderContacts(){
    q('#gdContacts').innerHTML = G().contacts.map((c,i) => `
      <div class="contact" data-i="${i}" data-on="0">
        <div class="av">${c.av}</div>
        <div class="ci"><div class="cn">${c.name}</div><div class="cp">${c.phone}</div></div>
        <div class="pick"></div>
      </div>`).join('');
    qa('#gdContacts .contact').forEach(el => el.addEventListener('click', () => {
      qa('#gdContacts .contact').forEach(x => x.dataset.on = '0');
      el.dataset.on = '1';
      pickedContact = G().contacts[+el.dataset.i];
      q('#gdMemberName').value = pickedContact.name;
    }));
  }
  function openMemberSheet(){
    pickedContact = null; pickedPerm = 'view';
    renderContacts();
    q('#gdMemberName').value = '';
    qa('#gdPermPick .permopt').forEach(o => o.dataset.on = o.dataset.v==='view' ? '1' : '0');
    window.openSheet('#gdMemberSheet');
  }

  /* ── boot ──────────────────────────────────────────────── */
  function boot(){
    const close = q('#gdClose'); if(close) close.addEventListener('click', closeGoalDetail);
    const info = q('#gdInfo'); if(info) info.addEventListener('click', () => window.openSheet('#plaqueSheet'));
    const manage = q('#gdManage'); if(manage) manage.addEventListener('click', () => toast('⚙️ Права: просмотр · пополнять · пополнять и снимать'));

    q('#gdTopupBtn').addEventListener('click', openTopup);
    q('#gdInvite').addEventListener('click', openMemberSheet);

    qa('#gdChips .amtchip').forEach(c => c.addEventListener('click', () => {
      qa('#gdChips .amtchip').forEach(x => x.dataset.on = '0'); c.dataset.on = '1';
      q('#gdAmt').value = money(+c.dataset.v); syncTopupBtn();
    }));
    q('#gdAmt').addEventListener('focus', e => { e.target.value = String(parseN(e.target.value)); e.target.select(); qa('#gdChips .amtchip').forEach(x=>x.dataset.on='0'); });
    q('#gdAmt').addEventListener('input', syncTopupBtn);
    q('#gdAmt').addEventListener('blur', e => { e.target.value = money(parseN(e.target.value)); });
    q('#gdTopupConfirm').addEventListener('click', () => {
      const v = parseN(q('#gdAmt').value);
      if(v <= 0){ toast('Введите сумму'); return; }
      window.closeSheets();
      setTimeout(() => {
        const g = G();
        g.collected = Math.min(g.target, g.collected + v);
        g.members[0].amount += v;
        renderProgress(); renderMembers();
        const pct = Math.round(g.collected / g.target * 100);
        window.showSuccess('Цель пополнена', `+${money(v)} ₸ на «${g.name}» ${g.emoji} Уже ${pct}% — вы на шаг ближе к отпуску!`);
      }, 300);
    });

    qa('#gdPermPick .permopt').forEach(o => o.addEventListener('click', () => {
      qa('#gdPermPick .permopt').forEach(x => x.dataset.on = '0'); o.dataset.on = '1'; pickedPerm = o.dataset.v;
    }));
    q('#gdMemberAdd').addEventListener('click', () => {
      const name = q('#gdMemberName').value.trim();
      if(!pickedContact && !name){ toast('Выберите контакт'); return; }
      const c = pickedContact || { av:'🧑🏻' };
      const labels = { view:'Только просмотр', topup:'Может пополнять', full:'Пополнять и снимать' };
      G().members.push({ av:c.av, name: name || c.name, perm: pickedPerm, permLabel: labels[pickedPerm], amount:0 });
      window.closeSheets();
      renderMembers();
      setTimeout(() => window.showSuccess('Приглашение отправлено', `${name || c.name} получит ссылку. После входа доступ к цели: «${labels[pickedPerm]}».`), 300);
    });

    // open via hash from home
    const h = location.hash;
    if(h === '#goal' || h === '#goal-paris'){ setTimeout(() => openGoalDetail('paris'), 250); }
    else if(h === '#goal-turkey'){ setTimeout(() => openGoalDetail('turkey'), 250); }
  }
  if(document.readyState !== 'loading') boot();
  else document.addEventListener('DOMContentLoaded', boot);
})();
