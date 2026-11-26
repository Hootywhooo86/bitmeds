/* bitmeds sync: sign in with the 128bit family account (Supabase project "128bit play") to keep meds,
 * history, labs and settings the same on every phone, and optionally post "dose taken" to the 128bit
 * family feed. Everything still works offline / signed out — sync is optional.
 * Server setup: supabase/bitmeds.sql (table bitmeds_state, one row per account, row level security). */
(function () {
  'use strict';
  const BM = window.BM;
  const URL_ = 'https://wvflpwtaszpstmjpkrhu.supabase.co';
  const KEY = 'sb_publishable_dO8uP74cTV1tbuqESgxysw_323C7lWh';   // publishable key: safe in an app, access is limited by row level security
  const LIB = 'https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2.45.4/dist/umd/supabase.min.js';
  let sb = null, user = null, status = 'off', lastSync = 0, pushTimer = null, box = null, loading = null, tableMissing = false;
  const esc = BM.esc;

  function load() {
    if (sb) return Promise.resolve(sb);
    if (loading) return loading;
    loading = new Promise((res, rej) => {
      const s = document.createElement('script'); s.src = LIB; s.async = true;
      s.onload = () => { try { sb = window.supabase.createClient(URL_, KEY, { auth: { persistSession: true, autoRefreshToken: true, storageKey: 'bitmeds.auth' } }); res(sb); } catch (e) { rej(e); } };
      s.onerror = () => { loading = null; rej(new Error('offline')); };
      document.head.appendChild(s);
    });
    return loading;
  }

  // ---- merge: newest version of each med wins (field "u"); deletions are remembered in extra.deleted
  function merge(remote) {
    const local = { meds: BM.meds, extra: BM.extra };
    const del = Object.assign({}, (remote.extra || {}).deleted || {}, local.extra.deleted || {});
    Object.keys((remote.extra || {}).deleted || {}).forEach(k => { del[k] = Math.max(del[k] || 0, remote.extra.deleted[k]); });
    const byId = {};
    [...(remote.meds || []), ...local.meds].forEach(m => {
      if (!m || !m.id) return;
      const cur = byId[m.id];
      if (!cur || (m.u || 0) > (cur.u || 0)) byId[m.id] = m;
    });
    const meds = Object.values(byId).filter(m => !(del[m.id] && del[m.id] >= (m.u || 0)));
    const rx = remote.extra || {}, lx = local.extra;
    const newer = (rx.u || 0) > (lx.u || 0) ? rx : lx;
    const union = (a, b) => { const o = {}; [...(a || []), ...(b || [])].forEach(x => { if (x && x.id && !del[x.id]) o[x.id] = x; }); return Object.values(o); };
    const extra = Object.assign({}, lx, {
      settings: Object.assign({}, lx.settings, newer.settings || {}),
      trip: newer.trip, profiles: union(rx.profiles, lx.profiles), labs: union(rx.labs, lx.labs), deleted: del,
      u: Math.max(rx.u || 0, lx.u || 0), sync: lx.sync
    });
    return { meds, extra };
  }
  const sig = d => JSON.stringify({ m: d.meds.map(m => [m.id, m.u || 0, m.nextAt]).sort(), x: [d.extra.u, (d.extra.labs || []).length, (d.extra.profiles || []).length] });

  async function syncNow(quiet) {
    if (!user) return;
    try {
      status = 'syncing'; paint();
      const { data, error } = await sb.from('bitmeds_state').select('data, updated_at').eq('user_id', user.id).maybeSingle();
      if (error) throw error;
      const before = sig({ meds: BM.meds, extra: BM.extra });
      const merged = data && data.data ? merge(data.data) : { meds: BM.meds, extra: BM.extra };
      if (sig(merged) !== before) {
        BM.meds = merged.meds; BM.extra = Object.assign(BM.extra, merged.extra);
        applying = true; BM.save(); BM.saveExtra(); applying = false;
        BM.render();
      }
      const payload = { meds: merged.meds, extra: Object.assign({}, merged.extra, { sync: undefined }), app: 'bitmeds', v: 1 };
      const up = await sb.from('bitmeds_state').upsert({ user_id: user.id, data: payload, updated_at: new Date().toISOString() });
      if (up.error) throw up.error;
      tableMissing = false; lastSync = Date.now(); status = 'ok';
    } catch (e) {
      tableMissing = /bitmeds_state|relation|schema cache|PGRST205|42P01/i.test(String(e.message || e.code || e));
      status = 'error';
      if (!quiet) BM.toast(tableMissing ? 'Sync isn’t switched on in Supabase yet (supabase/bitmeds.sql).' : 'Sync failed — will retry.');
    }
    paint();
  }
  let applying = false;
  function schedulePush() { if (!user || applying) return; clearTimeout(pushTimer); pushTimer = setTimeout(() => syncNow(true), 2500); }

  // family feed: "dose taken" events for 128bitlife quests / the family timeline (only if switched on)
  async function familyEvent(m) {
    if (!user || !BM.extra.settings.family) return;
    try {
      await sb.rpc('log_events', { events: [{ id: 'bitmeds-' + m.id + '-' + m.log[0], app: 'bitmeds', type: 'med.taken', at: m.log[0],
        data: { med: m.name, for: m.forWho || '', streak: BM.streakOf(m) } }] });
    } catch (e) { }
  }

  BM.onChange((kind, m) => {
    if (kind === 'meds' || kind === 'extra') schedulePush();
    if (kind === 'given') familyEvent(m);
    if (kind === 'reload' && user) syncNow(true);
  });
  document.addEventListener('visibilitychange', () => { if (!document.hidden && user && Date.now() - lastSync > 30000) syncNow(true); });

  async function start() {
    try {
      await load();
      const { data } = await sb.auth.getSession();
      user = data.session ? data.session.user : null;
      sb.auth.onAuthStateChange((_e, session) => { const was = user; user = session ? session.user : null; if (user && !was) syncNow(); paint(); });
      if (user) syncNow(true);
    } catch (e) { status = 'offline'; }
    paint();
  }

  // ---- UI (MORE tab)
  let mode = 'password';
  function paint(el) {
    if (el) box = el;
    if (!box || !document.body.contains(box)) return;
    const fam = BM.extra.settings.family;
    if (!sb && status === 'offline') { box.innerHTML = `<h3>▸ SYNC + FAMILY</h3><p class="note">Sync needs an internet connection. Everything still works on this phone.</p><button class="xbtn" id="syRetry">↻ TRY AGAIN</button>`; box.querySelector('#syRetry').onclick = () => { status = 'off'; start(); }; return; }
    if (!user) {
      box.innerHTML = `<h3>▸ SYNC + FAMILY</h3>
        <p class="note" style="margin:0 0 12px">Sign in with your 128bit family account to keep bitmeds the same on every phone — meds, history, labs and settings. Optional: share "dose taken" with the family feed.</p>
        <form class="xform" id="syForm" autocomplete="on">
          <div class="field"><label for="syEmail">Email</label><input id="syEmail" type="email" autocomplete="email" inputmode="email" required></div>
          ${mode === 'password' ? `<div class="field"><label for="syPass">Password</label><input id="syPass" type="password" autocomplete="current-password" required></div>` : ''}
          ${mode === 'code' ? `<div class="field"><label for="syCode">Code from the email <span style="font-weight:400">(after you tap SEND CODE)</span></label><input id="syCode" inputmode="numeric" autocomplete="one-time-code"></div>` : ''}
          <div style="display:flex;gap:10px;flex-wrap:wrap">
            ${mode === 'password' ? `<button class="xbtn go" type="submit">SIGN IN</button><button class="xbtn" type="button" id="sySignup">CREATE ACCOUNT</button>` : `<button class="xbtn" type="button" id="sySend">SEND CODE</button><button class="xbtn go" type="submit">SIGN IN</button>`}
          </div>
          <button class="xbtn" type="button" id="syMode" style="align-self:flex-start">${mode === 'password' ? 'USE AN EMAIL CODE INSTEAD' : 'USE A PASSWORD INSTEAD'}</button>
        </form>`;
      const $ = id => box.querySelector('#' + id);
      $('syMode').onclick = () => { mode = mode === 'password' ? 'code' : 'password'; paint(); };
      if ($('sySignup')) $('sySignup').onclick = async () => {
        const email = $('syEmail').value.trim(), password = $('syPass').value;
        if (!email || password.length < 8) return BM.toast('Enter your email and a password of 8+ characters.');
        const { error } = await sb.auth.signUp({ email, password });
        BM.toast(error ? error.message : 'Account made — check your email to confirm, then sign in.');
      };
      if ($('sySend')) $('sySend').onclick = async () => {
        const email = $('syEmail').value.trim(); if (!email) return BM.toast('Enter your email first.');
        const { error } = await sb.auth.signInWithOtp({ email, options: { shouldCreateUser: true } });
        BM.toast(error ? error.message : 'Code sent — check your email.');
      };
      $('syForm').onsubmit = async e => {
        e.preventDefault();
        const email = $('syEmail').value.trim();
        const r = mode === 'password' ? await sb.auth.signInWithPassword({ email, password: $('syPass').value })
          : await sb.auth.verifyOtp({ email, token: ($('syCode').value || '').trim(), type: 'email' });
        if (r.error) return BM.toast(r.error.message);
        BM.toast('Signed in — syncing…');
      };
      return;
    }
    const when = lastSync ? new Date(lastSync).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' }) : '—';
    box.innerHTML = `<h3>▸ SYNC + FAMILY</h3>
      <div class="rows"><div class="${status === 'error' ? 'miss' : 'ok'}"><span>${esc(user.email || 'Signed in')}<br><small>${status === 'syncing' ? 'Syncing…' : status === 'error' ? (tableMissing ? 'Sync isn’t switched on in Supabase yet — run supabase/bitmeds.sql once.' : 'Couldn’t sync — will retry') : 'Synced ' + when}</small></span><b>${status === 'error' ? 'ERROR' : 'ON'}</b></div></div>
      <label class="check" style="margin-top:12px"><input type="checkbox" id="syFam" ${fam ? 'checked' : ''}><span>Share "dose taken" with the 128bit family feed (med name + streak only) for quests + the family timeline</span></label>
      <div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:12px"><button class="xbtn go" id="syNow">↻ SYNC NOW</button><button class="xbtn" id="syOut">SIGN OUT</button></div>
      <p class="note">Your data is stored in the family's Supabase project, readable only by your account.</p>`;
    box.querySelector('#syNow').onclick = () => syncNow();
    box.querySelector('#syOut').onclick = async () => { await sb.auth.signOut(); user = null; status = 'off'; paint(); };
    box.querySelector('#syFam').onchange = e => { BM.extra.settings.family = e.target.checked; BM.saveExtra(); };
  }

  window.BMSync = { paint: el => { paint(el); if (!sb && status !== 'offline') start(); }, syncNow, merge };
  start();
})();
