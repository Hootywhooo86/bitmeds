/* bitmeds extras: HISTORY (calendar, streaks, site map), TRIP (packing list), LABS (bloodwork log),
 * MORE (family profiles, reminders, backup / export). Uses window.BM from index.html. */
(function () {
  'use strict';
  const BM = window.BM, $ = id => document.getElementById(id), esc = BM.esc;
  const DAY = 86400000;
  const css = document.createElement('style');
  css.textContent = `
  .xsec{background:var(--panel);border:3px solid var(--line);box-shadow:6px 6px 0 #000;padding:16px;margin-bottom:22px}
  .xsec h3{font-family:var(--px);font-size:10px;color:var(--pill);margin-bottom:14px;letter-spacing:1px;text-wrap:balance}
  .xsec p.note{font-size:13px;color:var(--muted);line-height:1.55;margin-top:10px}
  .xchips{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:14px}
  .xchips button{font-family:var(--px);font-size:8px;padding:9px 10px;background:var(--bg);border:3px solid var(--line);color:var(--muted);display:flex;align-items:center;gap:6px}
  .xchips button.on{border-color:var(--pill);color:var(--ink)}
  .stats{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin-bottom:14px}
  .stats div{background:var(--bg);border:3px solid var(--line);padding:10px 6px;text-align:center;font-family:var(--px);font-size:7px;color:var(--muted);line-height:1.6;min-width:0}
  .stats b{display:block;font-size:15px;color:var(--pill);margin-bottom:4px;font-weight:400}
  .stats .fire b{color:var(--orange)}
  @media (max-width:420px){.stats{grid-template-columns:repeat(2,1fr)}}
  .calhead{display:flex;align-items:center;justify-content:space-between;margin-bottom:10px}
  .calhead b{font-family:var(--px);font-size:10px;color:var(--ink);font-weight:400}
  .calhead button,.xbtn{font-family:var(--px);font-size:9px;padding:10px 12px;background:var(--panel-2);border:none;color:var(--ink);box-shadow:0 4px 0 #000}
  .xbtn.go{background:var(--pill);color:var(--bg);box-shadow:0 4px 0 var(--pill-deep)}
  .xbtn.warn{background:var(--red);color:#fff;box-shadow:0 4px 0 var(--red-deep)}
  .xbtn:active,.calhead button:active{transform:translateY(3px);box-shadow:0 1px 0 #000}
  .cal{display:grid;grid-template-columns:repeat(7,1fr);gap:4px}
  .cal i{font-style:normal;font-family:var(--px);font-size:7px;color:var(--muted);text-align:center;padding:4px 0}
  .cal div{aspect-ratio:1;background:var(--bg);border:2px solid var(--line);position:relative;font-family:var(--px);font-size:7px;color:var(--muted);padding:3px;min-width:0}
  .cal div.out{opacity:.25}
  .cal div.today{border-color:var(--ink)}
  .cal div span{position:absolute;left:50%;bottom:4px;transform:translateX(-50%);width:44%;height:30%;min-height:6px}
  .cal .ok span{background:var(--pill);box-shadow:0 0 6px var(--pill)} .cal .late span{background:var(--yellow)}
  .cal .skip span{background:#5b5b7a} .cal .miss span{background:var(--red);box-shadow:0 0 6px var(--red)} .cal .plan span{background:transparent;border:2px dashed #4b4b7a}
  .legend{display:flex;gap:12px;flex-wrap:wrap;margin-top:10px;font-family:var(--px);font-size:7px;color:var(--muted)}
  .legend em{display:inline-block;width:10px;height:10px;margin-right:5px;vertical-align:-1px}
  .rows{display:flex;flex-direction:column;gap:6px}
  .rows div{display:flex;justify-content:space-between;gap:10px;background:var(--bg);border:2px solid var(--line);padding:9px 10px;font-size:13px;align-items:center}
  .rows div b{font-family:var(--px);font-size:7px;padding:5px 6px;font-weight:400;white-space:nowrap}
  .rows .ok b{background:#0f2a22;color:var(--pill)} .rows .late b{background:#2a2410;color:var(--yellow)} .rows .skip b{background:#1f1f3a;color:var(--muted)} .rows .miss b{background:#2a1018;color:var(--red)}
  .rows div small{color:var(--muted);font-size:12px}
  .rows div button{background:none;border:none;color:var(--muted);font-family:var(--px);font-size:10px;padding:4px}
  .body{display:grid;grid-template-columns:150px 1fr;gap:14px;align-items:center}
  .body svg{width:150px;height:auto;image-rendering:pixelated;shape-rendering:crispEdges}
  @media (max-width:420px){.body{grid-template-columns:110px 1fr}.body svg{width:110px}}
  .xform{display:flex;flex-direction:column;gap:10px}
  .xform .row{display:flex;gap:10px;flex-wrap:wrap}
  .check{display:flex;gap:10px;align-items:center;background:var(--bg);border:2px solid var(--line);padding:10px;font-size:14px;cursor:pointer}
  .check input{width:22px;height:22px;flex-shrink:0;accent-color:var(--pill);-webkit-appearance:checkbox;appearance:auto}
  .check.done{opacity:.55;text-decoration:line-through}
  .check .need{margin-left:auto;font-family:var(--px);font-size:7px;padding:5px 6px;white-space:nowrap}
  .check .need.ok{background:#0f2a22;color:var(--pill)} .check .need.short{background:#2a1018;color:var(--red)}
  .chart{width:100%;height:auto;display:block;background:var(--bg);border:2px solid var(--line);image-rendering:pixelated;shape-rendering:crispEdges}
  .labgrp{margin-top:16px}
  .labgrp h4{font-family:var(--px);font-size:9px;color:var(--ink);margin-bottom:8px;display:flex;justify-content:space-between;font-weight:400}
  .labgrp h4 span{color:var(--pill)}
  .toggle{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:10px 0;border-bottom:2px solid var(--line);font-size:14px}
  .toggle input[type=number]{width:90px}
  .toggle small{display:block;color:var(--muted);font-size:12px;margin-top:3px}
  `;
  document.head.appendChild(css);

  // ---------------------------------------------------------------- tabs
  let tab = 'timers';
  function show(t) {
    tab = t;
    document.querySelectorAll('#tabs button').forEach(b => b.classList.toggle('on', b.dataset.tab === t));
    document.querySelectorAll('.tab').forEach(el => { el.hidden = el.id !== 'tab-' + t; });
    if (t === 'history') paintHistory();
    if (t === 'trip') paintTrip();
    if (t === 'labs') paintLabs();
    if (t === 'more') paintMore();
    if (t === 'timers') BM.render();
    window.scrollTo({ top: 0, behavior: 'auto' });
  }
  $('tabs').addEventListener('click', e => { const b = e.target.closest('button'); if (b) show(b.dataset.tab); });
  BM.onChange(kind => { if (kind === 'reload' || kind === 'synced') { if (tab !== 'timers') show(tab); } });

  const medChip = (m, on) => `<button data-med="${m.id}" class="${on ? 'on' : ''}">${BM.sprite(BM.iconOf(m), 18, BM.accOf(m))}${esc(m.name.toUpperCase())}${m.forWho ? ' · ' + esc(m.forWho.toUpperCase()) : ''}</button>`;
  const fmtDate = t => new Date(t).toLocaleDateString([], { weekday: 'short', month: 'short', day: 'numeric' });
  const fmtTime = t => new Date(t).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
  const dayKey = t => { const d = new Date(t); return d.getFullYear() + '-' + d.getMonth() + '-' + d.getDate(); };
  const startOfDay = t => { const d = new Date(t); d.setHours(0, 0, 0, 0); return d.getTime(); };

  // ---------------------------------------------------------------- HISTORY
  let hMed = null, hMonth = null;
  function entries(m) {
    // newest first: {t, due, site, skip, kind}
    const hist = (m.hist || []).map(e => Object.assign({}, e, { kind: e.skip ? 'skip' : (e.due && e.t > e.due + BM.GRACE) ? 'late' : 'ok' }));
    const seen = new Set(hist.map(e => e.t));
    (m.log || []).forEach(t => { if (!seen.has(t)) hist.push({ t, kind: 'ok' }); });   // older doses from before history existed
    return hist.sort((a, b) => b.t - a.t);
  }
  function missedSlots(m, from, to) {
    // set-day meds only: a scheduled day in the past with nothing logged within ±1 day counts as missed
    if (m.sched !== 'days' || !(m.days || []).length) return [];
    const ents = entries(m), out = [];
    const first = ents.length ? ents[ents.length - 1].t : Date.now();
    let t = BM.nextSlotAfter(m, Math.max(from, first - DAY) - 1);
    const now = Date.now();
    while (t < Math.min(to, now - BM.GRACE)) {
      if (!ents.some(e => Math.abs(e.t - t) < DAY + BM.GRACE)) out.push(t);
      t = BM.nextSlotAfter(m, t);
    }
    return out;
  }
  function paintHistory() {
    const meds = BM.meds, el = $('tab-history');
    if (!meds.length) { el.innerHTML = `<div class="xsec"><h3>▸ HISTORY</h3><p class="note">Add a med on the TIMERS tab — every dose you log shows up here.</p></div>`; return; }
    if (!meds.some(m => m.id === hMed)) hMed = (meds.find(m => m.pinned) || meds[0]).id;
    const m = meds.find(x => x.id === hMed), ents = entries(m);
    const given = ents.filter(e => e.kind !== 'skip'), onTime = ents.filter(e => e.kind === 'ok').length;
    const missed = missedSlots(m, Date.now() - 365 * DAY, Date.now());
    const pct = given.length + missed.length ? Math.round(onTime / (given.length + missed.length) * 100) : 100;
    if (hMonth == null) { const d = new Date(); hMonth = new Date(d.getFullYear(), d.getMonth(), 1).getTime(); }
    const mStart = new Date(hMonth), y = mStart.getFullYear(), mo = mStart.getMonth();
    const gridStart = new Date(y, mo, 1 - ((mStart.getDay() + 6) % 7)).getTime();
    const byDay = {};
    ents.forEach(e => { const k = dayKey(e.t); if (!byDay[k] || byDay[k] === 'skip') byDay[k] = e.kind; });
    missedSlots(m, gridStart, gridStart + 42 * DAY).forEach(t => { const k = dayKey(t); if (!byDay[k]) byDay[k] = 'miss'; });
    const planned = {};
    if (m.sched === 'days') { let t = m.nextAt; for (let i = 0; i < 12 && t < gridStart + 42 * DAY; i++) { planned[dayKey(t)] = 1; t = BM.nextSlotAfter(m, t); } }
    let cells = ['M', 'T', 'W', 'T', 'F', 'S', 'S'].map(d => `<i>${d}</i>`).join('');
    for (let i = 0; i < 42; i++) {
      const t = new Date(y, mo, 1 - ((mStart.getDay() + 6) % 7) + i).getTime(), k = dayKey(t), dd = new Date(t);
      const cls = [dd.getMonth() !== mo ? 'out' : '', k === dayKey(Date.now()) ? 'today' : '', byDay[k] || (planned[k] && t >= startOfDay(Date.now()) ? 'plan' : '')].join(' ');
      cells += `<div class="${cls}">${dd.getDate()}<span></span></div>`;
    }
    const recent = ents.slice(0, 30).map(e => {
      const label = e.kind === 'skip' ? 'SKIPPED' : e.kind === 'late' ? 'LATE ' + Math.round((e.t - e.due) / 3600000) + 'H' : 'ON TIME';
      return `<div class="${e.kind}"><span>${fmtDate(e.t)} · ${fmtTime(e.t)}${e.site ? ` <small>· ${esc(e.site)}</small>` : ''}</span><b>${label}</b></div>`;
    }).join('') + missed.slice(-5).reverse().map(t => `<div class="miss"><span>${fmtDate(t)} · ${fmtTime(t)}</span><b>MISSED</b></div>`).join('');
    el.innerHTML = `
      <div class="xsec"><h3>▸ HISTORY</h3>
        <div class="xchips" id="hMeds">${meds.map(x => medChip(x, x.id === hMed)).join('')}</div>
        <div class="stats">
          <div class="fire"><b>🔥 ${BM.streakOf(m)}</b>ON-TIME STREAK</div>
          <div><b>${given.length}</b>DOSES LOGGED</div>
          <div><b>${pct}%</b>ON TIME</div>
          <div><b style="color:var(--red)">${missed.length}</b>MISSED · ${ents.filter(e => e.kind === 'skip').length} SKIPPED</div>
        </div>
        <div class="calhead"><button id="calPrev">◀</button><b>${mStart.toLocaleDateString([], { month: 'long', year: 'numeric' }).toUpperCase()}</b><button id="calNext">▶</button></div>
        <div class="cal">${cells}</div>
        <div class="legend"><span><em style="background:var(--pill)"></em>ON TIME</span><span><em style="background:var(--yellow)"></em>LATE</span><span><em style="background:#5b5b7a"></em>SKIPPED</span><span><em style="background:var(--red)"></em>MISSED</span><span><em style="border:2px dashed #4b4b7a"></em>COMING UP</span></div>
      </div>
      ${(m.sites || []).length ? `<div class="xsec"><h3>▸ INJECTION SITES</h3>${siteMap(m, ents)}</div>` : ''}
      ${(m.refills || []).length ? `<div class="xsec"><h3>▸ PRESCRIPTIONS PICKED UP</h3><div class="rows">${m.refills.map(r => `<div class="ok"><span>${fmtDate(r.t)}</span><b>${r.mode === 'add' ? '+' : '= '}${BM.fmtAmt(r.amt)} ${esc((m.unit || '').toUpperCase())}</b></div>`).join('')}</div></div>` : ''}
      <div class="xsec"><h3>▸ RECENT DOSES</h3><div class="rows">${recent || '<p class="note">Nothing logged yet — tap GAVE IT when you take a dose.</p>'}</div>
        <p class="note">Export your history for your doctor from the MORE tab.</p></div>`;
    $('hMeds').onclick = e => { const b = e.target.closest('button'); if (b) { hMed = b.dataset.med; paintHistory(); } };
    $('calPrev').onclick = () => { const d = new Date(hMonth); hMonth = new Date(d.getFullYear(), d.getMonth() - 1, 1).getTime(); paintHistory(); };
    $('calNext').onclick = () => { const d = new Date(hMonth); hMonth = new Date(d.getFullYear(), d.getMonth() + 1, 1).getTime(); paintHistory(); };
  }
  // pixel body with the 8 injection sites; brighter = used more recently
  function siteMap(m, ents) {
    const SPOT = { 'Left delt': [3.6, 7.6], 'Right delt': [16.4, 7.6], 'Left belly': [8.6, 13], 'Right belly': [11.4, 13], 'Left glute': [7.8, 19.8], 'Right glute': [12.2, 19.8], 'Left thigh': [7.6, 25.4], 'Right thigh': [12.4, 25.4] };
    const body = ['......oooo......', '.....oWWWWo.....', '.....oWWWWo.....', '......oWWo......', '...oooWWWWooo...', '..oWWWWWWWWWWo..',
      '..oWWWWWWWWWWo..', '..oWWWWWWWWWWo..', '..oWoWWWWWWoWo..', '..oWoWWWWWWoWo..', '..oWoWWWWWWoWo..', '..oWoWWWWWWoWo..',
      '..ooo.oWWWWo.ooo', '......oWWWWo....', '.....oWWWWWWo...', '.....oWWWWWWo...', '.....oWWooWWo...', '.....oWWo.oWWo..',
      '.....oWWo.oWWo..', '.....oWWo.oWWo..', '.....oWWo.oWWo..', '.....oooo.oooo..'];
    const S = 1;
    let r = '';
    body.forEach((row, y) => [...row].forEach((ch, x) => { if (ch === 'o') r += `<rect x="${x * 1.25}" y="${y * 1.4}" width="1.3" height="1.45" fill="#4b4b7a"/>`; if (ch === 'W') r += `<rect x="${x * 1.25}" y="${y * 1.4}" width="1.3" height="1.45" fill="#2a2a4a"/>`; }));
    const counts = {}, last = {};
    ents.forEach(e => { if (e.site) { counts[e.site] = (counts[e.site] || 0) + 1; if (!last[e.site]) last[e.site] = e.t; } });
    const order = Object.keys(last).sort((a, b) => last[b] - last[a]);
    const next = (m.sites || [])[(m.siteIdx || 0) % Math.max(1, (m.sites || []).length)];
    (m.sites || []).forEach(sname => {
      const p = SPOT[sname]; if (!p) return;
      const rank = order.indexOf(sname), col = sname === next ? '#facc15' : rank === 0 ? '#ff5d5d' : rank > 0 ? '#34d399' : '#4b4b7a';
      r += `<rect x="${p[0] - 1.4}" y="${p[1] - 1.4}" width="2.8" height="2.8" fill="#000"/><rect x="${p[0] - 1.2}" y="${p[1] - 1.2}" width="2.4" height="2.4" fill="${col}"/>`;
    });
    const list = (m.sites || []).map(sname => `<div class="${sname === next ? 'late' : 'ok'}"><span>${esc(sname)}${last[sname] ? ` <small>· last ${fmtDate(last[sname])}</small>` : ''}</span><b>${sname === next ? 'NEXT' : (counts[sname] || 0) + '×'}</b></div>`).join('');
    return `<div class="body"><svg viewBox="0 0 21 31" aria-hidden="true">${r}</svg><div class="rows">${list}</div></div>
      <div class="legend"><span><em style="background:#facc15"></em>NEXT</span><span><em style="background:#ff5d5d"></em>LAST USED</span><span><em style="background:#34d399"></em>USED</span></div>`;
  }

  // ---------------------------------------------------------------- TRIP
  function dosesBetween(m, from, to) {
    const out = []; let t = m.nextAt;
    if (t < from) { // roll forward to the trip
      for (let i = 0; i < 500 && t < from; i++) t = m.sched === 'days' ? BM.nextSlotAfter(m, t) : t + (m.intervalMin || 480) * 60000;
    }
    for (let i = 0; i < 500 && t <= to; i++) { if (t >= from) out.push(t); t = m.sched === 'days' ? BM.nextSlotAfter(m, t) : t + (m.intervalMin || 480) * 60000; }
    return out;
  }
  function paintTrip() {
    const x = BM.extra, el = $('tab-trip'), trip = x.trip || null;
    const today = new Date(); const iso = d => new Date(d - new Date(d).getTimezoneOffset() * 60000).toISOString().slice(0, 10);
    let body = '';
    if (trip) {
      const from = trip.start, to = trip.end + DAY - 1, packed = trip.packed || {};
      const rows = BM.meds.filter(m => !trip.who || trip.who === 'all' || (m.forWho || '') === trip.who).map(m => {
        const ds = dosesBetween(m, from, to), need = Math.round(ds.length * m.amount * 1000) / 1000, sp = BM.supplyOf(m);
        const short = sp && sp.left < need;
        const extras = m.type === 'injection' ? [`Needles × ${ds.length + 1}`, `Alcohol swabs × ${ds.length + 2}`, 'Sharps container'] : m.type === 'pill' || m.type === 'tablet' ? ['Pill case'] : [];
        const main = `<label class="check ${packed[m.id] ? 'done' : ''}"><input type="checkbox" data-pack="${m.id}" ${packed[m.id] ? 'checked' : ''}>${BM.sprite(BM.iconOf(m), 22, BM.accOf(m))}<span>${esc(m.name)} — ${ds.length} dose${ds.length === 1 ? '' : 's'} = <b>${BM.fmtAmt(need)} ${esc(m.unit)}</b>${ds.length ? `<br><small style="color:var(--muted)">${ds.map(t => fmtDate(t)).join(' · ')}</small>` : ''}</span><span class="need ${short ? 'short' : 'ok'}">${sp ? (short ? 'SHORT ' + BM.fmtAmt(need - sp.left) : 'ENOUGH ✓') : 'SET SUPPLY?'}</span></label>`;
        return main + extras.map((e, i) => { const k = m.id + ':' + i; return `<label class="check ${packed[k] ? 'done' : ''}"><input type="checkbox" data-pack="${k}" ${packed[k] ? 'checked' : ''}><span>${e}</span></label>`; }).join('');
      }).join('');
      const nPacked = Object.values(packed).filter(Boolean).length, total = el.querySelectorAll ? 0 : 0;
      body = `<div class="xsec"><h3>▸ PACKING LIST · ${fmtDate(trip.start).toUpperCase()} → ${fmtDate(trip.end).toUpperCase()}</h3>
        <div class="xform">${rows || '<p class="note">No meds yet.</p>'}
          <label class="check"><input type="checkbox" data-pack="_rx" ${packed._rx ? 'checked' : ''}><span>Prescription / doctor's note (for flights + borders)</span></label>
        </div>
        <p class="note">${trip.remind ? `🔔 You'll get a "pack your meds" reminder the evening before you leave.` : ''} Doses are counted from your real schedule.</p>
        <div style="display:flex;gap:10px;margin-top:14px;flex-wrap:wrap"><button class="xbtn warn" id="tripClear">✕ END TRIP</button></div></div>`;
    }
    const profiles = [...new Set([...(x.profiles || []).map(p => p.name), ...BM.meds.map(m => m.forWho).filter(Boolean)])];
    el.innerHTML = `<div class="xsec"><h3>▸ TRIP MODE — NEVER LEAVE YOUR MEDS BEHIND</h3>
        <div class="xform">
          <div class="row"><div class="field"><label for="tStart">Leaving</label><input id="tStart" type="date" value="${trip ? iso(trip.start) : iso(today)}"></div>
          <div class="field"><label for="tEnd">Coming home</label><input id="tEnd" type="date" value="${trip ? iso(trip.end) : iso(today.getTime() + 4 * DAY)}"></div></div>
          ${profiles.length ? `<div class="field"><label for="tWho">Who's going</label><select id="tWho"><option value="all">Everyone</option>${profiles.map(p => `<option ${trip && trip.who === p ? 'selected' : ''}>${esc(p)}</option>`).join('')}</select></div>` : ''}
          <label class="check"><input type="checkbox" id="tRemind" ${!trip || trip.remind ? 'checked' : ''}><span>Remind me to pack the evening before</span></label>
          <div><button class="xbtn go" id="tGo">${trip ? '↻ UPDATE TRIP' : '🧳 PLAN MY PACKING'}</button></div>
        </div></div>${body}`;
    $('tGo').onclick = () => {
      const s = new Date($('tStart').value + 'T00:00').getTime(), e = new Date($('tEnd').value + 'T00:00').getTime();
      if (!(s > 0) || !(e >= s)) return BM.toast('Pick the day you leave and the day you come home.');
      const ev = new Date(s - DAY); ev.setHours(18, 0, 0, 0);
      x.trip = { start: s, end: e, who: $('tWho') ? $('tWho').value : 'all', remind: $('tRemind').checked, remindAt: ev.getTime(), packed: (x.trip && x.trip.packed) || {} };
      BM.saveExtra(); paintTrip(); BM.toast('Packing list ready.');
    };
    if ($('tripClear')) $('tripClear').onclick = () => { delete x.trip; BM.saveExtra(); paintTrip(); };
    el.querySelectorAll('[data-pack]').forEach(cb => cb.onchange = () => { x.trip.packed[cb.dataset.pack] = cb.checked; BM.saveExtra(); cb.closest('.check').classList.toggle('done', cb.checked); });
  }

  // ---------------------------------------------------------------- LABS
  const TESTS = ['Total testosterone', 'Free testosterone', 'Estradiol (E2)', 'Hematocrit', 'Hemoglobin', 'PSA', 'SHBG', 'LH', 'FSH', 'Prolactin', 'Cholesterol', 'LDL', 'HDL', 'A1C', 'Blood pressure', 'Weight'];
  const UNITS = ['ng/dL', 'nmol/L', 'pg/mL', 'pmol/L', '%', 'g/dL', 'ng/mL', 'mIU/mL', 'mg/dL', 'mmol/L', 'mmHg', 'lb', 'kg'];
  function paintLabs() {
    const x = BM.extra, el = $('tab-labs'), today = new Date();
    const iso = new Date(today - today.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
    const groups = {};
    x.labs.forEach(l => { (groups[l.test] = groups[l.test] || []).push(l); });
    const html = Object.keys(groups).sort().map(test => {
      const ls = groups[test].sort((a, b) => a.date - b.date), last = ls[ls.length - 1];
      return `<div class="labgrp"><h4>${esc(test.toUpperCase())}<span>${esc(String(last.value))} ${esc(last.unit || '')}</span></h4>${chart(ls)}
        <div class="rows" style="margin-top:8px">${ls.slice().reverse().map(l => `<div class="ok"><span>${fmtDate(l.date)} · <b style="background:none;color:var(--ink);font-size:9px">${esc(String(l.value))} ${esc(l.unit || '')}</b>${l.note ? ` <small>· ${esc(l.note)}</small>` : ''}</span><button data-del="${l.id}" aria-label="Delete">✕</button></div>`).join('')}</div></div>`;
    }).join('');
    el.innerHTML = `<div class="xsec"><h3>▸ LABS + LEVELS — BRING THIS TO YOUR APPOINTMENT</h3>
        <form class="xform" id="labForm" autocomplete="off">
          <div class="row"><div class="field"><label for="lTest">Test</label><input id="lTest" list="labTests" placeholder="e.g. Total testosterone" required><datalist id="labTests">${TESTS.map(t => `<option value="${t}">`).join('')}</datalist></div>
          <div class="field"><label for="lDate">Date</label><input id="lDate" type="date" value="${iso}"></div></div>
          <div class="row"><div class="field s"><label for="lVal">Result</label><input id="lVal" inputmode="decimal" placeholder="e.g. 650" required></div>
          <div class="field s"><label for="lUnit">Unit</label><input id="lUnit" list="labUnits" placeholder="ng/dL"><datalist id="labUnits">${UNITS.map(u => `<option value="${u}">`).join('')}</datalist></div></div>
          <div class="field"><label for="lNote">Note <span style="font-weight:400">(optional)</span></label><input id="lNote" placeholder="e.g. trough, day before shot"></div>
          <div><button class="xbtn go" type="submit">+ ADD RESULT</button></div>
        </form>
        ${html || '<p class="note">No results yet. Add bloodwork after each test to see how your levels move with your dose.</p>'}
      </div>`;
    $('labForm').onsubmit = e => {
      e.preventDefault();
      const test = $('lTest').value.trim(), raw = $('lVal').value.trim();
      if (!test || !raw) return;
      const num = parseFloat(raw);
      x.labs.push({ id: BM.uid(), test, value: isNaN(num) || /[^\d.\-]/.test(raw) ? raw : num, unit: $('lUnit').value.trim(), note: $('lNote').value.trim(), date: new Date($('lDate').value + 'T12:00').getTime() || Date.now() });
      BM.saveExtra(); paintLabs(); BM.toast(`${test} saved.`);
    };
    el.querySelectorAll('[data-del]').forEach(b => b.onclick = () => { if (!confirm('Delete this result?')) return; x.labs = x.labs.filter(l => l.id !== b.dataset.del); BM.extra.labs = x.labs; BM.saveExtra(); paintLabs(); });
  }
  function chart(ls) {
    const pts = ls.filter(l => typeof l.value === 'number');
    if (pts.length < 2) return '';
    const W = 64, H = 24, min = Math.min(...pts.map(p => p.value)), max = Math.max(...pts.map(p => p.value)), span = max - min || 1;
    const t0 = pts[0].date, t1 = pts[pts.length - 1].date, ts = t1 - t0 || 1;
    let r = `<rect width="${W}" height="${H}" fill="#0b0b16"/>`;
    for (let y = 4; y < H; y += 5) r += `<rect x="0" y="${y}" width="${W}" height=".3" fill="#1f1f3a"/>`;
    let prev = null;
    pts.forEach(p => {
      const x = Math.round(2 + (p.date - t0) / ts * (W - 6)), y = Math.round(H - 4 - (p.value - min) / span * (H - 8));
      if (prev) { const n = Math.max(Math.abs(x - prev[0]), Math.abs(y - prev[1])); for (let i = 1; i < n; i++) r += `<rect x="${Math.round(prev[0] + (x - prev[0]) * i / n)}" y="${Math.round(prev[1] + (y - prev[1]) * i / n)}" width="1" height="1" fill="#047857"/>`; }
      r += `<rect x="${x - 1}" y="${y - 1}" width="2" height="2" fill="#34d399"/>`;
      prev = [x, y];
    });
    return `<svg class="chart" viewBox="0 0 ${W} ${H}" aria-label="Chart">${r}</svg>`;
  }

  // ---------------------------------------------------------------- MORE: profiles, reminders, backup, sync
  function paintMore() {
    const x = BM.extra, st = x.settings, el = $('tab-more');
    el.innerHTML = `
      <div class="xsec"><h3>▸ FAMILY PROFILES</h3>
        <div class="rows" id="profRows">${x.profiles.map(p => `<div class="ok"><span>${esc(p.name)} <small>· ${BM.meds.filter(m => m.forWho === p.name).length} meds</small></span><button data-delp="${p.id}" aria-label="Remove">✕</button></div>`).join('') || '<p class="note">Add the people you give meds to. Each med can be for someone, and the TIMERS tab gets a filter for each person.</p>'}</div>
        <form class="xform" id="profForm" style="margin-top:12px" autocomplete="off"><div class="row"><div class="field"><label for="pName">Name</label><input id="pName" maxlength="24" placeholder="e.g. Harlow"></div></div><div><button class="xbtn go" type="submit">+ ADD PERSON</button></div></form>
      </div>
      <div class="xsec"><h3>▸ REMINDERS</h3>
        <div class="toggle"><div>Heads-up before a dose<small>Hours before (0 = off). "Shot tomorrow 20:00"</small></div><input type="number" id="sHead" min="0" max="72" value="${st.headsUpH}"></div>
        <div class="toggle"><div>Keep reminding while overdue<small>Every N minutes until you log it (0 = off)</small></div><input type="number" id="sNag" min="0" max="240" value="${st.nagMin}"></div>
        <p class="note">${BM.NATIVE ? 'These ring even when bitmeds is closed.' : 'Install the Android app for reminders when bitmeds is closed.'}</p>
      </div>
      <div class="xsec" id="syncBox"><h3>▸ SYNC + FAMILY</h3><p class="note">Loading…</p></div>
      <div class="xsec"><h3>▸ BACKUP + EXPORT</h3>
        <div style="display:flex;gap:10px;flex-wrap:wrap">
          <button class="xbtn go" id="bExport">⬇ BACKUP FILE</button>
          <button class="xbtn" id="bImport">⬆ RESTORE BACKUP</button>
          <button class="xbtn" id="bCsv">📄 DOSE LOG FOR DOCTOR (CSV)</button>
        </div>
        <input type="file" id="bFile" accept=".json,application/json" hidden>
        <p class="note">A backup holds all meds, history, labs and settings. Keep it somewhere safe (Drive, email to yourself) so a new phone gets everything back.</p>
      </div>
      <div class="xsec"><h3>▸ ABOUT</h3><p class="note">bitmeds is part of the 128bit family. It's a reminder, not medical advice — follow your doctor or pharmacist's directions.</p></div>`;
    $('profForm').onsubmit = e => { e.preventDefault(); const n = $('pName').value.trim(); if (!n) return; if (!x.profiles.some(p => p.name.toLowerCase() === n.toLowerCase())) x.profiles.push({ id: BM.uid(), name: n }); BM.saveExtra(); paintMore(); };
    el.querySelectorAll('[data-delp]').forEach(b => b.onclick = () => { x.profiles = x.profiles.filter(p => p.id !== b.dataset.delp); BM.extra.profiles = x.profiles; BM.saveExtra(); paintMore(); });
    $('sHead').onchange = () => { st.headsUpH = Math.max(0, +$('sHead').value || 0); BM.saveExtra(); };
    $('sNag').onchange = () => { st.nagMin = Math.max(0, +$('sNag').value || 0); BM.saveExtra(); };
    $('bExport').onclick = () => download(`bitmeds-backup-${new Date().toISOString().slice(0, 10)}.json`, 'application/json', JSON.stringify({ app: 'bitmeds', v: 1, at: Date.now(), meds: BM.meds, extra: BM.extra }, null, 1));
    $('bCsv').onclick = () => {
      const rows = [['med', 'for', 'dose', 'given', 'due', 'status', 'site']];
      BM.meds.forEach(m => entries(m).forEach(e => rows.push([m.name, m.forWho || '', BM.fmtAmt(m.amount) + ' ' + m.unit, new Date(e.t).toISOString(), e.due ? new Date(e.due).toISOString() : '', e.kind === 'ok' ? 'on time' : e.kind, e.site || ''])));
      download(`bitmeds-dose-log-${new Date().toISOString().slice(0, 10)}.csv`, 'text/csv', rows.map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n'));
    };
    $('bImport').onclick = () => $('bFile').click();
    $('bFile').onchange = () => { const f = $('bFile').files[0]; if (f) f.text().then(restore); };
    if (window.BMSync) window.BMSync.paint($('syncBox')); else $('syncBox').innerHTML = '<h3>▸ SYNC + FAMILY</h3><p class="note">Sync needs an internet connection.</p>';
  }
  function download(name, mime, text) {
    if (BM.NATIVE && BM.NATIVE.saveFile) { try { BM.NATIVE.saveFile(name, mime, text); return; } catch (e) { } }
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([text], { type: mime }));
    a.download = name; document.body.appendChild(a); a.click();
    setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 1000);
  }
  function restore(text) {
    let d; try { d = JSON.parse(text); } catch (e) { return BM.toast('That file isn’t a bitmeds backup.'); }
    if (!d || d.app !== 'bitmeds' || !Array.isArray(d.meds)) return BM.toast('That file isn’t a bitmeds backup.');
    if (!confirm(`Restore ${d.meds.length} meds from ${new Date(d.at).toLocaleString()}? This replaces what's on this phone.`)) return;
    BM.meds = d.meds; BM.extra = Object.assign(BM.extra, d.extra || {});
    BM.save(); BM.saveExtra(); BM.toast('Backup restored.'); show('timers');
  }
  window.bitmedsImport = restore;   // Android file picker hands the file text here
  window.BMX = { show, entries };
})();
