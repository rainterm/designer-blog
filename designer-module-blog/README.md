# designer-module-blog

博客业务模块。

## 结构

```
designer-module-blog/
├── blog-api/       # 按需 *Api；Api 自用入参/出参类型（勿放业务 model）
└── blog-service/   # ApiImpl、Service、Controller、Mapper、model
```

`designer-server` 依赖 `blog-service`。跨模块调用时只依赖 `blog-api`，并按需新增 `...api.XxxApi`。

初始化 SQL：`sql/init/mysql/blog/`。
