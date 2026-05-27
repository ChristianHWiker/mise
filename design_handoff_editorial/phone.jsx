// lib/phone.jsx — minimal Android device shell.
// Lets each variation pick its own background. Uses AndroidStatusBar +
// AndroidNavBar from android-frame.jsx for the system chrome.

function Phone({ children, dark = false, bg = '#fff', frameColor, width = 420, height = 860 }) {
  return (
    <div style={{
      width, height,
      borderRadius: 28, overflow: 'hidden',
      background: bg,
      border: `8px solid ${frameColor || (dark ? '#2a2a2a' : '#bdbdbd')}`,
      boxShadow: '0 30px 80px rgba(0,0,0,0.25)',
      display: 'flex', flexDirection: 'column', boxSizing: 'border-box',
      position: 'relative',
    }}>
      <AndroidStatusBar dark={dark} />
      <div style={{
        flex: 1, minHeight: 0, position: 'relative', overflow: 'hidden',
        display: 'flex', flexDirection: 'column',
      }}>
        {children}
      </div>
      <AndroidNavBar dark={dark} />
    </div>
  );
}

// SVG photo placeholder — striped, with a mono caption.
// Each instance gets a unique pattern id so they don't collide.
let __photoId = 0;
function PhotoPlaceholder({ tone = '#888', captionTone = 'rgba(255,255,255,0.7)', label = 'recipe photo', style = {} }) {
  const id = React.useMemo(() => `phpat-${++__photoId}`, []);
  return (
    <div style={{
      position: 'absolute', inset: 0,
      display: 'flex', alignItems: 'flex-end', justifyContent: 'flex-start',
      ...style,
    }}>
      <svg width="100%" height="100%" style={{ position: 'absolute', inset: 0, display: 'block' }}>
        <defs>
          <pattern id={id} width="8" height="8" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
            <line x1="0" y1="0" x2="0" y2="8" stroke={tone} strokeWidth="1" opacity="0.4" />
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill={`url(#${id})`} />
      </svg>
      <div style={{
        position: 'relative', padding: '10px 12px',
        fontFamily: '"Geist Mono", ui-monospace, monospace',
        fontSize: 10, letterSpacing: '0.06em', textTransform: 'uppercase',
        color: captionTone,
      }}>
        {label}
      </div>
    </div>
  );
}

Object.assign(window, { Phone, PhotoPlaceholder });
