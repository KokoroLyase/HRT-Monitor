/* 激素监测（Web）—— 绑定覆盖回归检查：
 * 校验所有渲染模板中的可交互 id 与类选择器在 JS 中都有对应的事件绑定，
 * 防止「元素渲染了但点击无反应」类缺陷。 */
const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');
const ui = fs.readFileSync(path.join(root, 'web/js/ui.js'), 'utf8');
const app = fs.readFileSync(path.join(root, 'web/js/app.js'), 'utf8');
const html = fs.readFileSync(path.join(root, 'web/index.html'), 'utf8');
const js = ui + '\n' + app;

// 模板中出现的所有 id="X"
const ids = new Set();
const idRe = /id="([A-Za-z0-9_-]+)"/g;
let m;
while ((m = idRe.exec(ui + html))) ids.add(m[1]);

// JS 中的 $('#x') / querySelector('#x') 引用
const refRe = /['"]#([A-Za-z0-9_-]+)/g;
const refs = new Set();
while ((m = refRe.exec(js))) refs.add(m[1]);

// 动态拼接 id（如 $('#rec-v-' + h.key)）
const dynPairs = [
  ['rec-v-', "$('#rec-v-'"],
  ['rec-u-', "$('#rec-u-'"],
  ['rec-hint-', "$('#rec-hint-'"],
  ['nav-', "$('#nav-'"]
];

// 纯容器 / 展示性 id（无交互，不需要绑定）
const containerIds = { 'range-list': true };

const missing = [];
for (const id of ids) {
  if (containerIds[id]) continue;
  let used = refs.has(id);
  if (!used) {
    for (const p of dynPairs) {
      if (id.startsWith(p[0]) && js.includes(p[1])) { used = true; break; }
    }
  }
  if (!used) missing.push('id:' + id);
}

// 类/属性选择器绑定
const classChecks = [
  ['.rec-row', 'rec-row'], ['.ev-row', 'ev-row'], ['.range-row', 'range-row'],
  ['.nav-btn', 'nav-btn'], ['[data-copy]', 'data-copy'], ['.chip-btn', 'chip-btn'],
  ['.r-edit', 'r-edit'], ['.r-del', 'r-del'], ['input[name=theme]', 'name=theme'],
  ['input[name=lang]', 'name=lang'], ['input[data-vis]', 'data-vis'], ['[data-close]', 'data-close']
];
for (const c of classChecks) {
  if (ui.includes(c[1]) && !js.includes(c[0])) missing.push('selector:' + c[0]);
}

if (missing.length) {
  console.error('FAIL: 以下元素缺少事件绑定——');
  missing.forEach(function (x) { console.error('  -', x); });
  process.exit(1);
}
console.log('PASS: binding coverage ok (' + ids.size + ' ids + ' + classChecks.length + ' selectors)');
