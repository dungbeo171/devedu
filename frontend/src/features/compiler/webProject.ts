export const defaultWebStyles = `body {
    font-family: system-ui, sans-serif;
    padding: 32px;
    color: #172554;
    background: #eff6ff;
}

button {
    padding: 10px 18px;
    border: 0;
    border-radius: 6px;
    background: #0d6efd;
    color: white;
    cursor: pointer;
}`

export const defaultWebScript = `document.querySelector('button').addEventListener('click', () => {
    document.querySelector('h1').textContent = 'Xin chào DevEdu!';
});`

function dataUrl(type: string, content: string) {
  const bytes = new TextEncoder().encode(content)
  return `data:${type};base64,${btoa(Array.from(bytes, (byte) => String.fromCharCode(byte)).join(''))}`
}

// Resolve only the project's local assets. The preview iframe blocks external requests.
export function buildWebPreview(html: string, files: Record<string, string>): string {
  return html
    .replace(/(<link\b[^>]*?\bhref\s*=\s*)(["'])(?:\.\/)?style\.css\2/gi,
      (_match, prefix: string, quote: string) => `${prefix}${quote}${dataUrl('text/css', files['style.css'] ?? '')}${quote}`)
    .replace(/(<script\b[^>]*?\bsrc\s*=\s*)(["'])(?:\.\/)?script\.js\2/gi,
      (_match, prefix: string, quote: string) => `${prefix}${quote}${dataUrl('text/javascript', files['script.js'] ?? '')}${quote}`)
}
