-- =====================================================================
--  blog/schema.sql
-- =====================================================================

SET NAMES utf8mb4;
USE `designer_blog`;

DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '分类名称',
    `slug`        VARCHAR(80)  NOT NULL                COMMENT 'URL 标识（唯一）',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序，越小越靠前',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
    `remark`      VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_category_slug` (`slug`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '博客分类';

DROP TABLE IF EXISTS `blog_tag`;
CREATE TABLE `blog_tag` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '标签名称',
    `slug`        VARCHAR(80)  NOT NULL                COMMENT 'URL 标识（唯一）',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_tag_slug` (`slug`, (IF(`deleted` = 0, 0, NULL))),
    UNIQUE KEY `uk_blog_tag_name` (`name`, (IF(`deleted` = 0, 0, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '博客标签';

DROP TABLE IF EXISTS `blog_post`;
CREATE TABLE `blog_post` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `title`          VARCHAR(200)  NOT NULL                COMMENT '标题',
    `slug`           VARCHAR(120)  NOT NULL                COMMENT 'URL 标识（唯一）',
    `summary`        VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '摘要',
    `content`        MEDIUMTEXT    NOT NULL                COMMENT '正文（Markdown）',
    `cover_url`      VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '封面图 URL',
    `category_id`    BIGINT        DEFAULT NULL            COMMENT '分类ID',
    `author_id`      BIGINT        DEFAULT NULL            COMMENT '作者用户ID',
    `author_name`    VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '作者展示名快照',
    `status`         TINYINT       NOT NULL DEFAULT 0      COMMENT '状态：0草稿 1已发布',
    `is_top`         TINYINT       NOT NULL DEFAULT 0      COMMENT '是否置顶：1是 0否',
    `allow_comment`  TINYINT       NOT NULL DEFAULT 1      COMMENT '是否允许评论：1是 0否',
    `view_count`     INT           NOT NULL DEFAULT 0      COMMENT '浏览量',
    `seo_title`      VARCHAR(200)  NOT NULL DEFAULT ''     COMMENT 'SEO 标题',
    `seo_keywords`   VARCHAR(255)  NOT NULL DEFAULT ''     COMMENT 'SEO 关键词',
    `seo_description` VARCHAR(500) NOT NULL DEFAULT ''     COMMENT 'SEO 描述',
    `published_at`   DATETIME      DEFAULT NULL            COMMENT '发布时间',
    `create_by`      VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`    DATETIME      DEFAULT NULL            COMMENT '创建时间',
    `update_by`      VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`    DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_post_slug` (`slug`, (IF(`deleted` = 0, 0, NULL))),
    KEY `idx_blog_post_category` (`category_id`),
    KEY `idx_blog_post_status` (`status`, `published_at`),
    KEY `idx_blog_post_author` (`author_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '博客文章';

DROP TABLE IF EXISTS `blog_post_tag`;
CREATE TABLE `blog_post_tag` (
    `post_id` BIGINT NOT NULL COMMENT '文章ID',
    `tag_id`  BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (`post_id`, `tag_id`),
    KEY `idx_blog_post_tag_tag` (`tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文章标签关联';

DROP TABLE IF EXISTS `blog_link`;
CREATE TABLE `blog_link` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '链接ID',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '链接名称，如 GitHub',
    `url`         VARCHAR(500) NOT NULL                COMMENT '跳转地址（http/https）',
    `icon`        VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '图标标识，如 github/gitee/link',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序，越小越靠前',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
    `remark`      VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '备注',
    `create_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_blog_link_status_sort` (`status`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '博客链接（社交/外链）';

DROP TABLE IF EXISTS `blog_setting`;
CREATE TABLE `blog_setting` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键（固定单行 id=1）',
    `site_name`       VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '站点名称',
    `site_subtitle`   VARCHAR(200) NOT NULL DEFAULT ''     COMMENT '站点副标题（顶栏展示 / SEO 描述兜底）',
    `logo_url`        VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '站点 Logo 地址，留空用内置 Logo',
    `logo_radius`     INT          NOT NULL DEFAULT 6      COMMENT 'Logo 圆角（px，0 为直角）',
    `theme_color`     VARCHAR(16)  NOT NULL DEFAULT '#16A34A' COMMENT '站点主题色（十六进制），前台据此覆盖品牌配色',
    `copyright`       VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '页脚版权',
    `icp`             VARCHAR(100) NOT NULL DEFAULT ''     COMMENT 'ICP 备案号',
    `icp_url`         VARCHAR(500) NOT NULL DEFAULT ''     COMMENT 'ICP 备案号链接，如 https://beian.miit.gov.cn/',
    `seo_keywords`    VARCHAR(255) NOT NULL DEFAULT ''     COMMENT 'SEO 默认关键字（逗号分隔）',
    `seo_description` VARCHAR(500) NOT NULL DEFAULT ''     COMMENT 'SEO 默认描述',
    `comment_enabled` TINYINT      NOT NULL DEFAULT 1      COMMENT '全局评论总开关：1 开启，0 关闭（单篇仍受 blog_post.allow_comment 约束）',
    `owner_nickname`  VARCHAR(50)  NOT NULL DEFAULT '博主' COMMENT '站长评论署名（后台回复默认署名，并自动加入保留昵称）',
    `reserved_nicknames` VARCHAR(500) NOT NULL DEFAULT '博主,站长,管理员,admin' COMMENT '保留昵称（逗号分隔）：访客昵称包含其中任一词即拒绝，后台可配',
    `create_by`       VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`     DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`       VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`     DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '博客站点设置（单行）';

DROP TABLE IF EXISTS `blog_comment`;
CREATE TABLE `blog_comment` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `post_id`       BIGINT        NOT NULL                COMMENT '文章ID',
    `parent_id`     BIGINT        NOT NULL DEFAULT 0      COMMENT '父评论ID，0为顶级',
    `nickname`      VARCHAR(50)   NOT NULL                COMMENT '昵称',
    `author_type`   TINYINT       NOT NULL DEFAULT 0      COMMENT '作者类型：0访客 1博主（仅服务端写入，前端据此展示「博主」标识，防止冒充）',
    `email`         VARCHAR(100)  NOT NULL DEFAULT ''     COMMENT '邮箱（可选）',
    `content`       VARCHAR(2000) NOT NULL                COMMENT '评论内容',
    `ip`            VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT 'IP',
    `ip_location`   VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT 'IP归属地（省级，如「广东」；内网为「内网」）',
    `commenter_key` VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '评论人匿名标识（客户端密钥的 SHA-256，用于本人查看/删除）',
    `status`        TINYINT       NOT NULL DEFAULT 0      COMMENT '状态：0待审 1通过 2拒绝',
    `is_top`        TINYINT       NOT NULL DEFAULT 0      COMMENT '是否置顶：0否 1是（只影响展示顺序，不改状态；排序在应用层做，故无需索引）',
    `create_by`     VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`   DATETIME      DEFAULT NULL            COMMENT '创建时间',
    `update_by`     VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`   DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `deleted`       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_blog_comment_post` (`post_id`, `status`),
    KEY `idx_blog_comment_parent` (`parent_id`),
    KEY `idx_blog_comment_owner` (`post_id`, `commenter_key`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '博客评论';

-- ------------------------- 访问日志 -------------------------
-- 每次来访写一条明细，后台「访问统计」按天/维度聚合查询。
-- 表会持续增长，配套内置定时任务 blogLogTask.cleanVisitLog(days) 做保留期清理。
DROP TABLE IF EXISTS `blog_visit_log`;
CREATE TABLE `blog_visit_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `visit_date`   DATE         NOT NULL                COMMENT '访问日期（冗余列：按天聚合走索引，不从 create_time 现算）',
    `page_type`    VARCHAR(16)  NOT NULL DEFAULT ''     COMMENT '页面类型：home首页 post详情 tag标签 category分类 other其他',
    `path`         VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '访问路径（已去 query 与锚点）',
    `post_id`      BIGINT       DEFAULT NULL            COMMENT '文章ID（仅详情页，按 slug 反查，不信任前端上报）',
    `category_id`  BIGINT       DEFAULT NULL            COMMENT '文章分类ID（仅详情页，用于分类访问分布）',
    `ip`           VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '客户端IP',
    `ip_location`  VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT 'IP归属地（省级，如「广东」；内网为「内网」）',
    `visitor_key`  VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '访客标识（IP+UA 的 SHA-256，用于 UV 去重）',
    `device`       VARCHAR(16)  NOT NULL DEFAULT ''     COMMENT '设备：PC MOBILE TABLET OTHER',
    `referer_host` VARCHAR(128) NOT NULL DEFAULT ''     COMMENT '来源域名，直接访问 / 站内跳转为空串',
    `referer_type` VARCHAR(16)  NOT NULL DEFAULT ''     COMMENT '来源类型：direct直接访问 search搜索引擎 external外链',
    `ua`           VARCHAR(255) NOT NULL DEFAULT ''     COMMENT 'User-Agent（截断 255，便于排查）',
    `create_by`    VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`  DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`    VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`  DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_blog_visit_date` (`visit_date`),
    KEY `idx_blog_visit_visitor` (`visit_date`, `visitor_key`),
    KEY `idx_blog_visit_post` (`post_id`, `visit_date`),
    KEY `idx_blog_visit_category` (`category_id`, `visit_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '博客访问日志';

