import { colors, radius } from './theme.js'

/**
 * 品牌 Logo SVG（与 admin-web 同源）
 */
export function brandMarkSvg({
  fill = colors.primary,
  rx = Number.parseInt(radius, 10) || 0
} = {}) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" fill="none" role="img" aria-label="logo">
  <rect width="64" height="64" rx="${rx}" fill="${fill}"/>
  <path
    fill="#ffffff"
    fill-rule="evenodd"
    d="M20 14h14c11.5 0 20 8 20 18s-8.5 18-20 18H20V14zm9 9v18h5c6.2 0 11-4.7 11-9s-4.8-9-11-9H29z"
  />
</svg>
`
}
