package com.designer.admin.framework.config;

import com.designer.admin.common.core.R;
import com.designer.admin.common.core.ResultCode;
import com.designer.admin.common.exception.BizException;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;
import org.noear.solon.validation.ValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局异常过滤器：把 Controller 抛出的异常统一转成 R 结构（HTTP 200 + 业务码）。
 */
@Component(index = -200)
public class GlobalExceptionFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionFilter.class);

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        try {
            chain.doFilter(ctx);
        } catch (BizException e) {
            write(ctx, R.fail(e.getCode(), e.getMessage()));
        } catch (ValidatorException e) {
            write(ctx, R.fail(ResultCode.BAD_REQUEST, "参数校验失败：" + e.getMessage()));
        } catch (Throwable e) {
            // 登录/权限异常已由 Sa-Token 拦截器处理，这里兜底其余未知异常。
            // 详情只进日志：把 e.getMessage() 回传会把 SQL 片段、类名、路径等内部信息泄露给匿名调用方。
            log.error("未处理异常 {} {}", ctx.method(), ctx.path(), e);
            write(ctx, R.fail(ResultCode.ERROR, "系统繁忙，请稍后重试"));
        }
    }

    private void write(Context ctx, R<?> body) {
        ctx.setRendered(false);
        ctx.contentType("application/json;charset=utf-8");
        ctx.output(com.designer.admin.common.util.JsonUtil.toJson(body));
        ctx.setRendered(true);
    }
}
