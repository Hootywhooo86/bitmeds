#!/usr/bin/env bash
# Renders every widget style x icon x state x frame with app/pixelart.js (node) and with
# Pixel.java (plain JVM) and checks they match pixel for pixel.  Run from the repo root.
set -euo pipefail
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
cat > "$T/r.js" <<'JS'
const P = require(process.argv[2]); const out = [];
const accents = ['#34d399', '#f472b6', '#00e5ff'], bgs = ['#101428', '#1e0f2e', '#f4f1ff'];
for (const [st] of P.STYLES) for (const [ic] of P.ICONS) for (let state = 0; state < 3; state++) for (const frac of [0, 0.37, 0.81, 1]) for (let k = 0; k < 4; k++) {
  const a = accents[(ic.length + k) % 3], b = bgs[(st.length + state) % 3];
  out.push([st, ic, a, b, frac, state, k].join(' ') + ' ' + P.render({ style: st, icon: ic, accent: a, bg: b, frac, state, k }).join(','));
}
require('fs').writeFileSync(process.argv[3], out.join('\n'));
JS
node "$T/r.js" "$PWD/app/pixelart.js" "$T/js.txt"
mkdir -p "$T/src/com/bitmeds/app" "$T/out"
cp android/app/src/main/java/com/bitmeds/app/{Pixel,SpriteData}.java "$T/src/com/bitmeds/app/"
cat > "$T/src/com/bitmeds/app/Parity.java" <<'JAVA'
package com.bitmeds.app;
import java.nio.file.*; import java.util.*;
public class Parity {
  public static void main(String[] a) throws Exception {
    int frames = 0, bad = 0, badPx = 0;
    for (String line : Files.readAllLines(Paths.get(a[0]))) {
      String[] p = line.split(" ");
      int[] want = Arrays.stream(p[7].split(",")).mapToInt(Integer::parseInt).toArray();
      int[] got = Pixel.render(p[0], p[1], p[2], p[3], Double.parseDouble(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]));
      int diff = 0; for (int i = 0; i < got.length; i++) if (got[i] != want[i]) diff++;
      frames++; if (diff > 0) { bad++; badPx += diff; if (bad <= 10) System.out.println("MISMATCH " + String.join(" ", Arrays.copyOf(p, 7)) + " : " + diff + " px"); }
    }
    System.out.println(frames + " frames compared, " + bad + " differ (" + badPx + " px)");
    if (bad > 0) System.exit(1);
  }
}
JAVA
javac -d "$T/out" "$T"/src/com/bitmeds/app/*.java
java -cp "$T/out" com.bitmeds.app.Parity "$T/js.txt"
