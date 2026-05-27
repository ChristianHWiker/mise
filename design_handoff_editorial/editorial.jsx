// variants/editorial.jsx
// V1 — Editorial / cookbook.
// Warm cream paper, Source Serif 4 headers, Geist body, terracotta accent.
// Photo-forward, single-column flow, thin rules between items.

const EditorialPalette = {
  light: {
    bg: '#faf6ec', surface: '#fdfbf3', text: '#1f1a14',
    muted: '#7a6f5d', mutedSoft: '#b4a895', rule: '#e5dcc7',
    accent: '#a04830', accentSoft: '#f1dccc',
    photoFrom: '#cd9c70', photoTo: '#6b3e22',
    frame: '#d8c9ad',
  },
  dark: {
    bg: '#181410', surface: '#221c15', text: '#f1ead8',
    muted: '#a39780', mutedSoft: '#6e6451', rule: '#322a1f',
    accent: '#dc8462', accentSoft: '#3a2418',
    photoFrom: '#7a5132', photoTo: '#2c1c10',
    frame: '#2c241b',
  },
};

const edSerif = "'Source Serif 4', Georgia, serif";
const edSans = "'Geist', system-ui, sans-serif";

function EditorialVariant({ tweaks, onBack }) {
  const c = tweaks.dark ? EditorialPalette.dark : EditorialPalette.light;
  const [factor, setFactor] = React.useState(1);
  const [sheetId, setSheetId] = React.useState(null);

  const servings = Math.max(1, Math.round(RECIPE.servings * factor));
  const setServings = (n) => setFactor(Math.max(1, n) / RECIPE.servings);
  const sheetIng = sheetId ? RECIPE.ingredients.find(i => i.id === sheetId) : null;

  return (
    <Phone dark={tweaks.dark} bg={c.bg} frameColor={c.frame}>
      <div style={{
        flex: 1, overflowY: 'auto', background: c.bg, color: c.text,
        fontFamily: edSans, fontSize: 14,
      }}>
        {/* top bar */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 16px 4px' }}>
          <button style={edIconBtn(c.text)} aria-label="Back" onClick={onBack}>‹</button>
          <div style={{ fontSize: 10, letterSpacing: '0.14em', textTransform: 'uppercase', color: c.muted, fontFamily: edSans }}>
            № 012 · Cookbook
          </div>
          <button style={edIconBtn(c.text)} aria-label="Save">♡</button>
        </div>

        {/* hero photo */}
        <div style={{
          margin: '6px 18px 0', height: 220, position: 'relative', overflow: 'hidden',
          background: `linear-gradient(135deg, ${c.photoFrom}, ${c.photoTo})`,
          borderRadius: 2,
        }}>
          <PhotoPlaceholder tone="#fff" captionTone="rgba(255,255,255,0.75)" label="hero shot — cookies on parchment" />
        </div>

        {/* title block */}
        <div style={{ padding: '24px 22px 4px' }}>
          <div style={{
            fontSize: 10, letterSpacing: '0.18em', textTransform: 'uppercase', fontWeight: 600,
            color: c.accent, marginBottom: 10,
          }}>
            {RECIPE.category}
          </div>
          <h1 style={{
            margin: 0, fontFamily: edSerif, fontWeight: 400,
            fontSize: 30, lineHeight: 1.1, letterSpacing: '-0.012em', color: c.text,
          }}>
            {RECIPE.title}
          </h1>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 10, marginTop: 16,
            fontSize: 12, color: c.muted, fontVariantNumeric: 'tabular-nums',
          }}>
            <span>{RECIPE.time}</span>
            <span style={{ opacity: 0.5 }}>·</span>
            <span>{RECIPE.difficulty}</span>
            <span style={{ opacity: 0.5 }}>·</span>
            <span>9 ingredients</span>
          </div>
        </div>

        {/* servings card */}
        <div style={{
          margin: '22px 22px 0', padding: '14px 18px',
          background: c.surface, border: `1px solid ${c.rule}`, borderRadius: 3,
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <div>
            <div style={{ fontSize: 10, letterSpacing: '0.14em', textTransform: 'uppercase', color: c.muted }}>Yield</div>
            <div style={{ fontFamily: edSerif, fontSize: 24, color: c.text, marginTop: 2 }}>
              {servings} <span style={{ color: c.muted, fontSize: 13, fontFamily: edSans }}>cookies</span>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <button onClick={() => setServings(servings - 2)} style={edStepBtn(c)} aria-label="Fewer">−</button>
            <button onClick={() => setServings(servings + 2)} style={edStepBtn(c)} aria-label="More">+</button>
          </div>
        </div>

        {/* hint */}
        <div style={{
          margin: '14px 22px 0', fontSize: 12, color: c.mutedSoft,
          fontStyle: 'italic', fontFamily: edSerif,
        }}>
          Tap an ingredient to scale by what you have →
        </div>

        {/* ingredients */}
        <div style={{ margin: '18px 22px 0' }}>
          <h2 style={{
            fontFamily: edSerif, fontWeight: 400, fontSize: 18, letterSpacing: 0,
            color: c.text, margin: '0 0 4px', paddingBottom: 8, borderBottom: `1px solid ${c.rule}`,
          }}>
            Ingredients
          </h2>
          {RECIPE.ingredients.map((ing, i) => {
            const d = Q.displayQty(ing, factor, tweaks.fractionStyle, tweaks.unitSystem);
            return (
              <button key={ing.id} onClick={() => setSheetId(ing.id)} style={{
                display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
                gap: 12, width: '100%', padding: '13px 0', border: 'none',
                background: 'transparent', borderBottom: `1px solid ${c.rule}`,
                cursor: 'pointer', textAlign: 'left', color: c.text, fontFamily: edSans,
              }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 15, lineHeight: 1.3 }}>{ing.name}</div>
                  {ing.modifier && (
                    <div style={{ fontSize: 12, fontStyle: 'italic', color: c.muted, marginTop: 2, fontFamily: edSerif }}>
                      {ing.modifier}
                    </div>
                  )}
                </div>
                <div style={{
                  fontFamily: edSerif, fontSize: 19, color: c.text,
                  fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap',
                }}>
                  {d.qtyText}
                  {d.unitText && <span style={{ color: c.muted, fontSize: 13, marginLeft: 4, fontFamily: edSans }}>{d.unitText}</span>}
                </div>
              </button>
            );
          })}
        </div>

        {/* method */}
        <div style={{ margin: '28px 22px 0', paddingBottom: 40 }}>
          <h2 style={{
            fontFamily: edSerif, fontWeight: 400, fontSize: 18,
            color: c.text, margin: '0 0 4px', paddingBottom: 8, borderBottom: `1px solid ${c.rule}`,
          }}>
            Method
          </h2>
          {RECIPE.steps.map((step, i) => (
            <div key={i} style={{
              display: 'flex', gap: 14, padding: '14px 0',
              borderBottom: i < RECIPE.steps.length - 1 ? `1px solid ${c.rule}` : 'none',
            }}>
              <div style={{
                fontFamily: edSerif, fontSize: 24, color: c.accent, lineHeight: 1,
                fontWeight: 400, minWidth: 22, fontStyle: 'italic',
              }}>
                {i + 1}
              </div>
              <div style={{ fontSize: 14, lineHeight: 1.6, color: c.text }}>{step}</div>
            </div>
          ))}
        </div>
      </div>

      {sheetIng && (
        <EditorialSheet
          c={c} ing={sheetIng} factor={factor} tweaks={tweaks}
          onApply={(f) => { setFactor(f); setSheetId(null); }}
          onClose={() => setSheetId(null)}
        />
      )}
    </Phone>
  );
}

function EditorialSheet({ c, ing, factor, tweaks, onApply, onClose }) {
  const current = Q.displayQty(ing, factor, tweaks.fractionStyle, tweaks.unitSystem);
  const [value, setValue] = React.useState(current.qtyValue);
  React.useEffect(() => { setValue(current.qtyValue); /* eslint-disable-next-line */ }, [ing.id]);

  const newFactor = Q.factorFromDisplayValue(ing, factor, tweaks.fractionStyle, tweaks.unitSystem, value);
  const newServings = Math.max(1, Math.round(RECIPE.servings * newFactor));

  const unit = current.unit;
  const step = unit === 'g' ? 5 : unit === 'ea' ? 1 : 0.25;
  const adj = (delta) => setValue(Math.max(0, Math.round((value + delta) * 1000) / 1000));

  // Format the central display value (treat as fraction unless gram/count)
  const valueAsFrac = (() => {
    if (unit === 'g' || unit === 'ml' || unit === 'ea') return String(Math.round(value));
    return Q.formatFraction({ num: Math.round(value * 1000), den: 1000 }, tweaks.fractionStyle);
  })();
  const unitLbl = Q.unitLabel(unit, { num: value, den: 1 });

  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 10, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'rgba(20,12,4,0.4)' }} />
      <div style={{
        position: 'relative', background: c.bg, color: c.text,
        borderTopLeftRadius: 4, borderTopRightRadius: 4,
        boxShadow: '0 -10px 30px rgba(0,0,0,0.25)',
        padding: '14px 22px 22px', fontFamily: edSans,
        borderTop: `4px solid ${c.accent}`,
      }}>
        <div style={{ width: 40, height: 3, background: c.mutedSoft, borderRadius: 2, margin: '0 auto 16px' }} />
        <div style={{
          fontSize: 10, letterSpacing: '0.16em', textTransform: 'uppercase', color: c.accent,
          marginBottom: 6, fontWeight: 600,
        }}>
          Reverse scale
        </div>
        <h3 style={{
          fontFamily: edSerif, fontWeight: 400, fontSize: 22, lineHeight: 1.2,
          margin: '0 0 8px', color: c.text, letterSpacing: '-0.01em',
        }}>
          How much {ing.name.toLowerCase()} do you have?
        </h3>
        <div style={{ fontSize: 13, color: c.muted, marginBottom: 18, lineHeight: 1.45 }}>
          Recipe calls for <span style={{ color: c.text, fontFamily: edSerif, fontSize: 15 }}>
            {current.qtyText} {current.unitText}
          </span>. We'll scale everything else to match.
        </div>

        <div style={{
          display: 'flex', alignItems: 'center', gap: 12, padding: '14px 10px',
          background: c.accentSoft, borderRadius: 2, marginBottom: 14,
        }}>
          <button onClick={() => adj(-step)} style={edStepBtn(c, true)} aria-label="Less">−</button>
          <div style={{ flex: 1, textAlign: 'center' }}>
            <div style={{
              fontFamily: edSerif, fontSize: 38, color: c.text,
              fontVariantNumeric: 'tabular-nums', lineHeight: 1,
            }}>
              {valueAsFrac}
            </div>
            {unitLbl && <div style={{ fontSize: 11, color: c.muted, marginTop: 4, letterSpacing: '0.08em', textTransform: 'uppercase' }}>{unitLbl}</div>}
          </div>
          <button onClick={() => adj(step)} style={edStepBtn(c, true)} aria-label="More">+</button>
        </div>

        <div style={{
          padding: '11px 14px', marginBottom: 16, background: c.surface,
          border: `1px solid ${c.rule}`, borderRadius: 2,
          display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
        }}>
          <span style={{ fontSize: 12, color: c.muted, letterSpacing: '0.08em', textTransform: 'uppercase' }}>Yields</span>
          <span style={{ fontFamily: edSerif, fontSize: 20, color: c.text }}>
            {newServings} <span style={{ fontSize: 13, color: c.muted, fontFamily: edSans }}>cookies</span>
          </span>
        </div>

        <div style={{ display: 'flex', gap: 10 }}>
          <button onClick={onClose} style={{
            flex: 1, padding: '13px 16px', borderRadius: 2,
            border: `1px solid ${c.rule}`, background: 'transparent', color: c.text,
            fontSize: 14, cursor: 'pointer', fontFamily: edSans, fontWeight: 500,
          }}>
            Cancel
          </button>
          <button onClick={() => onApply(newFactor)} style={{
            flex: 2, padding: '13px 16px', borderRadius: 2, border: 'none',
            background: c.accent, color: '#fff', fontSize: 14, cursor: 'pointer',
            fontFamily: edSans, fontWeight: 500, letterSpacing: '0.02em',
          }}>
            Scale recipe
          </button>
        </div>
      </div>
    </div>
  );
}

function edIconBtn(color) {
  return {
    width: 30, height: 30, borderRadius: '50%', border: 'none', background: 'transparent',
    color, fontSize: 22, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
    lineHeight: 1, fontFamily: edSerif, padding: 0,
  };
}
function edStepBtn(c, soft = false) {
  return {
    width: 38, height: 38, borderRadius: '50%',
    border: `1px solid ${c.rule}`, background: soft ? c.surface : c.surface, color: c.text,
    fontSize: 18, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontFamily: edSerif, padding: 0, lineHeight: 1,
  };
}

// ───────────────────────────────────────────────────────────────
// Editorial Home — the cookbook table of contents.
// Same visual system: warm cream, serif masthead, terracotta accent.
// ───────────────────────────────────────────────────────────────

const EditorialLibrary = [
  { id: 'cookies',   title: 'Brown butter chocolate chip cookies',  cat: 'Cookies',   yield: '12 cookies',  time: '45 min', cooked: '2 days ago',  pin: true,  tone: ['#cd9c70', '#6b3e22'] },
  { id: 'gnocchi',   title: 'Sunday gnocchi al pomodoro',           cat: 'Pasta',     yield: '4 servings',  time: '1h 10m', cooked: 'last week',   pin: true,  tone: ['#d97554', '#7a3a26'] },
  { id: 'sourdough', title: 'No-knead sourdough boule',             cat: 'Bread',     yield: '1 loaf',      time: '18 h',   cooked: '5 days ago',  pin: true,  tone: ['#b58454', '#5e3a1f'] },
  { id: 'roast',     title: 'Roast chicken with bread salad',       cat: 'Mains',     yield: '4 servings',  time: '1h 30m', cooked: '2 weeks ago', pin: false, tone: ['#c08555', '#643922'] },
  { id: 'bolognese', title: 'Slow-cooked bolognese',                cat: 'Pasta',     yield: '6 servings',  time: '3 h',    cooked: '3 weeks ago', pin: false, tone: ['#a64a30', '#3a1a10'] },
  { id: 'galette',   title: 'Apple-thyme galette',                  cat: 'Dessert',   yield: '8 slices',    time: '1 h',    cooked: 'a month ago', pin: false, tone: ['#c9925d', '#5a3818'] },
  { id: 'pancakes',  title: 'Buttermilk pancakes',                  cat: 'Breakfast', yield: '12 cakes',    time: '25 min', cooked: 'a month ago', pin: false, tone: ['#dba569', '#6b3f1a'] },
];

function EditorialHome({ tweaks, onOpen }) {
  const c = tweaks.dark ? EditorialPalette.dark : EditorialPalette.light;
  const [tag, setTag] = React.useState('All');
  const [search, setSearch] = React.useState('');

  const tags = ['All', 'Pinned', 'Cookies', 'Pasta', 'Bread', 'Mains', 'Dessert', 'Breakfast'];
  const filtered = EditorialLibrary.filter(r => {
    if (tag === 'Pinned' && !r.pin) return false;
    if (tag !== 'All' && tag !== 'Pinned' && r.cat !== tag) return false;
    if (search && !r.title.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  return (
    <Phone dark={tweaks.dark} bg={c.bg} frameColor={c.frame}>
      <div style={{
        flex: 1, overflowY: 'auto', background: c.bg, color: c.text,
        fontFamily: edSans, fontSize: 14,
      }}>
        {/* top bar */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '10px 18px 4px',
        }}>
          <button style={edIconBtn(c.text)} aria-label="Menu">≡</button>
          <div style={{
            fontSize: 10, letterSpacing: '0.18em', textTransform: 'uppercase',
            color: c.muted, fontFamily: edSans, fontWeight: 600,
          }}>
            Cookbook
          </div>
          <button style={edIconBtn(c.text)} aria-label="Profile">⋯</button>
        </div>

        {/* masthead */}
        <div style={{ padding: '22px 22px 6px' }}>
          <div style={{
            fontSize: 10, letterSpacing: '0.18em', textTransform: 'uppercase',
            color: c.accent, marginBottom: 10, fontWeight: 600,
          }}>
            Tuesday · 27 May
          </div>
          <h1 style={{
            margin: 0, fontFamily: edSerif, fontWeight: 400,
            fontSize: 40, lineHeight: 0.98, letterSpacing: '-0.02em', color: c.text,
          }}>
            My cookbook
          </h1>
          <div style={{
            fontSize: 12, color: c.muted, marginTop: 12,
            fontVariantNumeric: 'tabular-nums',
          }}>
            47 recipes · 3 pinned · last cooked 2 days ago
          </div>
        </div>

        {/* search */}
        <div style={{ padding: '16px 22px 0' }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 10,
            padding: '11px 14px', background: c.surface,
            border: `1px solid ${c.rule}`, borderRadius: 2,
          }}>
            <span style={{ fontSize: 14, color: c.mutedSoft, lineHeight: 1 }}>⌕</span>
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search recipes, ingredients…"
              style={{
                flex: 1, border: 'none', outline: 'none', background: 'transparent',
                fontFamily: edSans, fontSize: 13, color: c.text,
              }}
            />
          </div>
        </div>

        {/* tag filter */}
        <div style={{
          marginTop: 18,
          overflowX: 'auto', whiteSpace: 'nowrap',
          scrollbarWidth: 'none',
          borderBottom: `1px solid ${c.rule}`,
        }}>
          <div style={{ padding: '0 22px', display: 'inline-flex', gap: 0 }}>
            {tags.map(t => (
              <button key={t} onClick={() => setTag(t)} style={{
                border: 'none', background: 'transparent', cursor: 'pointer',
                padding: '8px 0', marginRight: 18, fontFamily: edSans,
                fontSize: 11, letterSpacing: '0.1em', textTransform: 'uppercase',
                fontWeight: 600,
                color: tag === t ? c.text : c.muted,
                borderBottom: tag === t ? `2px solid ${c.accent}` : '2px solid transparent',
                marginBottom: -1,
              }}>
                {t}
              </button>
            ))}
          </div>
        </div>

        {/* section header */}
        <div style={{
          padding: '22px 22px 4px',
          display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
        }}>
          <h2 style={{
            fontFamily: edSerif, fontWeight: 400, fontSize: 18, margin: 0, color: c.text,
            letterSpacing: '-0.005em',
          }}>
            {tag === 'All' ? 'All recipes' : tag}
          </h2>
          <span style={{ fontSize: 11, color: c.muted, fontVariantNumeric: 'tabular-nums', letterSpacing: '0.04em' }}>
            {filtered.length} {filtered.length === 1 ? 'recipe' : 'recipes'}
          </span>
        </div>

        {/* recipe list */}
        <div style={{ padding: '0 22px 16px' }}>
          {filtered.map((r, i) => (
            <button key={r.id} onClick={() => onOpen && onOpen(r.id)} style={{
              display: 'flex', alignItems: 'center', gap: 14,
              width: '100%', padding: '14px 0',
              borderTop: i === 0 ? `1px solid ${c.rule}` : 'none',
              borderBottom: `1px solid ${c.rule}`,
              background: 'transparent', cursor: 'pointer', textAlign: 'left',
              fontFamily: edSans, color: c.text, font: 'inherit',
            }}>
              {/* thumbnail */}
              <div style={{
                width: 64, height: 64, flexShrink: 0, borderRadius: 2,
                background: `linear-gradient(135deg, ${r.tone[0]}, ${r.tone[1]})`,
                position: 'relative', overflow: 'hidden',
              }}>
                <div style={{
                  position: 'absolute', inset: 0,
                  backgroundImage: `repeating-linear-gradient(45deg, transparent 0 6px, rgba(255,255,255,0.18) 6px 7px)`,
                }} />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{
                  fontFamily: edSerif, fontSize: 17, color: c.text,
                  lineHeight: 1.2, marginBottom: 5, letterSpacing: '-0.005em',
                }}>
                  {r.title}
                </div>
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 6,
                  fontSize: 11, color: c.muted, letterSpacing: '0.04em',
                  fontVariantNumeric: 'tabular-nums',
                }}>
                  <span style={{ textTransform: 'uppercase', letterSpacing: '0.08em' }}>{r.cat}</span>
                  <span style={{ opacity: 0.5 }}>·</span>
                  <span>{r.yield}</span>
                  <span style={{ opacity: 0.5 }}>·</span>
                  <span>{r.time}</span>
                </div>
              </div>
              {r.pin && (
                <div style={{
                  fontSize: 11, color: c.accent, lineHeight: 1,
                  fontFamily: edSerif,
                }} title="Pinned">
                  ★
                </div>
              )}
            </button>
          ))}

          {filtered.length === 0 && (
            <div style={{
              padding: '50px 0', textAlign: 'center', color: c.muted,
              fontFamily: edSerif, fontStyle: 'italic', fontSize: 14,
            }}>
              Nothing matches that — yet.
            </div>
          )}
        </div>

        <div style={{ height: 92 }} />
      </div>

      {/* FAB — extended pill */}
      <div style={{
        position: 'absolute', bottom: 18, right: 18, zIndex: 5,
        display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 10,
      }}>
        <button style={{
          height: 52, padding: '0 22px', borderRadius: 999,
          background: c.accent, color: '#fff', border: 'none',
          fontFamily: edSans, fontSize: 14, fontWeight: 500, letterSpacing: '0.01em',
          cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 10,
          boxShadow: '0 10px 28px rgba(160,72,48,0.4)',
        }}>
          <span style={{ fontSize: 20, lineHeight: 1, marginTop: -1, fontWeight: 400 }}>+</span>
          <span>Add recipe</span>
        </button>
      </div>
    </Phone>
  );
}

// ───────────────────────────────────────────────────────────────
// EditorialApp — full Editorial flow with Home ↔ Detail navigation.
// Tap a recipe in Home → drill into Detail. Detail back arrow → Home.
// ───────────────────────────────────────────────────────────────

function EditorialApp({ tweaks, startScreen = 'home' }) {
  const [screen, setScreen] = React.useState(startScreen);

  // Reset when startScreen prop changes (e.g. tweaks reload)
  React.useEffect(() => { setScreen(startScreen); }, [startScreen]);

  if (screen === 'detail') {
    return (
      <div data-screen-label="Recipe detail" style={{ width: '100%', height: '100%' }}>
        <EditorialVariant tweaks={tweaks} onBack={() => setScreen('home')} />
      </div>
    );
  }
  return (
    <div data-screen-label="Home" style={{ width: '100%', height: '100%' }}>
      <EditorialHome tweaks={tweaks} onOpen={() => setScreen('detail')} />
    </div>
  );
}

Object.assign(window, { EditorialVariant, EditorialHome, EditorialApp, EditorialLibrary });
