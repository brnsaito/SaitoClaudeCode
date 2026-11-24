// Prévia: tela inicial, bloqueio, AOD e paleta. Uso: node mockup.js <raiz_tema>
const { chromium } = require('playwright'); const path = require('path'); const fs = require('fs');
const R = path.resolve(process.argv[2]);
const u = f => 'file://' + path.join(R, f);
const ic = ['telefone','mensagens','camera','galeria','internet','email','mapas','calendario','relogio','notas','musica','whatsapp','instagram','banco','saude','configuracoes'];
const dock = ['telefone','mensagens','navegador','camera'];
const grid = ic.filter(x => !dock.includes(x)).slice(0, 12);
const html = `<!doctype html><html><head><meta charset="utf-8">
<link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@300;400&family=Inter:wght@300;400&display=swap" rel="stylesheet">
<style>
*{box-sizing:border-box;margin:0}
body{background:#d9c79c;font-family:Inter,sans-serif;padding:56px 56px 48px;width:1520px;color:#3D2B1F}
h1{font-family:'Cormorant Garamond',serif;font-weight:300;font-size:46px;letter-spacing:.02em}
.sub{font-size:13px;letter-spacing:.28em;text-transform:uppercase;margin:6px 0 36px;color:#5B6B45}
.row{display:flex;gap:40px;align-items:flex-start}
.ph{width:330px;height:733px;border-radius:44px;overflow:hidden;position:relative;box-shadow:0 0 0 9px #3D2B1F,0 30px 60px rgba(61,43,31,.35);background-size:cover;background-position:center}
.cap{margin-top:26px;font-size:12px;letter-spacing:.22em;text-transform:uppercase;text-align:center}
.sb{position:absolute;top:12px;left:26px;right:26px;display:flex;justify-content:space-between;font-size:11px}
.w{position:absolute;top:70px;left:28px}
.w .t{font-family:'Cormorant Garamond',serif;font-weight:300;font-size:58px;line-height:1}
.w .d{font-size:10px;letter-spacing:.24em;text-transform:uppercase;margin-top:6px;display:flex;align-items:center;gap:8px}
.dot{width:5px;height:5px;border-radius:50%;background:#A5502E;display:inline-block}
.g{position:absolute;left:22px;right:22px;top:370px;display:grid;grid-template-columns:repeat(4,1fr);row-gap:22px;justify-items:center}
.g img,.dk img{width:56px;height:56px}
.dk{position:absolute;left:22px;right:22px;bottom:34px;display:grid;grid-template-columns:repeat(4,1fr);justify-items:center}
.pg{position:absolute;bottom:112px;left:0;right:0;display:flex;justify-content:center;gap:7px}
.pg i{width:5px;height:5px;border-radius:50%;background:#E4D2A8;opacity:.5}.pg i:first-child{opacity:1}
.lk{position:absolute;top:120px;left:0;right:0;text-align:center}
.lk .t{font-family:'Cormorant Garamond',serif;font-weight:300;font-size:104px;line-height:.9}
.lk .d{font-size:11px;letter-spacing:.3em;text-transform:uppercase;margin-top:14px;display:flex;justify-content:center;gap:10px;align-items:center}
.nt{position:absolute;top:330px;left:22px;right:22px;background:rgba(228,210,168,.9);border:1px solid #B8935A;border-radius:18px;padding:12px 14px;font-size:11px;display:flex;gap:10px;align-items:center}
.nt img{width:30px;height:30px}.nt b{font-weight:400;display:block;font-size:12px}
.ls{position:absolute;bottom:30px;left:34px;right:34px;display:flex;justify-content:space-between}
.ls span{width:44px;height:44px;border-radius:50%;border:1px solid #B8935A;display:flex;align-items:center;justify-content:center;background:rgba(61,43,31,.55)}
.aod{background:#000}
.aod .t{position:absolute;top:150px;left:0;right:0;text-align:center;color:#B8935A;font-family:'Cormorant Garamond',serif;font-weight:300;font-size:88px;line-height:.9}
.aod .d{position:absolute;top:255px;left:0;right:0;text-align:center;color:#B8935A;font-size:10px;letter-spacing:.3em;opacity:.8}
.aod img{position:absolute;left:0;top:360px;width:330px}
.pal{display:flex;margin-top:46px;height:64px;border-radius:2px;overflow:hidden;border:1px solid #B8935A}
.pal div{display:flex;white-space:nowrap;align-items:flex-end;padding:8px 10px;font-size:10px;letter-spacing:.12em}
.qp{width:330px}
.qpc{background:#3D2B1F;border-radius:28px;padding:20px;color:#E4D2A8;box-shadow:0 30px 60px rgba(61,43,31,.35)}
.qpc .tl{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-top:14px}
.qpc .tl span{height:58px;border-radius:18px;border:1px solid #B8935A;display:flex;align-items:center;justify-content:center;font-size:9px;letter-spacing:.1em}
.qpc .tl span.on{background:#5B6B45;border-color:#5B6B45}
.bar{height:34px;border-radius:17px;border:1px solid #B8935A;margin-top:14px;position:relative;overflow:hidden}
.bar i{position:absolute;inset:0 45% 0 0;background:#5B6B45}
.kb{margin-top:22px;background:#E4D2A8;border-radius:22px;padding:12px 10px 14px;border-top:1px solid #B8935A;box-shadow:0 30px 60px rgba(61,43,31,.25)}
.kb .r{display:flex;gap:5px;margin-top:6px}.kb .r span{flex:1;height:34px;border-radius:7px;background:transparent;display:flex;align-items:center;justify-content:center;font-size:13px;color:#3D2B1F}
.kb .r span.f{background:#5B6B45;color:#E4D2A8;font-size:10px}.kb .r span.e{background:#3D2B1F;color:#E4D2A8;font-size:10px}
</style></head><body>
<h1>Serra &amp; Bruma</h1><div class="sub">Tema para Samsung Galaxy · paleta pessoal terrosa</div>
<div class="row">
 <div><div class="ph" style="background-image:url(${u('papeis_de_parede/01_tela_inicial.png')})">
   <div class="sb"><span>14:32</span><span>5G ▮▮▮ 86%</span></div>
   <div class="w"><div class="t">14:32</div><div class="d"><span class="dot"></span>terça · 24 nov · 21°</div></div>
   <div class="g">${grid.map(n=>`<img src="${u('icones/papel/'+n+'.png')}">`).join('')}</div>
   <div class="pg"><i></i><i></i><i></i></div>
   <div class="dk">${dock.map(n=>`<img src="${u('icones/papel/'+n+'.png')}">`).join('')}</div>
 </div><div class="cap">Tela inicial</div></div>
 <div><div class="ph" style="background-image:url(${u('papeis_de_parede/02_tela_bloqueio.png')})">
   <div class="sb"><span>Vivo</span><span>86%</span></div>
   <div class="lk"><div class="t">14:32</div><div class="d"><span class="dot"></span>terça, 24 de novembro</div></div>
   <div class="nt"><img src="${u('icones/carvao/calendario.png')}"><div><b>Reunião às 16h</b>Sala 3 · lembrete em 30 min</div></div>
   <div class="ls"><span><img src="${u('icones/carvao/telefone.png')}" style="width:44px"></span><span><img src="${u('icones/carvao/camera.png')}" style="width:44px"></span></div>
 </div><div class="cap">Tela de bloqueio</div></div>
 <div><div class="ph aod"><div class="t">14:32</div><div class="d">TER · 24 NOV · 86%</div><img src="${u('papeis_de_parede/04_aod_always_on_display.png')}"></div><div class="cap">Always On Display</div></div>
 <div class="qp"><div class="qpc">
   <div style="display:flex;justify-content:space-between;font-size:11px;letter-spacing:.2em"><span>PAINEL RÁPIDO</span><span>14:32</span></div>
   <div class="tl"><span class="on">Wi-Fi</span><span class="on">Som</span><span>Bluetooth</span><span>Lanterna</span><span>Rotação</span><span class="on">Avião</span><span>Dados</span><span>Noite</span></div>
   <div class="bar"><i></i></div>
 </div>
 <div class="kb">
   <div class="r">${'qwertyuiop'.split('').map(c=>`<span>${c}</span>`).join('')}</div>
   <div class="r" style="padding:0 14px">${'asdfghjkl'.split('').map(c=>`<span>${c}</span>`).join('')}</div>
   <div class="r"><span class="f">⇧</span>${'zxcvbnm'.split('').map(c=>`<span>${c}</span>`).join('')}<span class="f">⌫</span></div>
   <div class="r"><span class="f">123</span><span style="flex:5">espaço</span><span class="e">OK</span></div>
 </div><div class="cap">Painel rápido · Teclado</div></div>
</div>
<div class="pal">
 <div style="flex:50;background:#E4D2A8">E4D2A8 · 50% fundo</div>
 <div style="flex:20;background:#3D2B1F;color:#E4D2A8">3D2B1F · 20%</div>
 <div style="flex:18;background:#5B6B45;color:#E4D2A8">5B6B45 · 18%</div>
 <div style="flex:7;background:#E4D2A8;border-left:2px solid #B8935A;border-right:2px solid #B8935A;color:#3D2B1F">B8935A · 7%</div>
 <div style="flex:5;background:#A5502E;color:#E4D2A8;font-size:9px">A5502E · 5%</div>
</div>
</body></html>`;
(async()=>{const b=await chromium.launch({args:['--allow-file-access-from-files']});
const p=await b.newPage({viewport:{width:1520,height:1100},deviceScaleFactor:2});
const f=path.join(R,'fonte','_mockup.html');fs.writeFileSync(f,html);
await p.goto('file://'+f,{waitUntil:'networkidle'}).catch(()=>{});
await p.waitForTimeout(800);
const fam = await p.evaluate(()=>document.fonts.check("300 40px 'Cormorant Garamond'"));
await p.screenshot({path:path.join(R,'00_PREVIA_DO_TEMA.png'),fullPage:true});
console.log('cormorant carregada:',fam);await b.close();})();
