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

## 密码策略

规则（下限 6、上限 50，**且需同时包含大写字母、小写字母、数字、特殊符号**）集中在 `designer-common` 的
`com.designer.admin.common.constant.PasswordPolicy`，各入口引的是同一份：

| 入口 | 校验位置 | 长度 | 复杂度 |
| --- | --- | --- | --- |
| 登录 | `LoginDTO#password` | ✅ | ❌ 刻意不验 |
| 新增 / 修改用户 | `UserSaveDTO#password`、`SysUserService#save/#update` | ✅ | ✅ |
| 重置密码 | `ResetPwdDTO#password`、`SysUserService#resetPassword` | ✅ | ✅ |
| 个人改密 | `ProfileController#changePassword`、`SysUserService#changePassword` | ✅ | ✅ |

**复杂度为什么只在写入侧校验**：它是「新设密码」的要求。若登录侧也验，历史上那些纯字母 /
纯数字的老密码即使输对了也登不进来，账号会被直接锁死。登录只回答「输入形态是否合理」，
所以只卡长度（顺带让超短密码在一次 BCrypt 比对之前就被拒掉）。
初始化脚本里的 `admin123` / `guest123` 正是这种情况：**登录不受影响，但用「重置密码」
把它们再设一遍会被规则打回**，这一点在自测时容易被当成 bug。

「特殊符号」取的是 ASCII 可见标点（码点 0x21–0x2F、0x3A–0x40、0x5B–0x60、0x7B–0x7E 四段，
即 POSIX 的 `[:punct:]`），**有意排除空白与中文等非 ASCII 字符** —— 否则一个汉字或一个空格
就能凑满「特殊符号」，规则形同虚设。正则在 `PasswordPolicy.STRENGTH_REGEXP`，前端镜像在
`designer-ui/admin-web/src/constants/password.js`，改一边必须同步另一边。

两条只在 service 层能做的校验（需要读库拿哈希）：

- **个人改密**：新密码不能与原密码相同（原密码已验证过，直接比字符串）；
- **重置密码**：新密码不能与该用户当前密码相同 —— 「重置」却写回原密码等于什么都没做。

服务层的兜底校验不是重复劳动：Excel 导入完全绕过 DTO，将来别的模块也可能直接调服务。
**只有写入侧确实卡住了，登录侧才敢卡下限** —— 否则会产出「库里躺着 3 位密码、
却永远登不进去」的账号。前端是同一份规则的镜像：
`designer-ui/admin-web/src/constants/password.js`（含提示文案与复数比较规则）。

⚠️ **Excel 导入的密码是必填项**（`SysUserService#importRows`）：留空不再兜底成默认口令，
而是直接报 `第 N 行密码不能为空`。原因是默认口令人人皆知、导入又是批量操作，
一旦被发现就得逐个改。导出文件里密码列固定为空（避免泄露哈希），
所以**「导出 → 原样导入」会失败**，需要在导入前逐行填上密码。

## 入参校验：@Body 参数必须带 @Validated

⚠️ **Controller 类上的 `@Valid` 只校验「方法参数上直接标注的注解」，它不会去校验
`@Body` DTO 的字段。** 想让你写在 DTO 字段上的 `@NotBlank` / `@Length` 生效，
必须给该参数再补一个 `@Validated`：

```java
public R<Void> add(@Body @Validated UserSaveDTO dto) { ... }   // ✅ DTO 字段注解生效
public R<Void> add(@Body UserSaveDTO dto) { ... }              // ❌ DTO 字段注解被忽略
```

原因在 Solon 的实现里：`@Valid` 被注册成 `BeanValidateInterceptor`，
它调用的 `ValidUtils.validateInvocation` 只遍历**方法参数上的注解**；
唯一会反射校验实体字段的 `ValidUtils.validateEntity`，
只能由参数上的 `@Validated`（`ValidatedValidator` → `BeanValidatorDefault`）触发。

因此本项目约定：**所有 `@Body` 的参数都补 `@Validated`**，
这样「DTO 上声明了什么，接口就只接受什么」才成立。
`@Body List<...>` / `@Body Map<...>` 这类非 DTO 的裸集合不补（没有字段注解可校验）。

另有两个易踩的点：

- **顺序**：`@Validated` 是 index=1 的 bean 拦截器，而 `@RateLimit` 是 `@Around(index = -1)`，
  限流在**外层**。所以被校验打回的请求**已经**消耗了一次限流额度。
- **空值语义**：`@Length` / `@Pattern` 这类校验遇到空值直接放行（交由 `@NotBlank` / `@NotNull` 负责），
  正好对上「修改时留空表示不修改」；反过来，**任何允许留空的字段都不要加 `@NotBlank`**
  —— 存储配置的 `urlPrefix` 就是这种字段（留空表示沿用服务端默认值）。
- **不要用 `n-form-item` 的 `#feedback` 插槽写提示**：naive-ui 一旦拿到该插槽（或 `feedback` 属性）
  就不再渲染校验错误（见 `FormItem.mjs` 的 `resolveWrappedSlot` 分支），提示与报错会二选一。
  静态提示请写在默认插槽里（输入框后面）。
