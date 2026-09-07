/* 激素监测（Web）—— 核心数据逻辑：模型 / 单位 / 内置范围 / 存储 / 日期计算
 * 与安卓版保持数值与备份格式（JSON schema）完全一致，可互相导入导出。 */
(function (global) {
  'use strict';

  const HRT = global.HRT = global.HRT || {};

  /* ---------- 指标与单位（与 Android Units.kt 一致） ---------- */
  HRT.HORMONES = [
    { key: 'e2', short: 'E2' },
    { key: 't', short: 'T' },
    { key: 'lh', short: 'LH' },
    { key: 'fsh', short: 'FSH' },
    { key: 'prl', short: 'PRL' },
    { key: 'p4', short: 'P4' }
  ];

  HRT.UNITS = {
    e2: [{ label: 'pg/mL', f: 3.671 }, { label: 'pmol/L', f: 1.0 }],
    t: [{ label: 'ng/mL', f: 1.0 }, { label: 'ng/dL', f: 0.01 }, { label: 'nmol/L', f: 0.28846 }],
    lh: [{ label: 'IU/L', f: 1.0 }, { label: 'mIU/mL', f: 1.0 }],
    fsh: [{ label: 'IU/L', f: 1.0 }, { label: 'mIU/mL', f: 1.0 }],
    prl: [{ label: 'ng/mL', f: 1.0 }, { label: 'µg/L', f: 1.0 }, { label: 'mIU/L', f: 0.04717 }],
    p4: [{ label: 'nmol/L', f: 1.0 }, { label: 'ng/mL', f: 3.18 }]
  };

  HRT.EVENT_TYPES = ['start_hrt', 'med_change', 'doctor_change', 'surgery', 'other'];

  HRT.hormone = function (key) {
    for (const h of HRT.HORMONES) if (h.key === key) return h;
    return null;
  };
  HRT.hormoneLabelKey = function (key) { return 'hormone_' + key; };
  HRT.baseUnit = function (key) {
    for (const u of HRT.UNITS[key]) if (u.f === 1.0) return u.label;
    return HRT.UNITS[key][0].label;
  };
  HRT.defaultUnit = function (key) { return HRT.UNITS[key][0].label; };
  HRT.factor = function (key, unit) {
    for (const u of HRT.UNITS[key]) if (u.label === unit) return u.f;
    return 1.0;
  };
  HRT.toBase = function (key, v, unit) { return v * HRT.factor(key, unit); };
  HRT.fromBase = function (key, base, unit) { return base / HRT.factor(key, unit); };

  HRT.fmt = function (v) {
    if (typeof v !== 'number' || !isFinite(v)) return '-';
    const a = Math.abs(v);
    if (a >= 1000) return String(Math.round(v));
    if (a >= 0.1) return String(parseFloat(v.toFixed(3)));
    return String(parseFloat(v.toFixed(3)));
  };

  /* ---------- 内置参考范围（与 Android SeedData.kt 一致） ---------- */
  HRT.BUILTIN_RANGES = [
    { id: 'builtin-e2-male', h: 'e2', labelKey: 'range_e2_male', min: 8, max: 35, unit: 'pg/mL', builtin: true, visible: false },
    { id: 'builtin-e2-foll', h: 'e2', labelKey: 'range_e2_foll', min: 30, max: 100, unit: 'pg/mL', builtin: true, visible: true },
    { id: 'builtin-e2-lut', h: 'e2', labelKey: 'range_e2_lut', min: 70, max: 300, unit: 'pg/mL', builtin: true, visible: true },
    { id: 'builtin-e2-gaht', h: 'e2', labelKey: 'range_e2_gaht', min: 100, max: 200, unit: 'pg/mL', builtin: true, visible: true },
    { id: 'builtin-t-male', h: 't', labelKey: 'range_t_male', min: 2.64, max: 9.16, unit: 'ng/mL', builtin: true, visible: false },
    { id: 'builtin-t-female', h: 't', labelKey: 'range_t_female', min: 0.1, max: 0.55, unit: 'ng/mL', builtin: true, visible: true },
    { id: 'builtin-t-gaht', h: 't', labelKey: 'range_t_gaht', min: 0, max: 0.55, unit: 'ng/mL', builtin: true, visible: true },
    { id: 'builtin-prl-female', h: 'prl', labelKey: 'range_prl_female', min: 4.79, max: 23.3, unit: 'ng/mL', builtin: true, visible: true },
    { id: 'builtin-prl-high', h: 'prl', labelKey: 'range_prl_high', min: 69.9, max: 69.9, unit: 'ng/mL', builtin: true, visible: true, thresholdOnly: true },
    { id: 'builtin-lh-foll', h: 'lh', labelKey: 'range_lh_foll', min: 2.12, max: 10.89, unit: 'IU/L', builtin: true, visible: true },
    { id: 'builtin-lh-ovu', h: 'lh', labelKey: 'range_lh_ovu', min: 19.18, max: 103.03, unit: 'IU/L', builtin: true, visible: false },
    { id: 'builtin-lh-lut', h: 'lh', labelKey: 'range_lh_lut', min: 1.2, max: 12.86, unit: 'IU/L', builtin: true, visible: true },
    { id: 'builtin-lh-gaht', h: 'lh', labelKey: 'range_lh_gaht', min: 0, max: 2.0, unit: 'IU/L', builtin: true, visible: true },
    { id: 'builtin-fsh-foll', h: 'fsh', labelKey: 'range_fsh_foll', min: 3.85, max: 8.78, unit: 'IU/L', builtin: true, visible: true },
    { id: 'builtin-fsh-ovu', h: 'fsh', labelKey: 'range_fsh_ovu', min: 4.54, max: 22.51, unit: 'IU/L', builtin: true, visible: false },
    { id: 'builtin-fsh-lut', h: 'fsh', labelKey: 'range_fsh_lut', min: 1.79, max: 5.12, unit: 'IU/L', builtin: true, visible: true },
    { id: 'builtin-fsh-gaht', h: 'fsh', labelKey: 'range_fsh_gaht', min: 0, max: 2.0, unit: 'IU/L', builtin: true, visible: true },
    { id: 'builtin-p4-foll', h: 'p4', labelKey: 'range_p4_foll', min: 0.99, max: 4.83, unit: 'nmol/L', builtin: true, visible: true },
    { id: 'builtin-p4-lut', h: 'p4', labelKey: 'range_p4_lut', min: 16.4, max: 59.0, unit: 'nmol/L', builtin: true, visible: false },
    { id: 'builtin-p4-gaht', h: 'p4', labelKey: 'range_p4_gaht', min: 0, max: 3.18, unit: 'nmol/L', builtin: true, visible: true }
  ];

  HRT.DEFAULT_TARGETS = {
    e2: 'builtin-e2-gaht',
    t: 'builtin-t-gaht',
    prl: 'builtin-prl-female',
    lh: 'builtin-lh-gaht',
    fsh: 'builtin-fsh-gaht',
    p4: 'builtin-p4-gaht'
  };

  HRT.rangeLabel = function (r) {
    if (r.label) return r.label;
    if (r.labelKey) return HRT.t(r.labelKey);
    return '';
  };
  HRT.rangeMinBase = function (r) { return HRT.toBase(r.h, r.min, r.unit); };
  HRT.rangeMaxBase = function (r) { return HRT.toBase(r.h, r.max, r.unit); };
  HRT.rangeSpanText = function (r) {
    if (r.thresholdOnly) return '≥ ' + HRT.fmt(r.min) + ' ' + r.unit;
    return HRT.fmt(r.min) + ' – ' + HRT.fmt(r.max) + ' ' + r.unit;
  };

  /* ---------- 状态判定（与 Android 一致） ---------- */
  HRT.statusOf = function (baseValue, range) {
    if (baseValue === null || baseValue === undefined || !range) return 'UNKNOWN';
    const lo = HRT.rangeMinBase(range);
    const hi = HRT.rangeMaxBase(range);
    if (range.thresholdOnly) return baseValue >= lo ? 'HIGH' : 'IN';
    if (baseValue < lo) return 'LOW';
    if (baseValue > hi) return 'HIGH';
    return 'IN';
  };

  /* ---------- 日期工具（本地时区，与 Android 逻辑一致） ---------- */
  HRT.todayMillis = function () {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  };
  HRT.fmtDate = function (ms) {
    const d = new Date(ms);
    const p = function (n) { return (n < 10 ? '0' : '') + n; };
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
  };
  HRT.fmtMonthDay = function (ms) {
    const d = new Date(ms);
    const p = function (n) { return (n < 10 ? '0' : '') + n; };
    return p(d.getMonth() + 1) + '-' + p(d.getDate());
  };
  HRT.fmtYearMonth = function (ms) {
    const d = new Date(ms);
    const p = function (n) { return (n < 10 ? '0' : '') + n; };
    return String(d.getFullYear()).slice(2) + '-' + p(d.getMonth() + 1);
  };
  HRT.relativeDay = function (ms) {
    const todayStart = HRT.todayMillis();
    const day = 24 * 3600 * 1000;
    if (ms >= todayStart) return HRT.t('today');
    if (ms >= todayStart - day) return HRT.t('yesterday');
    return HRT.t('days_ago', Math.floor((todayStart - ms) / day) + 1);
  };
  /* 本地日期字符串 yyyy-mm-dd → 本地午夜毫秒 */
  HRT.parseLocalDate = function (s) {
    const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(s || '');
    if (!m) return 0;
    const d = new Date(parseInt(m[1], 10), parseInt(m[2], 10) - 1, parseInt(m[3], 10));
    return isNaN(d.getTime()) ? 0 : d.getTime();
  };

  HRT.durationParts = function (startMs, endMs) {
    const s = new Date(startMs);
    const e = new Date(endMs);
    let y = e.getFullYear() - s.getFullYear();
    let m = e.getMonth() - s.getMonth();
    let d = e.getDate() - s.getDate();
    if (d < 0) {
      m--;
      d += new Date(e.getFullYear(), e.getMonth(), 0).getDate();
    }
    if (m < 0) {
      y--;
      m += 12;
    }
    if (y < 0) { y = 0; m = 0; d = 0; }
    return { y: y, m: m, d: d };
  };

  HRT.ageAt = function (birthMs, atMs) {
    if (!birthMs || atMs < birthMs) return -1;
    const b = new Date(birthMs);
    const a = new Date(atMs);
    let age = a.getFullYear() - b.getFullYear();
    if (a.getMonth() < b.getMonth() || (a.getMonth() === b.getMonth() && a.getDate() < b.getDate())) age--;
    return age;
  };

  HRT.durationString = function (parts) {
    let s = '';
    if (parts.y > 0) s += HRT.t('dur_years', parts.y);
    if (parts.m > 0) s += HRT.t('dur_months', parts.m);
    if (parts.d > 0) s += HRT.t('dur_days', parts.d);
    if (!s) s = HRT.t('dur_days', 0);
    return s;
  };

  /* 记录元信息：血检时年龄 · HRT 已进行时长 */
  HRT.metaLine = function (dateMs, prefs) {
    const parts = [];
    if (prefs.birthday) {
      const age = HRT.ageAt(prefs.birthday, dateMs);
      if (age >= 0) parts.push(HRT.t('age_at', age));
    }
    if (prefs.hrtStart && dateMs >= prefs.hrtStart) {
      parts.push(HRT.t('hrt_for', HRT.durationString(HRT.durationParts(prefs.hrtStart, dateMs))));
    }
    return parts.length ? parts.join(' · ') : '';
  };

  HRT.uid = function () {
    if (global.crypto && crypto.randomUUID) return crypto.randomUUID();
    return 'id-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
  };

  /* ---------- 本地存储（localStorage，私有数据不离开设备） ---------- */
  HRT.storage = {
    get: function (key) {
      try { return global.localStorage.getItem(key); } catch (e) { return null; }
    },
    set: function (key, val) {
      try { global.localStorage.setItem(key, val); } catch (e) { /* 忽略（如隐私模式） */ }
    }
  };

  HRT.DATA_KEY = 'hrt_data_json_v1';
  HRT.PREFS_KEY = 'hrt_prefs_v1';

  HRT.defaultPrefs = function () {
    return {
      theme: 'system',
      lang: 'system',
      birthday: 0,
      hrtStart: 0,
      agreed: false,
      profileAsked: false,
      aiBaseUrl: 'https://api.deepseek.com/v1',
      aiKey: '',
      aiModel: 'deepseek-chat'
    };
  };

  HRT.defaultData = function () {
    return { records: [], events: [], ranges: [], targets: Object.assign({}, HRT.DEFAULT_TARGETS) };
  };

  /* 数据文件 schema 与安卓版 JsonIO 完全一致：
   * { version, records:[{id,date,note,items:[{h,v,u}]}],
   *   events:[{id,date,type,note}],
   *   ranges:[自定义范围 {id,h,label,min,max,unit,visible}],
   *   targets:{h:id} } */
  HRT.serialize = function (data) {
    return JSON.stringify({
      version: 1,
      records: data.records,
      events: data.events,
      ranges: data.ranges.filter(function (r) { return !r.builtin; }),
      targets: data.targets
    }, null, 2);
  };

  HRT.parse = function (text) {
    let root;
    try { root = JSON.parse(text); } catch (e) { return null; }
    if (!root || typeof root !== 'object') return null;
    const data = HRT.defaultData();
    (root.records || []).forEach(function (o) {
      if (!o || typeof o !== 'object') return;
      const items = (o.items || []).map(function (m) {
        if (!HRT.hormone(m.h) || typeof m.v !== 'number' || !isFinite(m.v) || m.v <= 0) return null;
        return { h: m.h, v: m.v, u: String(m.u || '') };
      }).filter(Boolean);
      data.records.push({ id: String(o.id || HRT.uid()), date: Number(o.date) || 0, note: String(o.note || ''), items: items });
    });
    (root.events || []).forEach(function (o) {
      if (!o || typeof o !== 'object') return;
      data.events.push({
        id: String(o.id || HRT.uid()),
        date: Number(o.date) || 0,
        type: HRT.EVENT_TYPES.indexOf(o.type) >= 0 ? o.type : 'other',
        note: String(o.note || '')
      });
    });
    (root.ranges || []).forEach(function (o) {
      if (!o || typeof o !== 'object' || !HRT.hormone(o.h)) return;
      if (typeof o.min !== 'number' || typeof o.max !== 'number' || !isFinite(o.min) || !isFinite(o.max)) return;
      data.ranges.push({
        id: String(o.id || HRT.uid()),
        h: o.h,
        label: o.label ? String(o.label) : '',
        min: o.min,
        max: o.max,
        unit: String(o.unit || ''),
        builtin: false,
        visible: o.visible !== false
      });
    });
    if (root.targets && typeof root.targets === 'object') {
      Object.keys(root.targets).forEach(function (k) {
        if (HRT.hormone(k)) data.targets[k] = String(root.targets[k]);
      });
    }
    return data;
  };

  HRT.mergeBuiltins = function (data) {
    const ids = {};
    data.ranges.forEach(function (r) { ids[r.id] = true; });
    HRT.BUILTIN_RANGES.forEach(function (r) {
      if (!ids[r.id]) {
        data.ranges.push(Object.assign({}, r));
        ids[r.id] = true;
      }
    });
    Object.keys(HRT.DEFAULT_TARGETS).forEach(function (k) {
      if (!data.targets[k]) data.targets[k] = HRT.DEFAULT_TARGETS[k];
    });
    data.records.sort(function (a, b) { return b.date - a.date; });
    data.events.sort(function (a, b) { return b.date - a.date; });
    return data;
  };

  HRT.loadData = function () {
    let data = null;
    const raw = HRT.storage.get(HRT.DATA_KEY);
    if (raw) data = HRT.parse(raw);
    if (!data) data = HRT.defaultData();
    return HRT.mergeBuiltins(data);
  };

  HRT.loadPrefs = function () {
    const p = HRT.defaultPrefs();
    const raw = HRT.storage.get(HRT.PREFS_KEY);
    if (raw) {
      try {
        const o = JSON.parse(raw);
        Object.keys(p).forEach(function (k) { if (o[k] !== undefined) p[k] = o[k]; });
      } catch (e) { /* 忽略损坏的配置 */ }
    }
    return p;
  };

  HRT.saveData = function (data) {
    HRT.storage.set(HRT.DATA_KEY, HRT.serialize(data));
  };
  HRT.savePrefs = function (prefs) {
    HRT.storage.set(HRT.PREFS_KEY, JSON.stringify(prefs));
  };

  /* ---------- 数据操作 ---------- */
  HRT.upsertRecord = function (data, record) {
    data.records = data.records.filter(function (r) { return r.id !== record.id; });
    data.records.push(record);
    data.records.sort(function (a, b) { return b.date - a.date; });
  };
  HRT.deleteRecord = function (data, id) {
    data.records = data.records.filter(function (r) { return r.id !== id; });
  };
  HRT.upsertEvent = function (data, ev) {
    data.events = data.events.filter(function (e) { return e.id !== ev.id; });
    data.events.push(ev);
    data.events.sort(function (a, b) { return b.date - a.date; });
  };
  HRT.deleteEvent = function (data, id) {
    data.events = data.events.filter(function (e) { return e.id !== id; });
  };
  HRT.upsertRange = function (data, range) {
    data.ranges = data.ranges.filter(function (r) { return r.id !== range.id; });
    data.ranges.push(range);
  };
  HRT.deleteRange = function (data, id) {
    data.ranges = data.ranges.filter(function (r) { return r.id !== id; });
    Object.keys(data.targets).forEach(function (k) {
      if (data.targets[k] === id) delete data.targets[k];
    });
  };
  HRT.setRangeVisible = function (data, id, visible) {
    data.ranges.forEach(function (r) { if (r.id === id) r.visible = visible; });
  };
  HRT.setTarget = function (data, hormoneKey, rangeId) {
    if (rangeId) data.targets[hormoneKey] = rangeId;
    else delete data.targets[hormoneKey];
  };
  HRT.targetOf = function (data, hormoneKey) {
    const id = data.targets[hormoneKey];
    if (!id) return null;
    for (const r of data.ranges) if (r.id === id) return r;
    return null;
  };
  HRT.importData = function (data, parsed, replace) {
    const builtins = {};
    HRT.BUILTIN_RANGES.forEach(function (r) { builtins[r.id] = r; });
    if (replace) {
      const ranges = parsed.ranges.slice();
      const ids = {};
      ranges.forEach(function (r) { ids[r.id] = true; });
      HRT.BUILTIN_RANGES.forEach(function (r) { if (!ids[r.id]) ranges.push(Object.assign({}, r)); });
      const targets = Object.assign({}, HRT.DEFAULT_TARGETS, parsed.targets);
      return {
        records: parsed.records.slice().sort(function (a, b) { return b.date - a.date; }),
        events: parsed.events.slice().sort(function (a, b) { return b.date - a.date; }),
        ranges: ranges,
        targets: targets
      };
    }
    const byId = {};
    data.records.concat(parsed.records).forEach(function (r) { byId[r.id] = r; });
    const records = Object.keys(byId).map(function (k) { return byId[k]; })
      .sort(function (a, b) { return b.date - a.date; });
    const evById = {};
    data.events.concat(parsed.events).forEach(function (e) { evById[e.id] = e; });
    const events = Object.keys(evById).map(function (k) { return evById[k]; })
      .sort(function (a, b) { return b.date - a.date; });
    const rById = {};
    data.ranges.filter(function (r) { return !r.builtin; }).concat(parsed.ranges)
      .forEach(function (r) { rById[r.id] = r; });
    const ranges = Object.keys(rById).map(function (k) { return rById[k]; });
    const rangeIds = {};
    ranges.forEach(function (r) { rangeIds[r.id] = true; });
    HRT.BUILTIN_RANGES.forEach(function (r) { if (!rangeIds[r.id]) ranges.push(Object.assign({}, r)); });
    const targets = Object.assign({}, HRT.DEFAULT_TARGETS, data.targets, parsed.targets);
    return { records: records, events: events, ranges: ranges, targets: targets };
  };

  /* ---------- CSV（与 Android Csv.kt 一致） ---------- */
  HRT.csvEscape = function (s) { return '"' + String(s).replace(/"/g, '""') + '"'; };
  HRT.buildRecordsCsv = function (data) {
    const lines = [];
    let head = '日期,备注,';
    HRT.HORMONES.forEach(function (h) { head += h.short + '值,' + h.short + '单位,'; });
    lines.push(head);
    data.records.forEach(function (r) {
      let line = HRT.fmtDate(r.date) + ',' + HRT.csvEscape(r.note) + ',';
      HRT.HORMONES.forEach(function (h) {
        let found = null;
        for (const m of r.items) if (m.h === h.key) { found = m; break; }
        if (found) line += HRT.fmt(found.v) + ',' + found.u + ',';
        else line += ',,';
      });
      lines.push(line);
    });
    return lines.join('\n');
  };

  if (typeof module !== 'undefined' && module.exports) module.exports = HRT;
})(typeof window !== 'undefined' ? window : globalThis);
