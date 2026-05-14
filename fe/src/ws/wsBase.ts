const runtimeOrigin = typeof window !== 'undefined' ? window.location.origin : '';

const normalizeBase = (base: string) => {
  const trimmed = base.endsWith('/') ? base.slice(0, -1) : base;
  if (trimmed.startsWith('http://')) {
    return `ws://${trimmed.slice('http://'.length)}`;
  }
  if (trimmed.startsWith('https://')) {
    return `wss://${trimmed.slice('https://'.length)}`;
  }
  return trimmed;
};

export const getWebSocketBase = () => {
  const envBase = import.meta.env.VITE_WS_BASE || import.meta.env.VITE_API_BASE || runtimeOrigin;
  if (!envBase) return '';
  return normalizeBase(envBase);
};
