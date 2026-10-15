# designer-module-system

RBAC 与系统管理（framework 扩展接口的实现放在 service 子模块）。

## 结构

```
designer-module-system/
├── system-api/       # 按需 *Api；Api 自用入参/出参类型（勿放业务 model）
└── system-service/   # ApiImpl、Service、Controller、Mapper、model、扩展实现、Job
```

其他业务模块需要用户/部门等能力时，依赖 `system-api`，并按需在 api 包声明 `XxxApi`；不要依赖 `*-service`。

## 登录与人机验证

登录本身在 `AuthService`，人机验证的能力**不在本模块**：滑块（AJ-Captcha）的实现与配置
都在 `designer-framework` 的 `com.designer.admin.framework.captcha`
—— 端点 `POST /captcha/get`、`POST /captcha/check` 由插件自带的 controller 注册在**根路径**下，
一个进程里只可能有一份，后台登录与博客前台评论共用它。

本模块只保留**策略**，落在 `LoginAttemptService`（原先叫 `CaptchaService`；改名是因为
画算术题那部分已整体删除，剩下的是「失败计数 + 锁定 + 要不要验」，与验证码形态无关，
不必跟着验证码实现一起搬走）：

| 关注点 | 由谁负责 |
| --- | --- |
| 该账号这次要不要验（全局开关 + 连续失败 ≥ `sys.account.captchaFailThreshold`） | `LoginAttemptService#needCaptcha` |
| 连续失败计数、达 `sys.account.lockMaxRetry` 后锁定 | `LoginAttemptService` |
| 凭据是否有效（解密、核对、一次性消费） | framework 的 `CaptchaVerifier#assertVerified` |
| 单个 **IP** 的登录尝试频次（10 次/分钟、100 次/24 小时） | `AuthController#login` 上的 `@RateLimit` |

上面三条都按**账号**计数，只能挡住盯着某一个账号试的人；换着用户名扫（撞库 / 爆破）
时每个账号只错一两次，够不到验证码阈值也触发不了锁定，所以还需要按 **IP** 记一份总额度。
阈值写在 `AuthController#login` 的 `@RateLimit` 注解上，改这里即可
（注解本身的能力见 `designer-framework` 的 `@RateLimit`）；IP 维度复用框架的
`ClientIpUtil`，与落库 IP、评论限流是同一个值。滑块凭据端点 `/captcha/**`
标注不到（第三方 controller），频控由 AJ-Captcha 内置 `req-*` 与 nginx 的 `admin_captcha` 桶承担。

⚠️ 顺序不能反：`AuthService#login` 先问 `needCaptcha`，需要验才去调 `assertVerified`。
凭据是**一次性**的，对不需要验证的账号调一次校验只会把它白白消费掉。

前端侧：`/admin-api/system/auth/captcha?username=` 只返回 `{ enabled }`（不再下发图片），
页面据此决定是否展示滑块；滑块组件与协议工具来自共享源码目录 `designer-ui/shared/captcha`
（与 blog-web 同一份，各自 alias 过去）。
