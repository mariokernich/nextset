// Generates the NextSet logo, app icons and README banner.
//
//   cd Branding && npm install --no-save playwright && node build-icons.mjs
//
// Writes the SVG sources into Branding/ and the rendered PNGs into the
// asset catalogs of the iOS and watchOS targets.

import { writeFileSync, readFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '..');
const require = createRequire(import.meta.url);

let chromium;
try {
  ({ chromium } = require('playwright'));
} catch {
  ({ chromium } = require('/opt/node22/lib/node_modules/playwright'));
}

// ---------------------------------------------------------------------------
// Geometry (1024 × 1024 canvas)

// Calm mint for rest and recovery.
const MINT_LIGHT = '#6CD9B3';
const MINT = '#22A884';

const polar = (cx, cy, r, deg) => {
  const a = (deg * Math.PI) / 180;
  return [cx + r * Math.cos(a), cy + r * Math.sin(a)];
};

const arcPath = (cx, cy, r, start, end) => {
  const [x1, y1] = polar(cx, cy, r, start);
  const [x2, y2] = polar(cx, cy, r, end);
  const sweep = ((end - start) % 360 + 360) % 360;
  return `M ${x1.toFixed(1)} ${y1.toFixed(1)} A ${r} ${r} 0 ${sweep > 180 ? 1 : 0} 1 ${x2.toFixed(1)} ${y2.toFixed(1)}`;
};

/** The kettlebell-shaped stopwatch. `palette` decides the colours. */
function glyph(palette, { id = 'g' } = {}) {
  const cx = 512, cy = 632, bodyR = 292, base = 884;
  const handleTop = 172, handleWidth = 96, taper = 24, cornerR = 150, inset = 56;
  const L = cx - (bodyR - inset), R = cx + (bodyR - inset);
  const Lt = L + taper, Rt = R - taper;
  const y0 = handleTop + handleWidth / 2;
  const yBottom = cy - 40;
  const handle =
    `M ${L} ${yBottom} L ${Lt} ${y0 + cornerR} Q ${Lt} ${y0}, ${Lt + cornerR} ${y0} ` +
    `L ${Rt - cornerR} ${y0} Q ${Rt} ${y0}, ${Rt} ${y0 + cornerR} L ${R} ${yBottom}`;
  const faceR = 204, ringR = 136, ringW = 66;
  return `
  <defs>
    <linearGradient id="${id}-bell" x1="0.2" y1="0" x2="0.8" y2="1">
      <stop offset="0" stop-color="${palette.bellTop}"/><stop offset="1" stop-color="${palette.bellBottom}"/>
    </linearGradient>
    <linearGradient id="${id}-ring" x1="0.15" y1="0" x2="0.85" y2="1">
      <stop offset="0" stop-color="${palette.ringTop}"/><stop offset="1" stop-color="${palette.ringBottom}"/>
    </linearGradient>
    <radialGradient id="${id}-face" cx="0.5" cy="0.38" r="0.7">
      <stop offset="0" stop-color="${palette.faceTop}"/><stop offset="1" stop-color="${palette.faceBottom}"/>
    </radialGradient>
    <clipPath id="${id}-base"><rect x="0" y="0" width="1024" height="${base}"/></clipPath>
  </defs>
  <path d="${handle}" fill="none" stroke="url(#${id}-bell)" stroke-width="${handleWidth}" stroke-linejoin="round"/>
  <circle cx="${cx}" cy="${cy}" r="${bodyR}" fill="url(#${id}-bell)" clip-path="url(#${id}-base)"/>
  <circle cx="${cx}" cy="${cy}" r="${faceR}" fill="url(#${id}-face)"/>
  <circle cx="${cx}" cy="${cy}" r="${ringR}" fill="none" stroke="${palette.track}" stroke-width="${ringW}"/>
  <path d="${arcPath(cx, cy, ringR, -90, 180)}" fill="none" stroke="url(#${id}-ring)" stroke-width="${ringW}" stroke-linecap="round"/>`;
}

const palettes = {
  // Light icon: graphite kettlebell with a bright dial.
  standard: {
    bellTop: '#3A4846', bellBottom: '#18211F',
    ringTop: MINT_LIGHT, ringBottom: MINT,
    faceTop: '#FFFFFF', faceBottom: '#EAF5F0',
    track: 'rgba(24,60,50,0.10)',
  },
  // Dark icon: light kettlebell with a dark dial.
  dark: {
    bellTop: '#F4F8F6', bellBottom: '#C7D3CF',
    ringTop: '#A8EFD5', ringBottom: '#4FC9A0',
    faceTop: '#1F2A2C', faceBottom: '#0D1416',
    track: 'rgba(255,255,255,0.12)',
  },
  tinted: {
    bellTop: '#E6E6E6', bellBottom: '#BDBDBD',
    ringTop: '#FFFFFF', ringBottom: '#F2F2F2',
    faceTop: '#161616', faceBottom: '#050505',
    track: 'rgba(255,255,255,0.18)',
  },
};

const background = (top, bottom) => `
  <defs><linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="${top}"/><stop offset="1" stop-color="${bottom}"/>
  </linearGradient></defs>
  <rect width="1024" height="1024" fill="url(#bg)"/>`;

const svg = (body) =>
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">${body}</svg>\n`;

// Optical centring: the heavy body sits low, so lift the glyph a little.
const placed = (palette, scale = 1) =>
  `<g transform="translate(512 512) scale(${scale}) translate(-512 -536)">${glyph(palette)}</g>`;

const sources = {
  'logo.svg': svg(placed(palettes.standard)),
  'AppIcon.svg': svg(background('#F7FCFA', '#CFEEE1') + placed(palettes.standard)),
  'AppIcon-Dark.svg': svg(background('#1E2A2B', '#070B0C') + placed(palettes.dark)),
  'AppIcon-Tinted.svg': svg(background('#0A0A0A', '#000000') + placed(palettes.tinted)),
  'AppIcon-Watch.svg': svg(background('#F7FCFA', '#CFEEE1') + placed(palettes.standard, 0.86)),
};

for (const [name, content] of Object.entries(sources)) {
  writeFileSync(join(here, name), content);
}

// ---------------------------------------------------------------------------
// Rendering

const iosIcons = join(root, 'NextSet/Assets.xcassets/AppIcon.appiconset');
const watchIcons = join(root, 'NextSetWatch/Assets.xcassets/AppIcon.appiconset');

const outputs = [
  { svg: 'AppIcon.svg', png: join(iosIcons, 'AppIcon.png') },
  { svg: 'AppIcon-Dark.svg', png: join(iosIcons, 'AppIcon-Dark.png') },
  { svg: 'AppIcon-Tinted.svg', png: join(iosIcons, 'AppIcon-Tinted.png') },
  { svg: 'AppIcon-Watch.svg', png: join(watchIcons, 'AppIcon-Watch.png') },
  { svg: 'logo.svg', png: join(here, 'logo.png'), transparent: true },
];

const fontFace = (weight) => {
  try {
    const data = readFileSync(join(here, `fonts/inter-latin-${weight}-normal.woff2`)).toString('base64');
    return `@font-face{font-family:Inter;font-weight:${weight};src:url(data:font/woff2;base64,${data}) format('woff2')}`;
  } catch {
    return '';
  }
};

const banner = `<!doctype html><html><head><style>
  ${fontFace(800)}${fontFace(500)}
  html,body{margin:0}
  body{width:1600px;height:600px;display:flex;align-items:center;gap:72px;padding:0 120px;box-sizing:border-box;
       background:radial-gradient(circle at 20% 45%, rgba(108,217,179,0.35), transparent 45%),
                  radial-gradient(circle at 85% 20%, rgba(170,200,250,0.35), transparent 40%),
                  linear-gradient(160deg,#F5FBF8,#E2F2EC 60%,#EEEAFB);
       font-family:Inter,-apple-system,'Helvetica Neue',sans-serif;color:#17211F}
  .icon{width:340px;height:340px;border-radius:78px;overflow:hidden;flex:none;
        box-shadow:0 30px 70px rgba(20,70,55,.18),0 0 0 1px rgba(255,255,255,.7)}
  .icon svg{width:100%;height:100%;display:block}
  h1{font-size:164px;font-weight:800;letter-spacing:-6px;margin:0;line-height:1}
  .tag{font-size:54px;font-weight:800;letter-spacing:-1px;margin:22px 0 0;
       background:linear-gradient(90deg,${MINT_LIGHT},${MINT});-webkit-background-clip:text;color:transparent}
  .sub{font-size:32px;font-weight:500;color:#5D6B67;margin:18px 0 0}
</style></head><body>
  <div class="icon">${sources['AppIcon.svg']}</div>
  <div><h1>NextSet</h1><p class="tag">Rest. Set. Go.</p><p class="sub">Satzpausen-Timer für iPhone &amp; Apple&nbsp;Watch</p></div>
</body></html>`;

const browser = await chromium.launch();
try {
  const page = await browser.newPage({ viewport: { width: 1024, height: 1024 }, deviceScaleFactor: 1 });
  for (const { svg: name, png, transparent } of outputs) {
    mkdirSync(dirname(png), { recursive: true });
    await page.setContent(
      `<html><body style="margin:0;background:${transparent ? 'transparent' : '#000'}">${sources[name]}</body></html>`
    );
    await page.screenshot({ path: png, omitBackground: !!transparent, clip: { x: 0, y: 0, width: 1024, height: 1024 } });
    console.log('rendered', png.replace(root + '/', ''));
  }
  const bannerPage = await browser.newPage({ viewport: { width: 1600, height: 600 }, deviceScaleFactor: 1 });
  await bannerPage.setContent(banner, { waitUntil: 'load' });
  await bannerPage.evaluate(() => document.fonts.ready);
  await bannerPage.screenshot({ path: join(here, 'banner.png') });
  console.log('rendered Branding/banner.png');
} finally {
  await browser.close();
}
