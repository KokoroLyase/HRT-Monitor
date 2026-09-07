/* 激素监测（Web）—— 本地统计报告 + OpenAI 兼容 AI 客户端 */
(function (global) {
  'use strict';
  const HRT = global.HRT = global.HRT || {};

  function statusText(status) {
    if (status === 'LOW') return HRT.t('report_status_low');
    if (status === 'HIGH') return HRT.t('report_status_high');
    if (status === 'IN') return HRT.t('report_status_in');
    return HRT.t('report_status_no_target');
  }

  function targetText(data, hKey) {
    const t = HRT.targetOf(data, hKey);
    if (!t) return null;
    const span = t.thresholdOnly
      ? ('≥ ' + HRT.fmt(t.min) + ' ' + t.unit)
      : (HRT.fmt(t.min) + ' – ' + HRT.fmt(t.max) + ' ' + t.unit);
    return HRT.rangeLabel(t) + '（' + span + '）';
  }

  function seriesOf(data, hKey) {
    const out = [];
    data.records.slice().sort(function (a, b) { return a.date - b.date; }).forEach(function (r) {
      (r.items || []).forEach(function (m) {
        if (m.h === hKey) out.push({ t: r.date, v: HRT.toBase(hKey, m.v, m.u) });
      });
    });
    return out;
  }

  /* 生成纯文本的本地统计报告 */
  HRT.buildLocalReport = function (data) {
    const lines = [];
    lines.push(HRT.t('report_header'));
    lines.push(HRT.t('report_generated_at', HRT.fmtDate(Date.now())));
    lines.push(HRT.t('report_record_count', data.records.length));
    lines.push(HRT.t('report_event_count', data.events.length));
    lines.push('');
    HRT.HORMONES.forEach(function (h) {
      const series = seriesOf(data, h.key);
      if (!series.length) return;
      const unit = HRT.baseUnit(h.key);
      lines.push(HRT.t('report_section_hormone', HRT.t(HRT.hormoneLabelKey(h.key)), unit));
      const values = series.map(function (p) { return p.v; });
      const latest = series[series.length - 1];
      const target = HRT.targetOf(data, h.key);
      const st = target ? statusText(HRT.statusOf(latest.v, target)) : HRT.t('report_status_no_target');
      lines.push(HRT.t('report_line_stats', series.length, HRT.fmtDate(latest.t),
        HRT.fmt(HRT.fromBase(h.key, latest.v, unit)), unit, st));
      const min = Math.min.apply(null, values);
      const max = Math.max.apply(null, values);
      const avg = values.reduce(function (a, b) { return a + b; }, 0) / values.length;
      let ratio = HRT.t('report_no_value');
      if (target) {
        const inCount = values.filter(function (v) { return HRT.statusOf(v, target) === 'IN'; }).length;
        ratio = Math.floor(inCount * 100 / values.length) + '%（' + inCount + '/' + values.length + '）';
      }
      lines.push(HRT.t('report_line_range',
        HRT.fmt(HRT.fromBase(h.key, min, unit)),
        HRT.fmt(HRT.fromBase(h.key, max, unit)),
        HRT.fmt(HRT.fromBase(h.key, avg, unit)), ratio));
      const tt = targetText(data, h.key);
      if (tt) lines.push('  ' + HRT.t('target') + '：' + tt);
      lines.push('');
    });
    if (data.events.length) {
      lines.push(HRT.t('report_recent_events'));
      data.events.slice(0, 5).forEach(function (e) {
        lines.push('  ' + HRT.fmtDate(e.date) + ' ' + HRT.t('event_type_' + e.type) + (e.note ? '：' + e.note : ''));
      });
      lines.push('');
    }
    lines.push(HRT.t('report_local_footer'));
    lines.push(HRT.t('disclaimer_banner'));
    return lines.join('\n');
  };

  /* 生成发送给 AI 的数据摘要（统一使用基准单位） */
  HRT.buildAiPrompt = function (data) {
    const lines = [];
    lines.push('以下是我的性激素化验数据摘要（共 ' + data.records.length + ' 条记录）：');
    HRT.HORMONES.forEach(function (h) {
      const series = seriesOf(data, h.key);
      if (!series.length) return;
      const unit = HRT.baseUnit(h.key);
      const values = series.map(function (p) { return p.v; });
      const latest = series[series.length - 1];
      const target = HRT.targetOf(data, h.key);
      const targetSpan = target
        ? (target.thresholdOnly
          ? ('≥ ' + HRT.fmt(HRT.rangeMinBase(target)) + ' ' + unit)
          : (HRT.fmt(HRT.rangeMinBase(target)) + ' – ' + HRT.fmt(HRT.rangeMaxBase(target)) + ' ' + unit))
        : '未设置';
      const st = target ? statusText(HRT.statusOf(latest.v, target)) : '-';
      const min = Math.min.apply(null, values);
      const max = Math.max.apply(null, values);
      const avg = values.reduce(function (a, b) { return a + b; }, 0) / values.length;
      lines.push('- ' + HRT.t(HRT.hormoneLabelKey(h.key)) + '：共 ' + series.length + ' 次；最近 '
        + HRT.fmtDate(latest.t) + ' 为 ' + HRT.fmt(latest.v) + ' ' + unit
        + '（目标范围 ' + targetSpan + '，判定：' + st + '）；历史范围 '
        + HRT.fmt(min) + ' – ' + HRT.fmt(max) + ' ' + unit + '，平均 ' + HRT.fmt(avg) + ' ' + unit + '。');
    });
    if (data.events.length) {
      lines.push('近期事件：');
      data.events.slice(0, 10).forEach(function (e) {
        lines.push('- ' + HRT.fmtDate(e.date) + ' ' + HRT.t('event_type_' + e.type) + (e.note ? '：' + e.note : ''));
      });
    }
    lines.push('');
    lines.push('请基于以上数据给出解读：各项指标的含义、变化趋势、与目标/参考范围的关系、需要关注的异常，以及建议与医生沟通的要点。');
    return lines.join('\n');
  };

  /* OpenAI 兼容 chat/completions 请求 */
  HRT.aiChat = async function (prefs, system, user) {
    const base = (prefs.aiBaseUrl || '').trim().replace(/\/+$/, '');
    if (!base) throw new Error('接口地址为空');
    if (!(prefs.aiKey || '').trim()) throw new Error('API 密钥为空');
    if (!(prefs.aiModel || '').trim()) throw new Error('模型名称为空');
    const res = await fetch(base + '/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + prefs.aiKey.trim()
      },
      body: JSON.stringify({
        model: prefs.aiModel.trim(),
        temperature: 0.3,
        max_tokens: 2048,
        messages: [
          { role: 'system', content: system },
          { role: 'user', content: user }
        ]
      })
    });
    const text = await res.text();
    if (!res.ok) {
      let msg = 'HTTP ' + res.status;
      try {
        const j = JSON.parse(text);
        if (j && j.error && j.error.message) msg += '：' + j.error.message;
      } catch (e) { /* 忽略 */ }
      throw new Error(msg);
    }
    let j;
    try { j = JSON.parse(text); } catch (e) { throw new Error('响应格式无效'); }
    const content = j && j.choices && j.choices[0] && j.choices[0].message && j.choices[0].message.content;
    if (!content) throw new Error('响应格式无效');
    return content;
  };
})(typeof window !== 'undefined' ? window : globalThis);
