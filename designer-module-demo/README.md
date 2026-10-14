# designer-module-demo

空示例业务模块模板。复制本目录为 `designer-module-<业务>` 即可二次开发。

## 模块结构

```
designer-module-demo/
├── demo-api/       # 对外契约：按需 *Api；Api 自用类型（勿放业务 model）
└── demo-service/   # 实现：*ApiImpl（api 包）、*Service、Controller、model
```

## 跨模块调用约定

1. 在 `*-api` 的 `...api` 包中按需新增 `XxxApi` 接口（不要把内部 `*Service` / 业务 `model` 放进 api）
2. `*Api` 入参/出参如需类型，在 `*-api` 内自行定义，不要复用 service 侧 entity/dto/vo
3. 在 `*-service` 的同名 `...api` 包中实现 `XxxApiImpl`（在实现里做 Api 类型 ↔ 内部 model 的转换）
4. 其他模块只依赖 `xxx-api`，`@Inject XxxApi`

本模块示例：[`DemoApi`](demo-api/src/main/java/com/designer/admin/module/demo/api/DemoApi.java) /
[`DemoApiImpl`](demo-service/src/main/java/com/designer/admin/module/demo/api/DemoApiImpl.java)。

## 接入步骤

1. 复制本模块，改父/`*-api`/`*-service` 的 `artifactId` 与包名
2. 根 `pom.xml` 增加父模块及 `*-api` / `*-service` 到 `dependencyManagement`
3. `designer-server` 只依赖 `*-service`
4. 前端复制 `modules/demo`；SQL 放到 `sql/init/mysql/<业务>/`

## 依赖规则

- `*-api` → `designer-common`（Api 自用类型需要时再加校验/lombok）
- `*-service` → 本模块 `*-api` + `designer-framework`
- 需要系统能力 → 依赖 `system-api`（不要依赖 system-service）
