# Demo 前端模块模板

对应后端 `designer-module-demo`（`demo-api` + `demo-service`）。

- `api/index.js`：调用 `/admin-api/demo/ping`
- `views/index.vue`：占位页；菜单 `component` 填 `demo/index`

复制本目录为 `src/modules/<业务>` 即完成前端接入，无需改 `router`（已按 `modules/*/views` 自动解析）。
