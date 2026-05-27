// data.js — Recipe data + rational quantity helpers
// Exposes: window.RECIPE, window.Q

(function () {
  const RECIPE = {
    id: 'cookies',
    title: 'Brown butter chocolate chip cookies',
    titleShort: 'Brown butter cookies',
    category: 'Cookies',
    time: '45 min',
    timeShort: '45m',
    difficulty: 'Easy',
    servings: 12,
    yieldUnit: 'cookies',
    // ingredients ordered: dry → fat/sugar → wet → flavorings → mix-in
    ingredients: [
      { id: 'flour',   name: 'All-purpose flour',     qty: { num: 9, den: 4 }, unit: 'cup', gramsPerCup: 120 },
      { id: 'butter',  name: 'Brown butter',          qty: { num: 1, den: 1 }, unit: 'cup', gramsPerCup: 225, modifier: 'cooled' },
      { id: 'sugar-w', name: 'Granulated sugar',      qty: { num: 3, den: 4 }, unit: 'cup', gramsPerCup: 200 },
      { id: 'sugar-b', name: 'Dark brown sugar',      qty: { num: 3, den: 4 }, unit: 'cup', gramsPerCup: 220, modifier: 'packed' },
      { id: 'eggs',    name: 'Eggs',                  qty: { num: 2, den: 1 }, unit: 'ea',  gramsPerCup: null, modifier: 'large' },
      { id: 'vanilla', name: 'Vanilla extract',       qty: { num: 2, den: 1 }, unit: 'tsp', gramsPerCup: null },
      { id: 'soda',    name: 'Baking soda',           qty: { num: 1, den: 1 }, unit: 'tsp', gramsPerCup: null },
      { id: 'salt',    name: 'Flaky sea salt',        qty: { num: 1, den: 1 }, unit: 'tsp', gramsPerCup: null },
      { id: 'chips',   name: 'Dark chocolate chips',  qty: { num: 2, den: 1 }, unit: 'cup', gramsPerCup: 170 },
    ],
    steps: [
      'Brown the butter in a pan over medium heat until nutty and amber. Cool 10 min.',
      'Whisk dry ingredients. Beat butter and sugars; add eggs and vanilla.',
      'Fold dry into wet, then chocolate. Rest dough 30 min, or chill overnight.',
      'Bake at 180 °C, 12–14 min, until edges set and centers look underdone.',
    ],
  };

  // ── Rational math ───────────────────────────────────────────────
  function gcd(a, b) {
    a = Math.abs(Math.round(a)); b = Math.abs(Math.round(b));
    while (b) { const t = b; b = a % b; a = t; }
    return a || 1;
  }
  function simplify(q) {
    const g = gcd(q.num, q.den);
    return { num: Math.round(q.num / g), den: Math.round(q.den / g) };
  }
  function scaleQty(q, factor) {
    const k = 100000;
    return simplify({ num: Math.round(q.num * factor * k), den: q.den * k });
  }
  function asNumber(q) { return q.num / q.den; }

  // Round whole-count ingredients (eggs etc.) up to nearest integer
  function roundCount(q) {
    return { num: Math.max(1, Math.round(q.num / q.den)), den: 1 };
  }

  // ── Display formatting ──────────────────────────────────────────
  function formatFraction(q, style) {
    const decimal = q.num / q.den;
    if (style === 'decimal') {
      const r = Math.round(decimal * 100) / 100;
      return r.toString();
    }
    const whole = Math.floor(decimal);
    const remainder = decimal - whole;
    if (remainder < 1 / 32) return String(whole);
    if (remainder > 1 - 1 / 32) return String(whole + 1);
    // Snap to common cooking fractions
    const candidates = [[1,8],[1,4],[1,3],[3,8],[1,2],[5,8],[2,3],[3,4],[7,8]];
    let best = candidates[0], bestDelta = Infinity;
    for (const [n, d] of candidates) {
      const delta = Math.abs(remainder - n / d);
      if (delta < bestDelta) { bestDelta = delta; best = [n, d]; }
    }
    const [n, d] = best;
    if (style === 'stacked') {
      const u = { '1/2':'½','1/4':'¼','3/4':'¾','1/8':'⅛','3/8':'⅜','5/8':'⅝','7/8':'⅞','1/3':'⅓','2/3':'⅔' };
      const f = u[`${n}/${d}`] || `${n}/${d}`;
      return whole > 0 ? `${whole}${f}` : f;
    }
    const inline = `${n}/${d}`;
    return whole > 0 ? `${whole} ${inline}` : inline;
  }

  // ── Unit conversion ─────────────────────────────────────────────
  function convert(qty, unit, gramsPerCup, system) {
    if (unit === 'ea') return { qty: roundCount(qty), unit: 'ea' };
    if (system === 'us') return { qty, unit };
    // metric
    if (unit === 'cup' && gramsPerCup) {
      const grams = (qty.num * gramsPerCup) / qty.den;
      const step = grams >= 50 ? 5 : 1;
      const snapped = Math.max(1, Math.round(grams / step) * step);
      return { qty: { num: snapped, den: 1 }, unit: 'g' };
    }
    if (unit === 'tbsp' && gramsPerCup) {
      const grams = (qty.num * gramsPerCup / 16) / qty.den;
      return { qty: { num: Math.max(1, Math.round(grams)), den: 1 }, unit: 'g' };
    }
    // spices/extracts → keep tsp/tbsp (sensible in metric kitchens)
    return { qty, unit };
  }

  function unitLabel(unit, qty) {
    const v = qty ? qty.num / qty.den : 1;
    const plural = v !== 1;
    const map = {
      cup: plural ? 'cups' : 'cup',
      tsp: 'tsp', tbsp: 'tbsp',
      g: 'g', ml: 'ml', kg: 'kg',
      ea: '',
    };
    return map[unit] ?? unit;
  }

  // Display helper: ingredient + scale + tweaks → text bits
  function displayQty(ing, factor, fractionStyle, system) {
    const scaled = scaleQty(ing.qty, factor);
    const conv = convert(scaled, ing.unit, ing.gramsPerCup, system);
    let style = fractionStyle;
    if (conv.unit === 'g' || conv.unit === 'ml' || conv.unit === 'ea') style = 'decimal';
    return {
      qtyText: formatFraction(conv.qty, style),
      unitText: unitLabel(conv.unit, conv.qty),
      unit: conv.unit,
      qtyValue: asNumber(conv.qty),
    };
  }

  // For reverse-scale sheet: given a typed display value, find the new factor
  function factorFromDisplayValue(ing, currentFactor, fractionStyle, system, newDisplayValue) {
    const current = displayQty(ing, currentFactor, fractionStyle, system);
    if (current.qtyValue <= 0) return currentFactor;
    return currentFactor * (newDisplayValue / current.qtyValue);
  }

  window.RECIPE = RECIPE;
  window.Q = { simplify, scaleQty, asNumber, formatFraction, convert, unitLabel, displayQty, factorFromDisplayValue, roundCount };
})();
