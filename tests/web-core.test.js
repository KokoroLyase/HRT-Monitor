/* 激素监测（Web）—— Node 单元测试：单位换算 / 日期计算 / 状态判定 / 序列化 */
const assert = require('assert');
global.HRT = {};
require('../web/js/i18n.js');
const HRT = require('../web/js/core.js');
HRT.lang = 'zh-CN';

function close(actual, expected, tol) {
  assert.ok(Math.abs(actual - expected) <= (tol || 0.02), actual + ' != ' + expected);
}

// —— 单位换算（与安卓版一致）——
close(HRT.toBase('e2', 100, 'pg/mL'), 367.1);
close(HRT.fromBase('e2', 367.1, 'pg/mL'), 100, 0.05);
close(HRT.toBase('t', 916, 'ng/dL'), 9.16);
close(HRT.toBase('t', 1, 'nmol/L'), 0.28846);
close(HRT.toBase('prl', 500, 'mIU/L'), 23.585);
close(HRT.toBase('p4', 1, 'ng/mL'), 3.18);
assert.strictEqual(HRT.baseUnit('e2'), 'pmol/L');
assert.strictEqual(HRT.baseUnit('t'), 'ng/mL');

// —— 格式化 ——
assert.strictEqual(HRT.fmt(367.1), '367.1');
assert.strictEqual(HRT.fmt(0.55), '0.55');
assert.strictEqual(HRT.fmt(1284.4), '1284');
assert.strictEqual(HRT.fmt(0.001), '0.001');
assert.strictEqual(HRT.fmt(23.3), '23.3');

// —— 日期 ——
const d1 = HRT.parseLocalDate('2024-01-15');
const d2 = HRT.parseLocalDate('2026-04-10');
assert.ok(d1 > 0 && d2 > 0);
assert.strictEqual(HRT.fmtDate(d1), '2024-01-15');
const parts = HRT.durationParts(d1, d2);
assert.deepStrictEqual(parts, { y: 2, m: 2, d: 26 });

const bday = HRT.parseLocalDate('2000-06-15');
assert.strictEqual(HRT.ageAt(bday, HRT.parseLocalDate('2026-06-14')), 25);
assert.strictEqual(HRT.ageAt(bday, HRT.parseLocalDate('2026-06-15')), 26);
assert.strictEqual(HRT.ageAt(bday, HRT.parseLocalDate('2000-06-14')), -1);

// —— 状态判定 ——
const gaht = HRT.BUILTIN_RANGES.filter(function (r) { return r.id === 'builtin-e2-gaht'; })[0];
assert.strictEqual(HRT.statusOf(500, gaht), 'IN');
assert.strictEqual(HRT.statusOf(800, gaht), 'HIGH');
assert.strictEqual(HRT.statusOf(200, gaht), 'LOW');
// 200 pg/mL × 3.671 = 734.2（上限边界，与安卓版算法一致）
assert.strictEqual(HRT.statusOf(734.3, gaht), 'HIGH');
const prlHigh = HRT.BUILTIN_RANGES.filter(function (r) { return r.id === 'builtin-prl-high'; })[0];
assert.strictEqual(HRT.statusOf(80, prlHigh), 'HIGH');
assert.strictEqual(HRT.statusOf(20, prlHigh), 'IN');

// —— 元信息 ——
const prefs = { birthday: bday, hrtStart: HRT.parseLocalDate('2024-01-15') };
const meta = HRT.metaLine(HRT.parseLocalDate('2026-04-10'), prefs);
assert.ok(meta.indexOf('年龄 25 岁') === 0, meta);
assert.ok(meta.indexOf('HRT 已进行 2年2个月26天') > 0, meta);
assert.strictEqual(HRT.metaLine(d2, { birthday: 0, hrtStart: 0 }), '');

// —— 序列化往返（与安卓备份格式互通）——
const data = HRT.defaultData();
HRT.upsertRecord(data, {
  id: 'r1', date: d2, note: '备注"引号"',
  items: [{ h: 'e2', v: 100, u: 'pg/mL' }, { h: 't', v: 0.4, u: 'ng/mL' }]
});
HRT.upsertEvent(data, { id: 'e1', date: d1, type: 'start_hrt', note: '开始' });
HRT.upsertRange(data, { id: 'rg1', h: 'e2', label: '我的目标', min: 90, max: 210, unit: 'pg/mL', builtin: false, visible: true });
HRT.setTarget(data, 'e2', 'rg1');
const text = HRT.serialize(data);
const parsed = HRT.parse(text);
assert.strictEqual(parsed.records.length, 1);
assert.strictEqual(parsed.records[0].items.length, 2);
assert.strictEqual(parsed.records[0].note, '备注"引号"');
assert.strictEqual(parsed.events.length, 1);
assert.strictEqual(parsed.ranges.length, 1);
assert.strictEqual(parsed.ranges[0].label, '我的目标');
assert.strictEqual(parsed.targets.e2, 'rg1');
const merged = HRT.mergeBuiltins(parsed);
assert.ok(merged.ranges.length >= 21, '内置范围数量不足: ' + merged.ranges.length);

// —— 导入合并 ——
const existing = HRT.defaultData();
HRT.upsertRecord(existing, { id: 'x', date: d1, note: 'old', items: [] });
const imported = HRT.parse(text); // 含 r1
const result = HRT.importData(existing, imported, false);
assert.strictEqual(result.records.length, 2);
const x = result.records.filter(function (r) { return r.id === 'x'; })[0];
assert.strictEqual(x.note, 'old');
const replaced = HRT.importData(existing, imported, true);
assert.strictEqual(replaced.records.length, 1);

// —— CSV ——
const csv = HRT.buildRecordsCsv(data);
assert.ok(csv.indexOf('日期,备注,E2值,E2单位,') === 0, csv.slice(0, 40));
assert.ok(csv.indexOf('"备注""引号"""') > 0);

console.log('ALL TESTS PASSED (' + 1 + ' file)');
