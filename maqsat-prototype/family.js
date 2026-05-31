/* ════════════════════════════════════════════════════════════
   Maqsat & Family — interactive logic
   ════════════════════════════════════════════════════════════ */
const $  = s => document.querySelector(s);
const $$ = s => Array.from(document.querySelectorAll(s));
const fmt = n => Math.round(n).toLocaleString('ru-RU');
const parseAmt = v => parseInt(String(v).replace(/\D/g,'')) || 0;

/* ── Theme/accent carried from the main app (if set) ──────── */
(function syncTheme(){
  try{
    const t = localStorage.getItem('halyk-theme');
    const a = localStorage.getItem('halyk-accent');
    if(t) document.body.setAttribute('data-theme', t);
    if(a) document.body.setAttribute('data-accent', a);
  }catch(e){}
})();

/* ── Data ─────────────────────────────────────────────────── */
const MEMBERS = [
  { av:'👨🏻', name:'Асхат',  role:'Папа · владелец',       badge:'owner',    badgeText:'Владелец' },
  { av:'👩🏻', name:'Динара', role:'Мама · со-родитель',    badge:'kaspi',    badgeText:'из Kaspi' },
  { av:'👧🏻', name:'Жанеке', role:'Дочь · 18 · студентка', badge:'child',    badgeText:'Студент' },
  { av:'🧒🏻', name:'Мадеке', role:'Сын · 14 · школа',      badge:'child',    badgeText:'Ребёнок' },
  { av:'👶🏻', name:'Аминош', role:'Дочь · 10',             badge:'child',    badgeText:'Ребёнок' },
];

const VCARDS = [
  { name:'Коммуналка', num:'7781', cls:'gold',  type:'депозитная 14%', bal:'32 590', linked:true },
  { name:'Париж 2027', num:'2026', cls:'',      type:'сберегат. 16.5%', bal:'740 000', linked:false },
];

const KIDS = [
  { av:'🧒🏻', name:'Мадеке', limit:2000, usedPct:0,  pocket:'15 000', note:'школа' },
  { av:'👶🏻', name:'Аминош', limit:1500, usedPct:40, pocket:'12 000', note:'танцы' },
  { av:'👧🏻', name:'Жанеке', limit:8000, usedPct:25, pocket:'80 000', note:'общежитие' },
];

const OFFERS = [
  { e:'🛒', a:'Magnum — продукты', b:'Оплатите из кошелька и получите +5% бонусов', cta:'+5%' },
  { e:'⛽', a:'Helios — топливо',  b:'Кэшбэк 3% уходит в цель «Париж 2027»',       cta:'3%' },
  { e:'💊', a:'Аптеки Europharma', b:'Семейная скидка для участников группы',        cta:'−7%' },
];

const CHILD_HISTORY = [
  { e:'🍫', n:'Магазин у школы', d:'Сегодня · 11:20', v:'−400 ₸' },
  { e:'🚌', n:'Проезд · автобус', d:'Сегодня · 08:05', v:'−160 ₸' },
  { e:'👛', n:'Карманные от папы', d:'1 июня', v:'+15 000 ₸', pos:true },
];

/* ── Render: members ──────────────────────────────────────── */
function renderMembers(){
  $('#membersCard').innerHTML = MEMBERS.map(m => `
    <div class="member">
      <div class="av">${m.av}</div>
      <div class="mi"><div class="mn">${m.name}</div><div class="md">${m.role}</div></div>
      <span class="badge ${m.badge}">${m.badgeText}</span>
    </div>`).join('') + `
    <div class="invite" id="inviteRow">
      <div class="ic">＋</div>
      <div class="it"><div class="a">Пригласить в семью</div><div class="b">Без передачи логина и пароля</div></div>
      <span class="ch">›</span>
    </div>`;
  $('#inviteRow').addEventListener('click', () => openSheet('#inviteSheet'));
}

/* ── Render: virtual goal cards ───────────────────────────── */
function renderVCards(){
  const html = VCARDS.map(c => `
    <div class="vcard ${c.cls}">
      <div class="vtop"><div class="vchip"></div><div class="vtype">${c.linked?'квитанции ✓':'цель'}<b>${c.type.split(' ')[0]}</b></div></div>
      <div class="vname">${c.name}</div>
      <div class="vnum">•••• ${c.num}</div>
      <div class="vbot"><div class="vbal"><div class="k">Баланс</div><div class="v">${c.bal} ₸</div></div><div class="vpct">${c.type.split(' ').slice(-1)[0]}</div></div>
    </div>`).join('');
  $('#vcards').innerHTML = html + `
    <div class="vcard add" id="addVCard"><div class="pl">＋</div><div class="tx">Открыть карту<br>под цель</div></div>`;
  $('#addVCard').addEventListener('click', () => window.openDistribute && window.openDistribute());
}

/* ── Render: kids limits ──────────────────────────────────── */
function renderKids(){
  $('#kidsCard').innerHTML = KIDS.map(k => `
    <div class="kid">
      <div class="av">${k.av}</div>
      <div class="ki">
        <div class="kn">${k.name}</div>
        <div class="kl">Карманные <b>${k.pocket} ₸</b>/мес · ${k.note}</div>
        <div class="limbar"><i style="width:${k.usedPct}%"></i></div>
      </div>
      <div class="kr"><div class="kv">${fmt(k.limit)} ₸</div><div class="ks">лимит/день</div></div>
    </div>`).join('');
}

/* ── Render: mama offers ──────────────────────────────────── */
function renderOffers(){
  $('#offers').innerHTML = OFFERS.map(o => `
    <div class="offer"><span class="oe">${o.e}</span><span class="ot"><span class="a">${o.a}</span><span class="b">${o.b}</span></span><button class="obtn" data-toast="Предложение активировано">${o.cta}</button></div>`).join('');
}

/* ── Render: child history ────────────────────────────────── */
function renderChildHistory(){
  $('#childHistory').innerHTML = CHILD_HISTORY.map(h => `
    <div class="member" style="padding:13px 16px;">
      <div class="av" style="background:var(--bg);">${h.e}</div>
      <div class="mi"><div class="mn">${h.n}</div><div class="md">${h.d}</div></div>
      <span style="font-size:14px;font-weight:800;color:${h.pos?'var(--pos)':'var(--text)'};">${h.v}</span>
    </div>`).join('');
}

/* ── Role switching ───────────────────────────────────────── */
function switchRole(role){
  $$('.role').forEach(b => b.dataset.on = b.dataset.role === role ? '1' : '0');
  $$('.rolepane').forEach(p => {
    const on = p.dataset.pane === role;
    p.classList.toggle('on', on);
    if(on){ p.classList.remove('enter'); void p.offsetWidth; p.classList.add('enter');
      p.addEventListener('animationend', () => p.classList.remove('enter'), { once:true }); }
  });
  $('#pushHost').style.display = 'none';
  $('#pushHost').innerHTML = '';
  $('#body').scrollTop = 0;
}

/* ── Sheets / scrim ───────────────────────────────────────── */
function openSheet(sel){ $(sel).classList.add('show'); $('#scrim').classList.add('show'); }
function closeSheets(){ $$('.sheet').forEach(s => s.classList.remove('show')); $('#scrim').classList.remove('show'); }

/* ── Toast ────────────────────────────────────────────────── */
let toastTimer;
function showToast(msg){
  const t = $('#toast'); t.innerHTML = msg; t.classList.add('show');
  clearTimeout(toastTimer); toastTimer = setTimeout(() => t.classList.remove('show'), 2400);
}

/* ── Success overlay ──────────────────────────────────────── */
function showSuccess(title, text, onDone){
  $('#successTitle').textContent = title;
  $('#successText').textContent = text;
  $('#success').classList.add('show');
  $('#successDone').onclick = () => { $('#success').classList.remove('show'); if(onDone) onDone(); };
}

/* ════════ Virtual-card distribution lives in distribute.js ════════ */

/* ════════ CONFLICT — SOS school lunch ════════ */
function startSOS(){
  const pr = $('#payResult');
  pr.innerHTML = `
    <div class="declined">
      <span class="di">🚫</span>
      <span class="dt">Недостаточно средств<span>Дневной лимит 2 000 ₸ · обед 2 500 ₸ · не хватает 500 ₸</span></span>
    </div>
    <button class="btn btn-primary" id="askParent" style="margin-top:12px;">🙋 Запросить у папы 500 ₸</button>`;
  $('#payLunch').style.display = 'none';
  $('#askParent').addEventListener('click', requestApproval);
}

function requestApproval(){
  $('#askParent').textContent = 'Запрос отправлен…';
  $('#askParent').disabled = true;
  showToast('📨 Smart-push отправлен папе');
  setTimeout(() => {
    switchRole('papa');
    showPush();
  }, 1100);
}

function showPush(){
  const host = $('#pushHost');
  host.style.display = 'block';
  host.innerHTML = `
    <div class="push">
      <div class="ph2"><span class="ap">💚</span><span class="an">Halyk · Maqsat Family</span><span class="now">сейчас</span></div>
      <div class="pbody">
        <div class="pt">⚡ Мадеке в школьной столовой</div>
        <div class="pd">Терминал распознан как <b>«Образование / Столовая»</b>. Не хватает <b>500 ₸</b> до оплаты обеда (лимит 2 000 ₸).</div>
        <div class="pacts">
          <button class="btn btn-primary btn-sm" id="pushApprove">Одобрить разово</button>
          <button class="btn btn-danger btn-sm" id="pushDeny" style="width:auto;padding-left:18px;padding-right:18px;">Отклонить</button>
        </div>
      </div>
    </div>`;
  $('#pushApprove').addEventListener('click', approveSOS);
  $('#pushDeny').addEventListener('click', denySOS);
}

function approveSOS(){
  const host = $('#pushHost');
  host.querySelector('.pacts').innerHTML = `<div class="spin"></div>`;
  setTimeout(() => {
    host.style.display = 'none'; host.innerHTML = '';
    showSuccess('Разовое превышение одобрено', 'Мадеке может оплатить обед прямо на кассе. Лимит вернётся к 2 000 ₸ завтра.', () => {
      switchRole('child');
      $('#childToday').textContent = '0 ₸';
      $('#payResult').innerHTML = `
        <div class="declined" style="background:var(--green-soft);">
          <span class="di">✅</span>
          <span class="dt" style="color:var(--pos);">Обед оплачен · 2 500 ₸<span>Папа одобрил +500 ₸ разово. Спасибо!</span></span>
        </div>`;
      showToast('✅ Оплачено на кассе');
    });
  }, 1300);
}

function denySOS(){
  $('#pushHost').style.display = 'none'; $('#pushHost').innerHTML = '';
  showToast('Запрос отклонён');
  switchRole('child');
  $('#payResult').innerHTML = `
    <div class="declined"><span class="di">🚫</span><span class="dt">Папа отклонил запрос<span>Попробуйте позже или сегодня без обеда 😕</span></span></div>
    <button class="btn btn-primary" id="askParent" style="margin-top:12px;">🙋 Запросить снова</button>`;
  $('#askParent').addEventListener('click', requestApproval);
}

/* ── Generic data-toast ───────────────────────────────────── */
function bindToasts(){
  document.addEventListener('click', e => {
    const t = e.target.closest('[data-toast]');
    if(t){ showToast(t.dataset.toast); }
  });
}

/* ── Init ─────────────────────────────────────────────────── */
function init(){
  renderMembers();
  renderVCards();
  renderKids();
  renderOffers();
  renderChildHistory();
  bindToasts();

  // role switcher
  $$('.role').forEach(b => b.addEventListener('click', () => switchRole(b.dataset.role)));
  $('#manageBtn').addEventListener('click', () => showToast('⚙️ Управление ролями и правами (RBAC)'));

  // scrim
  $('#scrim').addEventListener('click', closeSheets);

  // plaque
  $('#plaqueBtn').addEventListener('click', () => openSheet('#plaqueSheet'));
  $('#plaqueClose').addEventListener('click', closeSheets);

  // goal → full-screen detail view
  $('#goalCard').addEventListener('click', () => window.openGoalDetail ? window.openGoalDetail() : openSheet('#goalSheet'));
  $('#goalTopup').addEventListener('click', () => { closeSheets(); setTimeout(() => showSuccess('Цель пополнена', 'Вы на шаг ближе к Парижу 2027 🗼'), 300); });

  // mediator
  $('#medAccept').addEventListener('click', () => showToast('✅ Лимит «Развлечения» снижен на 10%'));

  // mama personal goal
  const mg = $('#mamaGoalTopup');
  if(mg) mg.addEventListener('click', () => setTimeout(() => showSuccess('Отложено в личную цель', '+12 000 ₸ на «Платье для Аминош» 👗 Цель видите только вы.'), 100));

  // invite
  $$('#inviteRole .typeopt').forEach(o => o.addEventListener('click', () => {
    $$('#inviteRole .typeopt').forEach(x => x.dataset.on = '0'); o.dataset.on = '1';
  }));
  $('#inviteSend').addEventListener('click', () => {
    closeSheets();
    setTimeout(() => showSuccess('Ссылка готова', 'Отправьте её родственнику в WhatsApp или SMS. Он войдёт под собой и получит виртуальную карту.'), 300);
  });

  // (virtual-card distribution wired in distribute.js)

  // conflict
  $('#payLunch').addEventListener('click', startSOS);
}
init();

/* expose helpers + state for distribute.js */
Object.assign(window, { fmt, parseAmt, showToast, showSuccess, openSheet, closeSheets, switchRole, VCARDS, renderVCards });
