/* 激素监测（Web）—— Canvas 趋势图表（自绘，零依赖） */
(function (global) {
  'use strict';
  const HRT = global.HRT = global.HRT || {};

  const BAND_PALETTE = ['#8E5BD7', '#E8639F', '#2E9E8F', '#E8963C', '#5C8FDE', '#7BA23F'];

  HRT.bandColor = function (range) {
    let h = 0;
    for (let i = 0; i < range.id.length; i++) h = (h * 31 + range.id.charCodeAt(i)) >>> 0;
    return BAND_PALETTE[h % BAND_PALETTE.length];
  };

  function computeYRange(values, ranges) {
    let lo = Math.min.apply(null, values);
    let hi = Math.max.apply(null, values);
    if (lo === hi) { lo -= 1; hi += 1; }
    ranges.forEach(function (r) {
      if (r.thresholdOnly) {
        const rmin = HRT.rangeMinBase(r);
        if (rmin > hi && rmin <= hi * 1.35) hi = rmin;
      } else {
        if (HRT.rangeMinBase(r) < lo) lo = HRT.rangeMinBase(r);
        if (HRT.rangeMaxBase(r) > hi) hi = HRT.rangeMaxBase(r);
      }
    });
    const pad = (hi - lo) * 0.08;
    return [lo - pad, hi + pad];
  }

  function niceStep(raw) {
    if (raw <= 0) return 1;
    const mag = Math.pow(10, Math.floor(Math.log10(raw)));
    const norm = raw / mag;
    return (norm <= 1 ? 1 : norm <= 2 ? 2 : norm <= 5 ? 5 : 10) * mag;
  }

  function hexToRgba(hex, alpha) {
    const n = parseInt(hex.slice(1), 16);
    return 'rgba(' + ((n >> 16) & 255) + ',' + ((n >> 8) & 255) + ',' + (n & 255) + ',' + alpha + ')';
  }

  /* 绘制图表。points: [{t: 毫秒, v: 基准值}]（按时间升序）
   * 返回点击命中函数 (clientX, clientY) → 点索引或 null */
  HRT.drawChart = function (canvas, points, ranges, target, hormoneKey, displayUnit, selectedIndex, dark) {
    const dpr = global.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const W = Math.max(60, rect.width);
    const H = Math.max(90, rect.height);
    canvas.width = Math.round(W * dpr);
    canvas.height = Math.round(H * dpr);
    const ctx = canvas.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);

    const PAD_L = 52, PAD_R = 10, PAD_T = 10, PAD_B = 22;
    const plotW = W - PAD_L - PAD_R;
    const plotH = H - PAD_T - PAD_B;
    const yRange = computeYRange(points.map(function (p) { return p.v; }), ranges);
    const minT = points[0].t;
    const maxT = points[points.length - 1].t;
    const spanDays = (maxT - minT) / 86400000;

    function xOf(t) {
      if (maxT <= minT) return PAD_L + plotW / 2;
      return PAD_L + (t - minT) / (maxT - minT) * plotW;
    }
    function yOf(v) {
      return PAD_T + (yRange[1] - v) / (yRange[1] - yRange[0]) * plotH;
    }

    const lineColor = dark ? '#D3B5FF' : '#7B4FBF';
    const gridColor = dark ? 'rgba(148,143,153,0.35)' : 'rgba(122,117,127,0.30)';
    const labelColor = dark ? '#CBC4CF' : '#49454E';

    // 1. 参考范围色带 / 阈值虚线
    ranges.forEach(function (r) {
      const color = HRT.bandColor(r);
      if (r.thresholdOnly) {
        const y = yOf(HRT.rangeMinBase(r));
        if (y >= PAD_T && y <= H - PAD_B) {
          ctx.strokeStyle = color;
          ctx.lineWidth = 2;
          ctx.setLineDash([6, 4]);
          ctx.beginPath();
          ctx.moveTo(PAD_L, y);
          ctx.lineTo(W - PAD_R, y);
          ctx.stroke();
          ctx.setLineDash([]);
        }
      } else {
        const alpha = r.id === (target && target.id) ? 0.22 : 0.11;
        const yTop = yOf(HRT.rangeMaxBase(r));
        const yBot = yOf(HRT.rangeMinBase(r));
        ctx.fillStyle = hexToRgba(color, alpha);
        ctx.fillRect(PAD_L, yTop, plotW, Math.max(0, yBot - yTop));
      }
    });

    // 2. 网格线与坐标标签
    ctx.font = '11px -apple-system, "Segoe UI", system-ui, sans-serif';
    const step = niceStep((yRange[1] - yRange[0]) / 4);
    let tick = Math.ceil(yRange[0] / step) * step;
    while (tick <= yRange[1] + step * 0.001) {
      const y = yOf(tick);
      ctx.strokeStyle = gridColor;
      ctx.lineWidth = 1;
      ctx.beginPath(); ctx.moveTo(PAD_L, y); ctx.lineTo(W - PAD_R, y); ctx.stroke();
      ctx.fillStyle = labelColor;
      ctx.textAlign = 'right';
      ctx.fillText(HRT.fmt(HRT.fromBase(hormoneKey, tick, displayUnit)), PAD_L - 6, y + 4);
      tick += step;
    }
    if (points.length > 1) {
      const ticks = 4;
      ctx.textAlign = 'center';
      for (let k = 0; k <= ticks; k++) {
        const t = minT + (maxT - minT) * k / ticks;
        const x = xOf(t);
        ctx.strokeStyle = gridColor;
        ctx.beginPath(); ctx.moveTo(x, PAD_T); ctx.lineTo(x, H - PAD_B); ctx.stroke();
        ctx.fillStyle = labelColor;
        ctx.fillText(spanDays > 400 ? HRT.fmtYearMonth(t) : HRT.fmtMonthDay(t), x, H - PAD_B + 14);
      }
    }

    // 3. 折线
    if (points.length > 1) {
      ctx.strokeStyle = lineColor;
      ctx.lineWidth = 2.5;
      ctx.lineJoin = 'round';
      ctx.lineCap = 'round';
      ctx.beginPath();
      points.forEach(function (p, i) {
        const x = xOf(p.t), y = yOf(p.v);
        if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
      });
      ctx.stroke();
    }

    // 4. 数据点
    points.forEach(function (p, i) {
      const x = xOf(p.t), y = yOf(p.v);
      if (i === selectedIndex) {
        ctx.fillStyle = dark ? '#1C1821' : '#FFFFFF';
        ctx.beginPath(); ctx.arc(x, y, 7, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = lineColor;
        ctx.beginPath(); ctx.arc(x, y, 4.5, 0, Math.PI * 2); ctx.fill();
      } else {
        ctx.fillStyle = lineColor;
        ctx.beginPath(); ctx.arc(x, y, 3.5, 0, Math.PI * 2); ctx.fill();
      }
    });

    return function (clientX, clientY) {
      const r2 = canvas.getBoundingClientRect();
      const offX = clientX - r2.left;
      if (points.length === 1) return 0;
      let best = null;
      let bestDist = 28;
      for (let i = 0; i < points.length; i++) {
        const d = Math.abs(xOf(points[i].t) - offX);
        if (d <= bestDist) { bestDist = d; best = i; }
      }
      return best;
    };
  };
})(typeof window !== 'undefined' ? window : globalThis);
