# designer-module-system

RBAC 与系统管理（framework 扩展接口的实现放在 service 子模块）。

## 结构

```
designer-module-system/
├── system-api/       # 按需 *Api；Api 自用入参/出参类型（勿放业务 model）
└── system-service/   # ApiImpl、Service、Controller、Mapper、model、扩展实现、Job
```

其他业务模块需要用户/部门等能力时，依赖 `system-api`，并按需在 api 包声明 `XxxApi`；不要依赖 `*-service`。
