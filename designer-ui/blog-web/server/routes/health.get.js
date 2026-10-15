/**
 * 容器自检端点。
 *
 * <p>给 Docker healthcheck 用：只验证 Nuxt 进程还活着，**不碰后端**。
 * 前端能渲染静态页面并不代表后端可用，两者语义分开，
 * 否则后端抖一下就会把前端容器判定为不健康并重启。</p>
 */
export default defineEventHandler(() => 'ok\n')
