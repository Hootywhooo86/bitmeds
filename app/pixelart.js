/* bitmeds pixel art: icons + the 6 widget animation styles.
 * One source of truth: the web app draws with this file, and the Android widget's Pixel.java is a
 * line-by-line port (sprite grids are generated from here by android/tools/gen_sprites.py).
 * Everything is drawn on a 52x52 grid, 4 animation frames (k = 0..3), as packed 0xRRGGBB ints.
 * Sprite chars: A / a / L = the med's colour (main / dark / light); everything else is fixed. */
(function (root) {
  'use strict';
  var G = 52;
  var PAL = { o:'#0b0b16', W:'#f4f1ff', k:'#c9c6e0', s:'#9a97b8', q:'#2a2a4a', R:'#ff5d5d', Y:'#facc15', O:'#ff9f1c' };

  var SPR = {
    capsule: ["................","................","................","................",
      "...oooooooooo...","..oAAAAAWWWWWo..",".oAAAAAAWWWWWWo.",".oAaAAAAWWWWkWo.",
      ".oAaAAAAWWWWkWo.",".oAAAAAAWWWWWWo.","..oAAAAAWWWWWo..","...oooooooooo...",
      "................","................","................","................"],
    tablet: ["................","................",".....oooooo.....","...ooLLLLLLoo...",
      "..oLWWLLLLLLLo..","..oLWLLLLLLLLo..",".oLLLLLLLLLLLLo.",".oaaaaaaaaaaaao.",
      ".oLLLLLLLLLLLLo.","..oLLLLLLLLLLo..","..oLLLLLLLLLLo..","...ooLLLLLLoo...",
      ".....oooooo.....","................","................","................"],
    bottle: ["................","......oooo......","......okko......","......okko......",
      ".....oooooo.....","....oWWWWWWo....","....oWkWWWWo....","....oLLLLLLo....",
      "....oAaAAAAo....","....oAaAAAAo....","....oAAAAAAo....","....oAAAAAAo....",
      "....oaaaaaao....",".....oooooo.....","................","................"],
    inhaler: ["................","....oooo........","....oAAo........","....oALo........",
      "....oALo........","....oALo........","....oAao........","....oAaooooooo..",
      "....oAAAAAAAAAo.","....oAAAAAAAAAo.","....oAaaaaooooo.","....oooooo......",
      "................","................","................","................"],
    drop: ["................","........o.......",".......oAo......",".......oAo......",
      "......oAAAo.....",".....oAAAAAo....","....oAAAAAAAo...","....oALAAAAAo...",
      "...oAALAAAAAAo..","...oAALAAAAAAo..","...oAAAAAAAAAo..","....oAAAAAAao...",
      ".....oaaaaao....","......ooooo.....","................","................"],
    tube: ["................","................","................","................",
      ".ooooooooooo....",".oWWWWWWWWWoooo.",".oWAAAAAAAWoaao.",".oWALAAAAAWoaao.",
      ".oWAAAAAAAWoaao.",".oWWWWWWWWWoooo.",".ooooooooooo....","................",
      "................","................","................","................"],
    patch: ["................","................","..oooooooooooo..","..oWWWWWWWWWWo..",
      "..oWkkkkkkkkWo..","..oWkooooookWo..","..oWkoAAAAokWo..","..oWkoALAAokWo..",
      "..oWkoAAAAokWo..","..oWkooooookWo..","..oWkkkkkkkkWo..","..oWWWWWWWWWWo..",
      "..oooooooooooo..","................","................","................"],
    spray: ["................",".......oo.......","......oWWo.....s","......oWWo...s..",
      ".....oooooo.....","....oWWWWWWo....","....oAAAAAAo....","....oALAAAAo....",
      "....oALAAAAo....","....oALAAAAo....","....oAAAAAAo....","....oaaaaaao....",
      ".....oooooo.....","................","................","................"],
    cross: ["................","................",".....oooooo.....",".....oAAAAo.....",
      ".....oALAAo.....","..ooooALAAoooo..","..oAAAAAAAAAAo..","..oAAAAAAAAAAo..",
      "..oaaaaAAaaaao..","..ooooaAAaoooo..",".....oaAAao.....",".....oaaaao.....",
      ".....oooooo.....","................","................","................"],
    pen: ["................","................","................","................",
      "................","...ooooooooooo..","..okkkkkoWWoAAo.","ooakkkkkoWLoAAAo",
      "ooakkkkkoWLoAAAo","..okkkkkoWWoAAo.","...ooooooooooo..","................",
      "................","................","................","................"],
    vial: ["................","......oooo......",".....oAAAAo.....",".....oaaaao.....",
      "......oWWo......",".....oWWWWo.....","....oWkWWWWo....","....oWkWWWWo....",
      "....oLLLLLLo....","....oAAAAAAo....","....oAaAAAAo....","....oAaAAAAo....",
      "....oAAAAAAo....",".....oooooo.....","................","................"],
    heart: ["................","................","...oooo..oooo...","..oAAAAooAAAAo..",
      ".oAALAAAAAAAAAo.",".oALLAAAAAAAAAo.",".oAAAAAAAAAAAAo.","..oAAAAAAAAAAo..",
      "...oAAAAAAAAo...","....oAAAAAAo....",".....oAAAAo.....","......oaao......",
      ".......oo.......","................","................","................"],
    star: ["................",".......oo.......","......oAAo......","......oAAo......",
      ".....oALAAo.....","ooooooALAAoooooo","oAAAAAAAAAAAAAAo",".oAAAAAAAAAAAAo.",
      "..oAAAAAAAAAAo..","...oAAAAAAAAo...","...oAAAAAAAAo...","..oAAAaooaAAAo..",
      "..oAAaooooaAAo..",".oAAao....oaAAo.",".ooo........ooo.","................"],
    sun: ["................",".......AA.......","..A....AA....A..","...A..oooo..A...",
      ".....oAAAAo.....","....oLLAAAAo....","AA..oLAAAAAo..AA","AA..oAAAAAAo..AA",
      "....oAAAAAAo....",".....oAAAAo.....","...A..oooo..A...","..A....AA....A..",
      ".......AA.......","................","................","................"],
    bandage: ["................","................","................","................",
      "................",".oooooooooooooo.","oLLLLoAAAAoLLLLo","oLsLLoAkAAoLLsLo",
      "oLLLLoAAkAoLLLLo","oLLsLoAAAAoLsLLo",".oooooooooooooo.","................",
      "................","................","................","................"]
  };

  // syringe: f = filled oil columns 0..6 (plunger moves), drip = 0..3
  function syringeRows(f, drip) {
    var g = [], x, y;
    for (y = 0; y < 16; y++) { g.push([]); for (x = 0; x < 16; x++) g[y].push('.'); }
    function s(x, y, c) { if (x >= 0 && x < 16 && y >= 0 && y < 16) g[y][x] = c; }
    for (x = 4; x <= 11; x++) { s(x, 4, 'o'); s(x, 9, 'o'); }
    for (y = 4; y <= 9; y++) { s(4, y, 'o'); s(11, y, 'o'); }
    s(3, 3, 'o'); s(4, 3, 'o'); s(3, 10, 'o'); s(4, 10, 'o');
    for (x = 5; x <= 10; x++) for (y = 5; y <= 8; y++) {
      var full = x >= 11 - f;
      s(x, y, full ? (y === 5 ? 'W' : y === 8 ? 'a' : 'A') : (y === 5 ? 'k' : 'q'));
    }
    for (x = 6; x <= 10; x += 2) s(x, 5, x >= 11 - f ? 'L' : 's');
    var stop = 10 - f;
    if (stop >= 5) for (y = 5; y <= 8; y++) s(stop, y, 'o');
    var plate = Math.max(0, Math.min(3, stop - 4));
    for (y = 4; y <= 9; y++) s(plate, y, 'o');
    for (x = plate + 1; x < Math.max(stop, 5); x++) { if (x === 4) continue; s(x, 6, 'k'); s(x, 7, 's'); }
    s(12, 6, 'o'); s(12, 7, 'o'); s(12, 8, 'o');
    for (x = 13; x <= 15; x++) s(x, 7, 'k');
    if (drip === 1) s(15, 8, 'A');
    if (drip === 2) { s(15, 8, 'A'); s(15, 9, 'A'); }
    if (drip === 3) s(15, 11, 'A');
    return g.map(function (r) { return r.join(''); });
  }

  var ICONS = [['syringe','SYRINGE'],['pen','PEN'],['vial','VIAL'],['capsule','CAPSULE'],['tablet','TABLET'],['bottle','BOTTLE'],
               ['inhaler','INHALER'],['drop','DROPS'],['tube','CREAM'],['patch','PATCH'],['spray','SPRAY'],['heart','HEART'],['star','STAR'],['sun','SUN'],['bandage','BANDAGE'],['cross','PLUS']];
  var TYPE_ICON = { injection:'syringe', pill:'capsule', tablet:'tablet', liquid:'bottle', inhaler:'inhaler', drops:'drop', cream:'tube', patch:'patch', spray:'spray', other:'cross' };
  var STYLES = [['ring','RING'],['battery','BATTERY'],['hourglass','HOURGLASS'],['heart','HEART'],['tank','TANK'],['hop','HOP'],['rocket','ROCKET']];
  var COLORS = ['#34d399','#60a5fa','#a78bfa','#f472b6','#ff9f1c','#facc15','#22d3ee','#a3e635','#ff5d5d','#f4f1ff'];
  var BGS = ['#101428','#050508','#1e0f2e','#0c1f17','#0a1a2f','#2a0f17'];

  function iconRows(icon, k) {
    if (icon === 'syringe') return syringeRows(6, [0, 1, 2, 3][k & 3]);
    return SPR[icon] || SPR.cross;
  }

  // ---- colour helpers (same maths in Pixel.java) ----
  function hex(c) { return [parseInt(c.slice(1, 3), 16), parseInt(c.slice(3, 5), 16), parseInt(c.slice(5, 7), 16)]; }
  function mix(c, d, t) { return [Math.round(c[0] + (d[0] - c[0]) * t), Math.round(c[1] + (d[1] - c[1]) * t), Math.round(c[2] + (d[2] - c[2]) * t)]; }
  function pack(c) { return (c[0] << 16) | (c[1] << 8) | c[2]; }
  function css(c) { return 'rgb(' + c[0] + ',' + c[1] + ',' + c[2] + ')'; }
  var WHITE = [255, 255, 255], BLACK = [0, 0, 0], OUT = [7, 7, 15], RED = [255, 93, 93], YEL = [250, 204, 21];
  function shades(c) { return [c, mix(c, WHITE, 0.45), mix(c, BLACK, 0.45)]; } // main, light, dark
  function validHex(c) { return typeof c === 'string' && /^#[0-9a-fA-F]{6}$/.test(c); }

  function charColor(ch, ac) {
    if (ch === 'A') return ac[0];
    if (ch === 'a') return ac[2];
    if (ch === 'L') return ac[1];
    var h = PAL[ch];
    return h ? hex(h) : null;
  }

  // ---- renderer ----
  function render(o) {
    var px = new Array(G * G), bg = hex(validHex(o.bg) ? o.bg : BGS[0]), i;
    for (i = 0; i < px.length; i++) px[i] = pack(bg);
    var ac = shades(hex(validHex(o.accent) ? o.accent : COLORS[0]));
    var st = o.state | 0, k = o.k & 3, frac = Math.max(0, Math.min(1, o.frac));
    var T = st === 2 ? shades(RED) : st === 1 ? shades(YEL) : ac;
    var TR = [mix(bg, WHITE, 0.10), mix(bg, WHITE, 0.18), mix(bg, BLACK, 0.35)];
    var DIM = [mix(T[0], bg, 0.72), mix(T[0], bg, 0.6), mix(T[0], BLACK, 0.72)];
    var c = { px: px, k: k, frac: frac, st: st, T: T, TR: TR, DIM: DIM, ac: ac, icon: o.icon || 'syringe',
              wob: st === 2 ? ((k & 1) === 1 ? 1 : -1) : 0,
              bob: st !== 2 && o.icon !== 'syringe' && (k & 1) === 1 ? -1 : 0 };
    var fn = { ring: ring, battery: battery, hourglass: hourglass, heart: heart, tank: tank, hop: hop, rocket: rocket }[o.style] || ring;
    fn(c);
    return px;
  }
  function put(px, x, y, col) { if (x >= 0 && x < G && y >= 0 && y < G) px[y * G + x] = pack(col); }
  function icon(c, ox, oy, scale) {
    var rows = iconRows(c.icon, c.k), x, y, a, b;
    for (y = 0; y < 16; y++) for (x = 0; x < 16; x++) {
      var col = charColor(rows[y].charAt(x), c.ac);
      if (!col) continue;
      for (a = 0; a < scale; a++) for (b = 0; b < scale; b++) put(c.px, ox + x * scale + b, oy + y * scale + a, col);
    }
  }
  function fmod(a, n) { return ((a % n) + n) % n; }

  // 1. RING — 36 blocks fill clockwise, head blinks, spark orbits; red chase when due
  function ring(c) {
    var N = 36, c0 = (G - 1) / 2, rOut = 25.0, rIn = 20.0, filled = Math.floor(c.frac * N), blink = (c.k & 1) === 0, chase = c.k * 9;
    var mask = new Array(G * G), col = new Array(G * G), x, y, i;
    for (y = 0; y < G; y++) for (x = 0; x < G; x++) {
      var dx = x - c0, dy = y - c0, d = Math.sqrt(dx * dx + dy * dy);
      if (d < rIn || d > rOut) continue;
      var t = (Math.atan2(dy, dx) + Math.PI / 2) / (Math.PI * 2);
      t = (t + 1) % 1;
      var seg = Math.floor(t * N);
      if ((t * N) % 1 > 0.74) continue;
      var pal = c.TR;
      if (c.st === 2) pal = fmod(seg - chase, N) < 9 ? c.T : c.DIM;
      else if (seg < filled) pal = c.T;
      else if (seg === filled) pal = blink ? c.T : c.TR;
      var shade = d > rOut - 1.2 ? 1 : d < rIn + 1.2 ? 2 : 0;
      mask[y * G + x] = true; col[y * G + x] = pal[shade];
    }
    for (y = 0; y < G - 1; y++) for (x = 0; x < G - 1; x++)
      if (mask[y * G + x] && !mask[(y + 1) * G + x + 1]) put(c.px, x + 1, y + 1, OUT);
    for (i = 0; i < G * G; i++) if (mask[i]) c.px[i] = pack(col[i]);
    if (c.st !== 2) {
      var a = -Math.PI / 2 + c.k / 4 * Math.PI * 2 + c.frac * Math.PI * 2;
      var sx = Math.round(c0 + Math.cos(a) * 17.5), sy = Math.round(c0 + Math.sin(a) * 17.5);
      put(c.px, sx, sy, c.T[1]); put(c.px, sx + 1, sy, c.T[1]); put(c.px, sx, sy + 1, c.T[1]); put(c.px, sx + 1, sy + 1, c.T[1]);
    }
    icon(c, 10 + c.wob, 10 + c.bob, 2);
  }

  // 2. BATTERY — 9 cells charge up toward the dose, a glint runs through; cells flash when due
  function battery(c) {
    var x, y, i;
    for (x = 4; x <= 45; x++) for (y = 20; y <= 43; y++)
      put(c.px, x + c.wob, y, (x < 6 || x > 43 || y < 22 || y > 41) ? OUT : c.TR[2]);
    for (x = 46; x <= 48; x++) for (y = 27; y <= 36; y++) put(c.px, x + c.wob, y, (x < 48 && y > 28 && y < 35) ? c.TR[1] : OUT);
    var filled = Math.floor(c.frac * 9), glint = filled > 0 ? (c.k * 2) % filled : -1;
    for (i = 0; i < 9; i++) {
      var pal = null;
      if (c.st === 2) pal = (i + c.k) % 2 === 0 ? c.T : c.DIM;
      else if (i < filled) pal = c.T;
      else if (i === filled && (c.k & 1) === 0) pal = c.T;
      if (!pal) continue;
      var x0 = 7 + i * 4;
      for (x = x0; x <= x0 + 2; x++) for (y = 23; y <= 40; y++)
        put(c.px, x + c.wob, y, y === 23 ? pal[1] : y === 40 ? pal[2] : (c.st !== 2 && i === glint && x === x0 + 1) ? pal[1] : pal[0]);
    }
    icon(c, 18 + c.wob, 2 + c.bob, 1);
  }

  // 3. HOURGLASS — sand runs from the top bulb to the bottom as the dose gets closer
  function hourglass(c) {
    var cx = 19 + c.wob, x, y;
    for (x = 6; x <= 32; x++) for (y = 3; y <= 5; y++) { put(c.px, x + c.wob, y, y === 3 ? c.T[1] : c.T[2]); put(c.px, x + c.wob, y + 43, y === 3 ? c.T[1] : c.T[2]); }
    for (y = 6; y <= 45; y++) { put(c.px, 7 + c.wob, y, c.T[2]); put(c.px, 31 + c.wob, y, c.T[2]); }
    var hTop = Math.round((1 - c.frac) * 15), hBot = Math.round(c.frac * 15);
    for (y = 7; y <= 44; y++) {
      var d = Math.abs(y - 25.5), w = Math.min(10, Math.floor(d * 0.75) + 1), edge = y === 7 || y === 44;
      for (x = cx - w - 1; x <= cx + w + 1; x++) {
        var outline = edge || x === cx - w - 1 || x === cx + w + 1, col = outline ? c.TR[1] : c.TR[2];
        if (!outline) {
          if (y <= 25 && 25 - y < hTop) col = (25 - y === hTop - 1) ? c.T[1] : c.T[0];
          if (y >= 26 && y > 43 - hBot) col = (y === 44 - hBot) ? c.T[1] : c.T[0];
        }
        put(c.px, x, y, col);
      }
    }
    if (c.st !== 2 && hTop > 0) for (y = 26; y <= 43 - hBot; y++) if ((y + c.k * 2) % 4 < 2) put(c.px, cx, y, c.T[1]);
    icon(c, 35, 18 + c.bob, 1);
  }

  // 4. HEART — beats, and fills up from the bottom as the dose gets closer
  function heart(c) {
    var s = c.st === 2 ? [1, 1.09, 0.98, 1.09][c.k] : [1, 1.07, 1, 0.96][c.k], R = 19 * s, cy = 27.75, x, y;
    var m = new Array(G * G), minY = G, maxY = -1;
    for (y = 0; y < G; y++) for (x = 0; x < G; x++) {
      var nx = (x - 25.5) / R, ny = (cy - y) / R, a = nx * nx + ny * ny - 1;
      if (a * a * a - nx * nx * ny * ny * ny <= 0) { m[y * G + x] = true; if (y < minY) minY = y; if (y > maxY) maxY = y; }
    }
    var level = maxY - c.frac * (maxY - minY), surf = Math.ceil(level);
    for (y = 0; y < G; y++) for (x = 0; x < G; x++) {
      if (!m[y * G + x]) continue;
      var edge = x === 0 || y === 0 || x === G - 1 || y === G - 1 || !m[y * G + x - 1] || !m[y * G + x + 1] || !m[(y - 1) * G + x] || !m[(y + 1) * G + x];
      if (edge) { put(c.px, x, y, OUT); continue; }
      var full = y >= level, nx2 = (x - 25.5) / R, ny2 = (cy - y) / R;
      var shine = nx2 < -0.42 && nx2 > -0.66 && ny2 > 0.5 && ny2 < 0.7 && !(nx2 < -0.58 && ny2 > 0.62);
      put(c.px, x, y, full ? (shine || y === surf ? c.T[1] : c.T[0]) : (shine ? c.TR[1] : c.TR[0]));
    }
    icon(c, 18 + c.wob, 17 + c.bob, 1);
  }

  // 5. TANK — liquid rises with a rolling wave and bubbles; the icon floats on top
  function tank(c) {
    var WAVE = [0, 0, 1, 1, 0, 0, -1, -1], x, y, i;
    for (x = 7; x <= 44; x++) for (y = 5; y <= 47; y++)
      put(c.px, x, y, (x < 9 || x > 42 || y < 7 || y > 45) ? OUT : c.TR[2]);
    for (y = 9; y < 45; y += 6) { put(c.px, 40, y, c.TR[1]); put(c.px, 41, y, c.TR[1]); }
    var h = Math.round(c.frac * 39), sy = 46 - h;
    if (h > 0) for (x = 9; x <= 42; x++) {
      var top = h >= 39 ? 7 : Math.max(7, sy + WAVE[(x + c.k * 2) % 8]);
      for (y = top; y <= 45; y++) put(c.px, x, y, y === top ? ((c.st === 2 && (c.k & 1) === 1) ? c.T[0] : c.T[1]) : y >= 44 ? c.T[2] : x === 11 ? c.T[1] : c.T[0]);
    }
    if (h > 8) for (i = 0; i < 3; i++) {
      var bx = 15 + i * 9, by = 45 - ((c.k * 3 + i * 7) % (h - 4));
      if (by - 1 > sy + 1) { put(c.px, bx, by, c.T[1]); put(c.px, bx + 1, by, c.T[1]); put(c.px, bx, by - 1, c.T[1]); put(c.px, bx + 1, by - 1, c.T[1]); }
    }
    var iy = Math.max(1, Math.min(30, sy - 15 + ((c.k & 1) === 1 ? 1 : 0)));
    icon(c, 18 + c.wob, iy, 1);
  }

  // 6. HOP — the icon hops along a row of blocks that light up as the dose gets closer
  function hop(c) {
    var JUMP = c.st === 2 ? [-6, 0, -6, 0] : [0, -3, -5, -3], x, y, i;
    var filled = Math.floor(c.frac * 8);
    for (i = 0; i < 8; i++) {
      var x0 = 2 + i * 6, pal = i < filled || (c.st === 2) ? c.T : (i === filled && (c.k & 1) === 0) ? c.T : null;
      if (c.st === 2 && (i + c.k) % 2 === 1) pal = c.DIM;
      for (x = x0; x <= x0 + 4; x++) for (y = 40; y <= 44; y++) {
        var col = pal ? (y === 40 ? pal[1] : y === 44 ? pal[2] : pal[0]) : (y === 40 ? c.TR[1] : c.TR[0]);
        put(c.px, x, y, col);
      }
      if (pal) put(c.px, x0 + 2, 42, pal[2]);
      for (x = x0; x <= x0 + 4; x++) put(c.px, x + 1, 45, OUT);
    }
    for (x = 0; x < G; x++) { put(c.px, x, 46, c.TR[2]); put(c.px, x, 47, c.TR[2]); }
    var ix = 1 + Math.round(c.frac * 18), jump = JUMP[c.k], iy = 10 + jump;
    var sw = 10 + jump; // shadow shrinks while airborne
    for (x = ix + 16 - (sw >> 1); x < ix + 16 + (sw >> 1); x++) put(c.px, x, 39, c.TR[2]);
    if (c.st !== 2 && c.frac > 0) for (i = 0; i < 3; i++) put(c.px, ix - 2 - i * 3, iy + 18 + ((c.k + i) % 3) * 3, c.T[1]);
    icon(c, ix, iy, 2);
    if (c.st === 2) {
      var ex = Math.min(ix + 33, 48);
      for (y = 4; y <= 9; y++) { put(c.px, ex, y, c.T[0]); put(c.px, ex + 1, y, c.T[0]); }
      put(c.px, ex, 11, c.T[0]); put(c.px, ex + 1, 11, c.T[0]);
    }
  }

  // 7. ROCKET — the icon blasts off up a dotted trail toward the moon as the dose gets closer
  function rocket(c) {
    var x, y, i, ST = [[4, 6], [13, 3], [26, 8], [47, 24], [33, 30], [45, 40], [7, 38], [29, 44], [16, 18], [49, 4], [3, 26], [36, 18]];
    for (i = 0; i < ST.length; i++) if ((i + c.k) % 3 !== 0) put(c.px, ST[i][0], ST[i][1], c.TR[1]);
    var moon = c.st === 2 ? c.T : [[201, 198, 224], [244, 241, 255], [120, 117, 150]];
    for (y = 2; y <= 18; y++) for (x = 33; x <= 49; x++) {
      var dx = x - 41, dy = y - 10, d = dx * dx + dy * dy;
      if (d > 49) continue;
      var col = d >= 37 ? moon[2] : (dx < -1 && dy < -1 && d < 26) ? moon[1] : moon[0];
      if ((x === 43 && y === 8) || (x === 39 && y === 12) || (x === 44 && y === 13) || (x === 40 && y === 7)) col = moon[2];
      put(c.px, x, y, col);
    }
    for (x = 0; x < G; x++) for (y = 48; y < G; y++) put(c.px, x, y, y === 48 ? c.TR[1] : c.TR[2]);
    for (x = 12; x <= 31; x++) put(c.px, x, 47, c.TR[1]);
    var iy = 20 - Math.round(c.frac * 22) + (c.st === 2 ? 0 : (c.k & 1) === 1 ? -1 : 0), ix = 6 + c.wob;
    for (y = iy + 30; y < 47; y += 3) put(c.px, 22, y, y < iy + 38 ? c.T[1] : c.T[2]);
    if (c.st !== 2 && c.frac < 1) {
      var len = [3, 5, 4, 6][c.k], FL = [[250, 204, 21], [255, 159, 28], [255, 93, 93]];
      for (y = 0; y < len; y++) for (x = 20; x <= 23; x++) {
        if ((x === 20 || x === 23) && y > len - 3) continue;
        put(c.px, x, iy + 25 + y, FL[Math.min(2, Math.floor(y / 2))]);
      }
    }
    icon(c, ix, iy, 2);
    if (c.st === 2 && (c.k & 1) === 0) { for (y = 22; y <= 27; y++) { put(c.px, 44, y, c.T[0]); put(c.px, 45, y, c.T[0]); } put(c.px, 44, 29, c.T[0]); put(c.px, 45, 29, c.T[0]); }
  }

  // SYRINGE GAUGE (widget layout): a tall syringe whose barrel fills with the med as dose time gets
  // closer. 24 x 60 grid, 4 frames (rolling surface, rising bubbles, drip). Returns packed 0xRRGGBB pixels.
  var GW = 24, GH = 60;
  function gauge(o) {
    var px = new Array(GW * GH), bg = hex(validHex(o.bg) ? o.bg : BGS[0]), i, x, y;
    for (i = 0; i < px.length; i++) px[i] = pack(bg);
    var st = o.state | 0, k = o.k & 3, frac = Math.max(0, Math.min(1, o.frac));
    var ac = shades(hex(validHex(o.accent) ? o.accent : COLORS[0]));
    var T = st === 2 ? shades(RED) : st === 1 ? shades(YEL) : ac;
    var TR = [mix(bg, WHITE, 0.10), mix(bg, WHITE, 0.18), mix(bg, BLACK, 0.35)];
    var K = hex(PAL.k), S = hex(PAL.s), W = hex(PAL.W);
    function p(x, y, c) { if (x >= 0 && x < GW && y >= 0 && y < GH) px[y * GW + x] = pack(c); }
    var top = 11, bot = 47, inner = bot - top - 1;                          // barrel rows (inside: top+1 .. bot-1)
    var lvl = Math.round(frac * inner), surf = bot - lvl;                   // first liquid row
    var stop = Math.max(top + 1, surf - 2);                                 // rubber stopper rests on the liquid
    for (x = 4; x <= 19; x++) { p(x, 0, OUT); p(x, 2, OUT); p(x, 1, x === 4 || x === 19 ? OUT : K); }   // thumb plate
    for (x = 1; x <= 22; x++) { p(x, 9, OUT); p(x, 10, x === 1 || x === 22 ? OUT : S); }               // finger flange
    for (y = top; y <= bot; y++) { p(4, y, OUT); p(19, y, OUT); }
    for (x = 4; x <= 19; x++) p(x, bot, OUT);
    for (y = top + 1; y < bot; y++) for (x = 5; x <= 18; x++) {
      var c = TR[2];
      if (y >= surf) c = y === surf ? T[1] : (x === 6 ? T[1] : y >= bot - 2 ? T[2] : T[0]);
      if (y === surf && ((x + k * 2) % 6 < 2)) c = T[0];                    // rolling surface
      p(x, y, c);
    }
    for (y = stop; y < Math.min(stop + 2, surf); y++) for (x = 5; x <= 18; x++) p(x, y, OUT);     // stopper
    for (y = top + 3; y < bot; y += 4) { p(16, y, y < surf ? S : T[1]); p(17, y, y < surf ? S : T[1]); if ((y - top) % 8 === 3) p(15, y, y < surf ? S : T[1]); }  // marks
    for (y = top + 2; y < bot - 1; y++) if (y < surf - 2 || y > surf) p(7, y, y < surf ? TR[1] : W);  // glass shine
    for (y = 3; y < stop; y++) { p(10, y, OUT); p(13, y, OUT); p(11, y, K); p(12, y, S); }             // plunger rod (over the glass)
    if (lvl > 6) for (i = 0; i < 2; i++) {                                  // bubbles
      var bx = 9 + i * 5, by = bot - 2 - ((k * 3 + i * 5) % Math.max(1, lvl - 3));
      if (by > surf + 1) p(bx, by, T[1]);
    }
    for (y = bot + 1; y <= bot + 3; y++) for (x = 9; x <= 14; x++) p(x, y, x === 9 || x === 14 ? OUT : K);  // hub
    for (y = bot + 4; y < GH - 2; y++) { p(11, y, K); p(12, y, S); }                                  // needle
    if (st === 2 || frac >= 0.85) { var dy = [0, 1, 2, -1][k]; if (dy >= 0) { p(11, GH - 2 + Math.min(dy, 1), T[0]); p(12, GH - 2 + Math.min(dy, 1), T[0]); } }
    return { w: GW, h: GH, px: px };
  }

  // ---- web helpers ----
  function iconSvg(iconKey, size, accent, rows) {
    var ac = shades(hex(validHex(accent) ? accent : COLORS[0])), r = '', y, x;
    rows = rows || iconRows(iconKey, 0);
    for (y = 0; y < 16; y++) {
      x = 0;
      while (x < 16) {
        var col = charColor(rows[y].charAt(x), ac);
        if (!col) { x++; continue; }
        var w = 1;
        while (x + w < 16 && rows[y].charAt(x + w) === rows[y].charAt(x)) w++;
        r += '<rect x="' + x + '" y="' + y + '" width="' + w + '" height="1" fill="' + css(col) + '"/>';
        x += w;
      }
    }
    return '<svg class="sprite" width="' + size + '" height="' + size + '" viewBox="0 0 16 16" aria-hidden="true">' + r + '</svg>';
  }
  var buf = null, bctx = null, img = null;
  function paint(canvas, opts, dpr) {
    if (typeof document === 'undefined') return;
    if (!buf) { buf = document.createElement('canvas'); buf.width = buf.height = G; bctx = buf.getContext('2d'); img = bctx.createImageData(G, G); }
    var css = canvas.clientWidth; if (!css) return;
    var size = Math.round(css * (dpr || 1));
    if (canvas.width !== size) { canvas.width = canvas.height = size; }
    var px = render(opts), d = img.data;
    for (var i = 0; i < px.length; i++) { d[i * 4] = px[i] >> 16 & 255; d[i * 4 + 1] = px[i] >> 8 & 255; d[i * 4 + 2] = px[i] & 255; d[i * 4 + 3] = 255; }
    bctx.putImageData(img, 0, 0);
    var cx = canvas.getContext('2d'); cx.imageSmoothingEnabled = false; cx.drawImage(buf, 0, 0, size, size);
  }

  var gbuf = null;
  function paintGauge(canvas, opts, dpr) {
    if (typeof document === 'undefined') return;
    if (!gbuf) { gbuf = document.createElement('canvas'); gbuf.width = GW; gbuf.height = GH; }
    var cw = canvas.clientWidth, chh = canvas.clientHeight; if (!cw || !chh) return;
    var w = Math.round(cw * (dpr || 1)), h = Math.round(chh * (dpr || 1));
    if (canvas.width !== w || canvas.height !== h) { canvas.width = w; canvas.height = h; }
    var g = gauge(opts), bx = gbuf.getContext('2d'), im = bx.createImageData(GW, GH), d = im.data;
    for (var i = 0; i < g.px.length; i++) { d[i * 4] = g.px[i] >> 16 & 255; d[i * 4 + 1] = g.px[i] >> 8 & 255; d[i * 4 + 2] = g.px[i] & 255; d[i * 4 + 3] = 255; }
    bx.putImageData(im, 0, 0);
    var cx = canvas.getContext('2d'); cx.imageSmoothingEnabled = false; cx.drawImage(gbuf, 0, 0, w, h);
  }

  var api = { G: G, PAL: PAL, SPR: SPR, ICONS: ICONS, TYPE_ICON: TYPE_ICON, STYLES: STYLES, COLORS: COLORS, BGS: BGS,
              syringeRows: syringeRows, iconRows: iconRows, render: render, paint: paint, iconSvg: iconSvg, gauge: gauge, paintGauge: paintGauge,
              hex: hex, mix: mix, shades: shades, css: css, validHex: validHex };
  if (typeof module !== 'undefined' && module.exports) module.exports = api; else root.PixelArt = api;
})(this);
