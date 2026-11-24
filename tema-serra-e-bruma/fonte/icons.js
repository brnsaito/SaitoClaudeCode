// Pacote de ícones 'Serra & Bruma' — glifos lineares em grade 24, renderizados a 512 px.
// Uso: node icons.js <saida_dir>
const { chromium } = require('playwright');
const fs = require('fs'); const path = require('path');
const OUT = process.argv[2] || '.';

const C = { paper: '#E4D2A8', ink: '#3D2B1F', moss: '#5B6B45', gold: '#B8935A', rust: '#A5502E' };

// [arquivo, nome, sugestão de app, svg interno]
const G = [
  ['telefone', 'Telefone', 'Telefone', '<path d="M6 4h2.6l1.4 3.8-1.9 1.4a10.5 10.5 0 0 0 6.7 6.7l1.4-1.9 3.8 1.4V18a2 2 0 0 1-2 2A15 15 0 0 1 4 6a2 2 0 0 1 2-2z"/>'],
  ['mensagens', 'Mensagens', 'Mensagens', '<path d="M4 6.5A2.5 2.5 0 0 1 6.5 4h11A2.5 2.5 0 0 1 20 6.5v7a2.5 2.5 0 0 1-2.5 2.5H10l-4.5 4v-4A2.5 2.5 0 0 1 4 13.5z"/><path d="M8 8.5h8M8 11.5h5"/>'],
  ['contatos', 'Contatos', 'Contatos', '<circle cx="12" cy="9" r="3.5"/><path d="M5.5 19.5c.9-3.6 3.5-5.5 6.5-5.5s5.6 1.9 6.5 5.5"/>'],
  ['camera', 'Câmera', 'Câmera', '<path d="M3.5 8.5A1.5 1.5 0 0 1 5 7h2.5L9 5h6l1.5 2H19a1.5 1.5 0 0 1 1.5 1.5v9A1.5 1.5 0 0 1 19 19H5a1.5 1.5 0 0 1-1.5-1.5z"/><circle cx="12" cy="13" r="3.5"/>'],
  ['galeria', 'Galeria', 'Galeria / Fotos', '<rect x="3.5" y="5" width="17" height="14" rx="1.5"/><path d="M3.5 16.5l5.5-6 4 4.5 2.5-2.5 5 4.5"/><circle cx="16" cy="9" r="1.4"/>'],
  ['internet', 'Internet', 'Samsung Internet / Navegador', '<circle cx="12" cy="12" r="8.5"/><ellipse cx="12" cy="12" rx="3.6" ry="8.5"/><path d="M3.5 12h17M5 7.5h14M5 16.5h14"/>'],
  ['navegador', 'Navegador', 'Chrome / Firefox', '<circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="3.2"/><path d="M12 8.8h8M9.2 13.6 5.2 6.7M14.8 13.6l-4 6.9"/>'],
  ['configuracoes', 'Configurações', 'Configurações', 'GEAR'],
  ['relogio', 'Relógio', 'Relógio', '<circle cx="12" cy="12" r="8.5"/><path d="M12 7v5l3.2 2"/>'],
  ['calendario', 'Calendário', 'Calendário', '<rect x="4" y="5.5" width="16" height="14.5" rx="1.5"/><path d="M4 10h16M8.5 3.5v4M15.5 3.5v4"/><path d="M14 14h2.5v2.5H14z"/>'],
  ['calculadora', 'Calculadora', 'Calculadora', '<rect x="5.5" y="3.5" width="13" height="17" rx="1.5"/><path d="M8.5 7h7v2.5h-7z"/><path d="M8.6 13h.01M12 13h.01M15.4 13h.01M8.6 16.5h.01M12 16.5h.01M15.4 16.5h.01" stroke-width="2"/>'],
  ['notas', 'Notas', 'Samsung Notes / Keep', '<path d="M6 3.5h8.5l3.5 3.5v13.5H6z"/><path d="M14.5 3.5V7H18"/><path d="M9 11h6M9 14h6M9 17h4"/>'],
  ['arquivos', 'Meus Arquivos', 'Meus Arquivos / Drive', '<path d="M3.5 7a1 1 0 0 1 1-1h4.8l2 2h8.2a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1h-15a1 1 0 0 1-1-1z"/><path d="M3.5 10.5h17"/>'],
  ['play_store', 'Play Store', 'Play Store', '<path d="M7.5 4.5v15l12-7.5z"/>'],
  ['galaxy_store', 'Galaxy Store', 'Galaxy Store / Compras', '<path d="M5 8.5h14l-1.2 11.5H6.2z"/><path d="M9 8.5V7a3 3 0 0 1 6 0v1.5"/>'],
  ['email', 'E-mail', 'Gmail / Outlook / E-mail', '<rect x="3.5" y="6" width="17" height="12" rx="1.5"/><path d="M4 7l8 6 8-6"/>'],
  ['video', 'Vídeo', 'YouTube / Netflix / Streaming', '<rect x="3" y="6" width="18" height="12" rx="3"/><path d="M10.5 9.5v5l4.3-2.5z"/>'],
  ['mapas', 'Mapas', 'Maps / Waze', '<path d="M12 21s-6.5-5.7-6.5-10.5a6.5 6.5 0 0 1 13 0C18.5 15.3 12 21 12 21z"/><circle cx="12" cy="10.5" r="2.3"/>'],
  ['whatsapp', 'WhatsApp', 'WhatsApp', '<path d="M4 20l1.2-3.7A8.5 8.5 0 1 1 8 19z"/><path transform="translate(7.6 7.6) scale(.38)" d="M6 4h2.6l1.4 3.8-1.9 1.4a10.5 10.5 0 0 0 6.7 6.7l1.4-1.9 3.8 1.4V18a2 2 0 0 1-2 2A15 15 0 0 1 4 6a2 2 0 0 1 2-2z" stroke-width="3.2"/>'],
  ['instagram', 'Instagram', 'Instagram', '<rect x="4" y="4" width="16" height="16" rx="4.5"/><circle cx="12" cy="12" r="3.8"/><path d="M16.6 7.4h.01" stroke-width="2"/>'],
  ['musica', 'Música', 'Spotify / Música', '<path d="M9 17.5V6l10-2v11.5"/><circle cx="6.8" cy="17.5" r="2.2"/><circle cx="16.8" cy="15.5" r="2.2"/>'],
  ['clima', 'Clima', 'Clima', '<path d="M7.5 18.5h9.5a3.5 3.5 0 0 0 .3-7 5 5 0 0 0-9.5-1.2A4.1 4.1 0 0 0 7.5 18.5z"/><path d="M16 4.5v1.2M20.3 6.3l-.9.9M22 10.5h-1.2"/>'],
  ['saude', 'Saúde', 'Samsung Health', '<path d="M12 20s-7.5-4.6-7.5-10.2A4.2 4.2 0 0 1 12 7.2a4.2 4.2 0 0 1 7.5 2.6C19.5 15.4 12 20 12 20z"/><path d="M7.5 12.5h2.6l1.2-2 1.8 3.6 1.2-1.6h2.2"/>'],
  ['gravador', 'Gravador', 'Gravador de voz', '<rect x="9" y="3.5" width="6" height="11" rx="3"/><path d="M5.8 11a6.2 6.2 0 0 0 12.4 0M12 17.2v3.3M9 20.5h6"/>'],
  ['carteira', 'Carteira', 'Samsung Wallet / Pix', '<rect x="3.5" y="6" width="17" height="13" rx="2"/><path d="M15 10h5.5v5H15a2.5 2.5 0 0 1 0-5z"/><path d="M15.4 12.5h.01" stroke-width="2"/><path d="M6 6l9-2.5 1 2.5"/>'],
  ['banco', 'Banco', 'Apps de banco', '<path d="M3.5 9 12 4.5 20.5 9z"/><path d="M5.5 9.5v8M9.8 9.5v8M14.2 9.5v8M18.5 9.5v8M3.5 20h17"/>'],
  ['nuvem', 'Nuvem', 'Drive / OneDrive / Fotos', '<path d="M7 18.5h10a4 4 0 0 0 .5-8 5.5 5.5 0 0 0-10.6-1A4.5 4.5 0 0 0 7 18.5z"/><path d="M12 16v-5M9.8 13l2.2-2.2 2.2 2.2"/>'],
  ['videochamada', 'Videochamada', 'Meet / Zoom / Teams', '<rect x="3" y="7" width="12.5" height="10" rx="1.5"/><path d="M15.5 10.5 21 7.5v9l-5.5-3z"/>'],
  ['carro', 'Transporte', 'Uber / 99', '<path d="M4 16.5V12l2-5h12l2 5v4.5z"/><path d="M4 12h16"/><circle cx="7.5" cy="16.5" r="1.8"/><circle cx="16.5" cy="16.5" r="1.8"/>'],
  ['comida', 'Comida', 'iFood / Delivery', '<path d="M6.5 3.5v6a2 2 0 0 0 4 0v-6M8.5 9.5v11"/><path d="M17.5 20.5v-17c-2.2 1.6-3.3 4.4-3.3 7.8h3.3"/>'],
  ['livros', 'Leitura', 'Kindle / Livros', '<path d="M12 6.5C10 5 7 4.5 3.5 5v13.5c3.5-.5 6.5 0 8.5 1.5 2-1.5 5-2 8.5-1.5V5C17 4.5 14 5 12 6.5z"/><path d="M12 6.5V20"/>'],
  ['telegram', 'Telegram', 'Telegram', '<path d="M20.5 4 3.5 11l5.5 2 2 6 3-4 4.8 3.5z"/><path d="M9 13l11.5-9"/>'],
  ['casa', 'Casa', 'SmartThings', '<path d="M4 11 12 4l8 7v9H4z"/><path d="M10 20v-5.5h4V20"/>'],
  ['relogio_pulso', 'Wearable', 'Galaxy Wearable', '<rect x="7.5" y="7" width="9" height="10" rx="2.5"/><path d="M9 7l.8-3.5h4.4L15 7M9 17l.8 3.5h4.4L15 17M12 10v2.2l1.4 1"/>'],
  ['lembretes', 'Lembretes', 'Lembretes / Tarefas', '<circle cx="12" cy="12" r="8.5"/><path d="M8.3 12.3l2.6 2.6 5-5.5"/>'],
  ['assistente', 'Assistente', 'Assistentes / IA', '<path d="M12 3.5c.6 4.4 4.1 7.9 8.5 8.5-4.4.6-7.9 4.1-8.5 8.5-.6-4.4-4.1-7.9-8.5-8.5 4.4-.6 7.9-4.1 8.5-8.5z"/>'],
  ['ferramentas', 'Ferramentas', 'Utilitários', '<path d="M14.5 4.5a4 4 0 0 0-5 5l-5 5a1.4 1.4 0 0 0 2 2l5-5a4 4 0 0 0 5-5l-2.3 2.3-2-.5-.5-2z"/>'],
  ['pinheiro', 'Pinheiro', 'Pastas / ícone extra', '<path d="M12 3.5 16.5 10h-2.8l4 5.5H13.2v5h-2.4v-5H6.3l4-5.5H7.5z"/>'],
  ['montanha', 'Serra', 'Pastas / ícone extra', '<path d="M2.5 19 9 8.5l3.2 5 2.3-3.5 7 9z"/><path d="M7.4 11.1l1.6 1.4 1.4-1.6"/><circle cx="17.5" cy="6.5" r="1.6"/>'],
  ['folha', 'Folha', 'Pastas / ícone extra', '<path d="M5 19C5 10 10 5 19.5 4.5 19 14 14 19 5 19z"/><path d="M5 19l9-9"/>'],
];

function gear() { // engrenagem procedural (8 dentes)
  const pts = []; const n = 8;
  for (let i = 0; i < n * 4; i++) {
    const a = (i / (n * 4)) * Math.PI * 2 - Math.PI / 2;
    const r = [8.3, 8.3, 6.6, 6.6][i % 4];
    const off = [-0.16, 0.16, 0.22, -0.22][i % 4] * (Math.PI * 2 / n) / 2.2;
    pts.push([12 + r * Math.cos(a + off), 12 + r * Math.sin(a + off)]);
  }
  return `<path d="M${pts.map(p => p.map(v => v.toFixed(2)).join(' ')).join('L')}z"/><circle cx="12" cy="12" r="2.8"/>`;
}

function squircle(s, inset) { // superelipse n=4.6 (aprox. forma One UI)
  const n = 4.6, r = s / 2 - inset, c = s / 2, pts = [];
  for (let i = 0; i < 180; i++) {
    const t = (i / 180) * Math.PI * 2, ct = Math.cos(t), st = Math.sin(t);
    pts.push([c + r * Math.sign(ct) * Math.abs(ct) ** (2 / n), c + r * Math.sign(st) * Math.abs(st) ** (2 / n)]);
  }
  return 'M' + pts.map(p => p.map(v => v.toFixed(2)).join(' ')).join('L') + 'Z';
}

const VAR = {
  papel:  { tile: C.paper, glyph: C.ink,   ring: C.gold },
  carvao: { tile: C.ink,   glyph: C.paper, ring: C.gold },
  musgo:  { tile: C.moss,  glyph: C.paper, ring: C.gold },
};

function iconSVG(inner, v, size = 512) {
  const body = inner === 'GEAR' ? gear() : inner;
  const g = 0.5 * size / 24; // glifo ocupa 50% do azulejo
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <path d="${squircle(size, 4)}" fill="${v.tile}"/>
  <path d="${squircle(size, size * 0.075)}" fill="none" stroke="${v.ring}" stroke-width="${(size / 512 * 3).toFixed(2)}" opacity=".9"/>
  <g transform="translate(${size * 0.25} ${size * 0.25}) scale(${g})" fill="none" stroke="${v.glyph}" stroke-width="1.15" stroke-linecap="round" stroke-linejoin="round">${body}</g>
</svg>`;
}

(async () => {
  const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 512, height: 512 } });
  const manifest = [];
  for (const [vn, v] of Object.entries(VAR)) {
    const dir = path.join(OUT, vn); fs.mkdirSync(dir, { recursive: true });
    for (const [f, nome, app, inner] of G) {
      const svg = iconSVG(inner, v);
      fs.mkdirSync(path.join(OUT, '_svg', vn), { recursive: true });
      fs.writeFileSync(path.join(OUT, '_svg', vn, f + '.svg'), svg);
      await p.setContent(`<html><body style="margin:0;background:transparent">${svg}</body></html>`);
      await p.locator('svg').screenshot({ path: path.join(dir, f + '.png'), omitBackground: true });
      if (vn === 'papel') manifest.push({ arquivo: f + '.png', nome, app });
    }
  }
  fs.writeFileSync(path.join(OUT, 'lista_icones.json'), JSON.stringify(manifest, null, 2));
  await b.close(); console.log('ok', G.length, 'glifos x', Object.keys(VAR).length, 'variantes');
})();
