-- =====================================================================
--  blog/data.sql（菜单 ID 8000 段）
-- =====================================================================

SET NAMES utf8mb4;
USE `designer_blog`;

-- ------------------------- 菜单 -------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(8000, 0,    '博客模块', 'M', '/blog',              '',                    '',                   'book',     80, 1, 1, 'system', NOW(), 'system', NOW()),
(8008, 8000, '访问统计', 'C', '/blog/visit',        'blog/visit/index',    'blog:visit:list',    'trending',  1, 1, 1, 'system', NOW(), 'system', NOW()),
(8001, 8000, '文章管理', 'C', '/blog/post',         'blog/post/index',     'blog:post:list',     'document',  2, 1, 1, 'system', NOW(), 'system', NOW()),
(8002, 8000, '分类管理', 'C', '/blog/category',     'blog/category/index', 'blog:category:list', 'folder',    3, 1, 1, 'system', NOW(), 'system', NOW()),
(8003, 8000, '标签管理', 'C', '/blog/tag',          'blog/tag/index',      'blog:tag:list',      'pricetag',  4, 1, 1, 'system', NOW(), 'system', NOW()),
(8004, 8000, '评论管理', 'C', '/blog/comment',      'blog/comment/index',  'blog:comment:list',  'chatbubbles', 5, 1, 1, 'system', NOW(), 'system', NOW()),
(8006, 8000, '链接管理', 'C', '/blog/link',         'blog/link/index',     'blog:link:list',     'link',      6, 1, 1, 'system', NOW(), 'system', NOW()),
(8007, 8000, '站点设置', 'C', '/blog/setting',      'blog/setting/index',  'blog:setting:query', 'settings',  7, 1, 1, 'system', NOW(), 'system', NOW()),
-- 文章按钮
(8010, 8001, '文章查询', 'F', '', '', 'blog:post:query',   '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(8011, 8001, '文章新增', 'F', '', '', 'blog:post:add',     '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(8012, 8001, '文章编辑', 'F', '', '', 'blog:post:edit',    '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(8013, 8001, '文章删除', 'F', '', '', 'blog:post:remove',  '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
(8014, 8001, '文章发布', 'F', '', '', 'blog:post:publish', '', 5, 1, 1, 'system', NOW(), 'system', NOW()),
-- 分类按钮
(8020, 8002, '分类查询', 'F', '', '', 'blog:category:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(8021, 8002, '分类新增', 'F', '', '', 'blog:category:add',    '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(8022, 8002, '分类编辑', 'F', '', '', 'blog:category:edit',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(8023, 8002, '分类删除', 'F', '', '', 'blog:category:remove', '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
-- 标签按钮
(8030, 8003, '标签查询', 'F', '', '', 'blog:tag:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(8031, 8003, '标签新增', 'F', '', '', 'blog:tag:add',    '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(8032, 8003, '标签编辑', 'F', '', '', 'blog:tag:edit',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(8033, 8003, '标签删除', 'F', '', '', 'blog:tag:remove', '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
-- 评论按钮
(8040, 8004, '评论查询', 'F', '', '', 'blog:comment:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(8041, 8004, '评论审核', 'F', '', '', 'blog:comment:audit',  '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(8042, 8004, '评论删除', 'F', '', '', 'blog:comment:remove', '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(8043, 8004, '评论回复', 'F', '', '', 'blog:comment:reply',  '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
-- 链接按钮
(8050, 8006, '链接查询', 'F', '', '', 'blog:link:query',  '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(8051, 8006, '链接新增', 'F', '', '', 'blog:link:add',    '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
(8052, 8006, '链接编辑', 'F', '', '', 'blog:link:edit',   '', 3, 1, 1, 'system', NOW(), 'system', NOW()),
(8053, 8006, '链接删除', 'F', '', '', 'blog:link:remove', '', 4, 1, 1, 'system', NOW(), 'system', NOW()),
-- 站点设置按钮
(8060, 8007, '站点查询', 'F', '', '', 'blog:setting:query', '', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(8061, 8007, '站点保存', 'F', '', '', 'blog:setting:edit',  '', 2, 1, 1, 'system', NOW(), 'system', NOW()),
-- 访问统计按钮
(8062, 8008, '访问统计查询', 'F', '', '', 'blog:visit:query', '', 1, 1, 1, 'system', NOW(), 'system', NOW());

-- 文章编辑页已改为独立全屏页（前端常量路由 /blog/post/edit，列表页新标签打开），
-- 不再经后端菜单注册，故无需隐藏菜单行；权限由前端 meta.authPerm + 后端接口校验。

-- ------------------------- 定时任务 -------------------------
-- 访问日志是明细表，会随流量持续增长；注册内置任务做保留期清理（默认暂停，按需开启）。
INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(4, '清理访问日志', 'SYSTEM', 'blogLogTask.cleanVisitLog(90)', '0 30 3 * * ?', 1, 0, 0, '内置：每天 3:30 清理 90 天前的博客访问日志（默认暂停）', 'system', NOW(), 'system', NOW());

-- ------------------------- 种子数据 -------------------------
INSERT INTO `blog_category` (`id`, `name`, `slug`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '技术', 'tech',     1, 1, '技术相关', 'system', NOW(), 'system', NOW()),
(2, '生活', 'life',     2, 1, '生活随笔', 'system', NOW(), 'system', NOW()),
(3, '随笔', 'essay',    3, 1, '随想杂记', 'system', NOW(), 'system', NOW());

INSERT INTO `blog_tag` (`id`, `name`, `slug`, `status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 'Java',   'java',   1, 'system', NOW(), 'system', NOW()),
(2, 'Vue',    'vue',    1, 'system', NOW(), 'system', NOW()),
(3, '博客',   'blog',   1, 'system', NOW(), 'system', NOW());

INSERT INTO `blog_post` (`id`, `title`, `slug`, `summary`, `content`, `cover_url`, `category_id`, `author_id`, `author_name`, `status`, `is_top`, `allow_comment`, `view_count`, `seo_title`, `seo_keywords`, `seo_description`, `published_at`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '欢迎使用博客系统', 'welcome-to-blog',
 '这是一篇示例文章，介绍博客模块的基本能力。',
 '# 欢迎使用博客系统\n\n本博客已接入 Designer Admin，支持：\n\n- 文章草稿 / 发布\n- 分类与标签\n- 评论审核\n- Markdown 正文\n\n开始写下你的第一篇文章吧。\n',
 '', 1, 1, '管理员', 1, 1, 1, 0,
 '欢迎使用博客系统', '博客,Designer Admin', '博客模块示例文章',
 NOW(), 'system', NOW(), 'system', NOW(), 0),
(2, 'Markdown 写作指南', 'markdown-guide',
 '快速了解本站文章正文的 Markdown 写法。',
 '# Markdown 写作指南\n\n## 标题\n\n使用 `#` 创建标题。\n\n## 代码\n\n```js\nconsole.log(\"Hello Blog\")\n```\n\n## 列表\n\n- 条目一\n- 条目二\n\n祝写作愉快！\n',
 '', 1, 1, '管理员', 1, 0, 1, 0,
 'Markdown 写作指南', 'Markdown,写作', 'Markdown 写作示例',
 NOW(), 'system', NOW(), 'system', NOW(), 0);

INSERT INTO `blog_post_tag` (`post_id`, `tag_id`) VALUES
(1, 3), (2, 2), (2, 3);

-- ------------------------- 示例评论 -------------------------
-- 演示用的评论种子，覆盖后台「评论管理」的几个关键场景：
--   - 已通过 / 待审两种状态（待审的用于试「审核通过 / 拒绝」）
--   - 显示层压平为 2 层：顶级评论 + 其下全部回复，并含「在第 2 层继续回复」的深链
--   - 管理员回复（不记 IP / 归属地，status 直接为 1；署名「博主」与自定义「站长」）
--   - 不同 IP 归属地
--
-- 时间用 DATE_SUB(NOW(), ...) 相对生成，所以无论何时初始化，看起来都是「最近几天」的评论。
-- ID 从 101 起是为了与真实评论区分开：不需要时一条语句即可清空。
--   DELETE FROM `blog_comment` WHERE `id` BETWEEN 101 AND 110;
-- author_type：0 访客，1 博主。管理员回复（空 ip）标 1，署名统一取「站点设置 → 站长署名」。
INSERT INTO `blog_comment`
(`id`, `post_id`, `parent_id`, `nickname`, `author_type`, `email`, `content`, `ip`, `ip_location`, `commenter_key`, `status`, `create_by`, `create_time`, `update_by`, `update_time`)
VALUES
-- ---------- 文章 2《Markdown 写作指南》：一条「回复的回复」深链（显示时压平为 2 层） ----------
(101, 2, 0,   '张三', 0, 'zhangsan@example.com', '代码块的写法很清晰，收藏了。',                   '114.114.114.114', '江苏', '', 1, 'system', DATE_SUB(NOW(), INTERVAL 3 DAY), 'system', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(102, 2, 101, '李四', 0, '',                     '同感，另外想问下支持表格吗？',                   '202.96.128.86',   '广东', '', 1, 'system', DATE_SUB(NOW(), INTERVAL 2 DAY), 'system', DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- 管理员回复（回复 #102 这条第 2 层回复，显示时仍在第 2 层）
(103, 2, 102, '博主', 1, '',                     '支持的，标准 Markdown 表格语法即可，后面我会补一篇详细说明。', '', '', '', 1, 'system', DATE_SUB(NOW(), INTERVAL 2 DAY), 'system', DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- 同一顶级下的另一条管理员回复（第 2 层）
(107, 2, 101, '博主', 1, '',                     '谢谢！后面还会写一篇代码块高亮的配置说明。',       '', '', '', 1, 'system', DATE_SUB(NOW(), INTERVAL 1 DAY), 'system', DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 在第 2 层继续回复（parentId 指向 103 这条回复）：显示时仍压平在第 2 层，标题为「王五 回复 博主」
(110, 2, 103, '王五', 0, '',                     '等这篇！高亮主题能自定义吗？',                   '223.5.5.5',       '浙江', '', 1, 'system', DATE_SUB(NOW(), INTERVAL 6 HOUR), 'system', DATE_SUB(NOW(), INTERVAL 6 HOUR)),

-- ---------- 文章 2：一条待审评论（用于测试审核通过 / 拒绝） ----------
(104, 2, 0,   '王五', 0, '',                     '标题层级那部分能再展开讲讲吗？',                 '223.5.5.5',       '浙江', '', 0, 'system', DATE_SUB(NOW(), INTERVAL 5 HOUR), 'system', DATE_SUB(NOW(), INTERVAL 5 HOUR)),

-- ---------- 文章 1《欢迎使用博客系统》 ----------
(105, 1, 0,   '小明', 0, 'xiaoming@example.com', '博客做得不错，界面很干净！',                     '1.2.4.8',         '北京', '', 1, 'system', DATE_SUB(NOW(), INTERVAL 1 DAY), 'system', DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 管理员回复（署名统一取站点设置，这里与默认的「博主」一致）
(108, 1, 105, '博主', 1, '',                     '谢谢支持！有问题随时留言。',                     '', '', '', 1, 'system', DATE_SUB(NOW(), INTERVAL 20 HOUR), 'system', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(109, 1, 108, '博主', 1, '',                     'RSS 订阅在规划中了，预计下周上线。',              '', '', '', 1, 'system', DATE_SUB(NOW(), INTERVAL 19 HOUR), 'system', DATE_SUB(NOW(), INTERVAL 19 HOUR)),

-- ---------- 文章 1：一条待审评论 ----------
(106, 1, 0,   '路人甲', 0, '',                   '第一次来，请问有没有 RSS 订阅？',                '101.226.4.6',     '上海', '', 0, 'system', DATE_SUB(NOW(), INTERVAL 20 MINUTE), 'system', DATE_SUB(NOW(), INTERVAL 20 MINUTE));

INSERT INTO `blog_link` (`id`, `name`, `url`, `icon`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 'GitHub', 'https://github.com/rainterm', 'github', 1, 1, '代码托管', 'system', NOW(), 'system', NOW()),
(2, 'Gitee',  'https://gitee.com/rainterm',  'gitee',  2, 1, '代码托管', 'system', NOW(), 'system', NOW());

-- ------------------------- 站点设置（单行，id=1）-------------------------
INSERT INTO `blog_setting` (`id`, `site_name`, `site_subtitle`, `logo_url`, `logo_radius`, `copyright`, `icp`, `icp_url`, `seo_keywords`, `seo_description`, `comment_enabled`, `owner_nickname`, `reserved_nicknames`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 'Designer Blog', '记录技术与生活', '', 6, '© 2026 Designer. All rights reserved.', '粤ICP备XXXXXX号', 'https://beian.miit.gov.cn/', 'Designer,个人博客,技术与生活', 'Designer 的个人博客，分享技术与生活。', 1, '博主', '博主,站长,管理员,admin', 'system', NOW(), 'system', NOW());

