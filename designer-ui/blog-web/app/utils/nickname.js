/**
 * 评论昵称的归一化与「保留昵称」比对。
 *
 * <p>与后端 `NicknameUtil` 保持同一套规则：NFKC 折叠全角/兼容字符、去掉零宽与控制字符、
 * 去掉**全部**空白、转小写后再比较。所以「admin」「ADMIN」「ａｄｍｉｎ」「a d m i n」
 * 归一后都是 `admin`，大小写与全角/半角不影响判断。</p>
 *
 * <p><b>匹配是「包含」而不是「等于」</b>：昵称里只要出现任意一个保留词就判为命中。
 * 这样「博主小号」「我是站长」「admin01」都会被拦下，而不是只有一模一样才拦。
 * 规则与后端 `NicknameUtil.firstMatch` 一致 —— 规则只在后端改、前端还按相等判断，
 * 就会出现「前端放行、提交后被拒」，所以两边要一起改。</p>
 *
 * <p><b>名单不在这里维护</b>：完整的保留昵称由后端通过 `/site` 接口下发
 * （`site.reservedNicknames`，来自后台「保留昵称」配置 + 站长署名），
 * 与后端实际拦截用的是同一份（`NicknameUtil.reserved`）。前端只负责比对。</p>
 *
 * <p>⚠️ 这里只是提交前的即时提示，真正的拦截在服务端；异体字等形近变体挡不住，
 * 身份鉴别由后端 `author_type` 决定（展示层认标记不认昵称）。</p>
 */

/**
 * 兜底名单：仅在 `/site` 请求失败、后端未下发时使用。
 *
 * <p>与后端 `NicknameUtil.DEFAULT_RESERVED` 保持一致，但**不要**把它当成权威来源 ——
 * 要改保留词请到后台「站点设置 → 保留昵称」。前端这份只影响提示时机，不影响安全性
 * （后端仍会拦）。</p>
 */
const FALLBACK_RESERVED = ['博主', '站长', '管理员', 'admin']

/** 归一化后的比对键：去空白、去不可见字符、忽略大小写 */
export function nicknameKey(raw) {
  return String(raw ?? '')
    .normalize('NFKC')
    .replace(/[\u200B-\u200D\u200E\u200F\u2060\uFEFF\u00AD]/g, '')
    .replace(/[\u0000-\u001F\u007F]/g, '')
    .replace(/\s+/g, '')
    .toLowerCase()
}

/**
 * 昵称命中的保留词。
 *
 * @param raw      访客填写的原始昵称
 * @param reserved 后端下发的保留昵称名单（`site.reservedNicknames`）；为空时用兜底名单
 * @returns 命中的保留词（原样），未命中返回 `null`
 */
export function matchReservedNickname(raw, reserved) {
  const key = nicknameKey(raw)
  if (!key) return null
  const list = Array.isArray(reserved) && reserved.length ? reserved : FALLBACK_RESERVED
  return list.find((name) => {
    const reservedKey = nicknameKey(name)
    return reservedKey !== '' && key.includes(reservedKey)
  }) ?? null
}

/** 昵称是否命中保留名单（`matchReservedNickname` 的布尔便捷写法） */
export function containsReservedNickname(raw, reserved) {
  return matchReservedNickname(raw, reserved) !== null
}
