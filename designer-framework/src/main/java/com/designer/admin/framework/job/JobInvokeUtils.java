package com.designer.admin.framework.job;

import com.designer.admin.common.exception.BizException;
import org.noear.solon.Solon;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 解析并调用 invoke_target：beanName.methodName(arg1, arg2, ...)
 */
public final class JobInvokeUtils {

    private JobInvokeUtils() {
    }

    public static void invokeMethod(String invokeTarget) throws Exception {
        String beanName = beanName(invokeTarget);
        String methodName = methodName(invokeTarget);
        List<Object[]> methodParams = methodParams(invokeTarget);

        Object bean = Solon.context().getBean(beanName);
        if (bean == null) {
            throw new BizException("未找到 Bean：" + beanName + "，请确认已用 @Component(\"" + beanName + "\") 注册");
        }
        invoke(bean, methodName, methodParams);
    }

    public static void validate(String invokeTarget) {
        if (invokeTarget == null || invokeTarget.isBlank()) {
            throw new BizException("调用目标不能为空");
        }
        String target = invokeTarget.trim();
        if (!target.contains(".")) {
            throw new BizException("调用目标格式错误，应为 bean.method 或 bean.method(args)");
        }
        String beanName = beanName(target);
        if (beanName.isEmpty() || methodName(target).isEmpty()) {
            throw new BizException("调用目标格式错误，应为 bean.method 或 bean.method(args)");
        }
        if (Solon.context().getBean(beanName) == null) {
            throw new BizException("未找到 Bean：" + beanName);
        }
    }

    private static void invoke(Object bean, String methodName, List<Object[]> methodParams) throws Exception {
        if (methodParams != null && !methodParams.isEmpty()) {
            Method method = bean.getClass().getDeclaredMethod(methodName, getMethodParamsType(methodParams));
            method.setAccessible(true);
            method.invoke(bean, getMethodParamsValue(methodParams));
        } else {
            Method method = bean.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(bean);
        }
    }

    private static String beanName(String invokeTarget) {
        String name = invokeTarget.substring(0, invokeTarget.indexOf('.'));
        return name.trim();
    }

    private static String methodName(String invokeTarget) {
        String method = invokeTarget.substring(invokeTarget.indexOf('.') + 1);
        int idx = method.indexOf('(');
        return (idx > 0 ? method.substring(0, idx) : method).trim();
    }

    private static List<Object[]> methodParams(String invokeTarget) {
        int start = invokeTarget.indexOf('(');
        int end = invokeTarget.lastIndexOf(')');
        if (start < 0 || end <= start + 1) {
            return null;
        }
        String params = invokeTarget.substring(start + 1, end).trim();
        if (params.isEmpty()) {
            return null;
        }
        List<Object[]> result = new LinkedList<>();
        for (String part : splitParams(params)) {
            String p = part.trim();
            if ((p.startsWith("'") && p.endsWith("'")) || (p.startsWith("\"") && p.endsWith("\""))) {
                result.add(new Object[]{p.substring(1, p.length() - 1), String.class});
            } else if ("true".equalsIgnoreCase(p) || "false".equalsIgnoreCase(p)) {
                result.add(new Object[]{Boolean.valueOf(p), Boolean.class});
            } else if (p.endsWith("L") || p.endsWith("l")) {
                result.add(new Object[]{Long.valueOf(p.substring(0, p.length() - 1)), Long.class});
            } else if (p.endsWith("D") || p.endsWith("d")) {
                result.add(new Object[]{Double.valueOf(p.substring(0, p.length() - 1)), Double.class});
            } else if (p.contains(".")) {
                result.add(new Object[]{Double.valueOf(p), Double.class});
            } else {
                result.add(new Object[]{Integer.valueOf(p), Integer.class});
            }
        }
        return result;
    }

    /** 按逗号拆分参数，忽略引号内逗号 */
    private static List<String> splitParams(String params) {
        List<String> list = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        char quote = 0;
        for (int i = 0; i < params.length(); i++) {
            char c = params.charAt(i);
            if ((c == '\'' || c == '"') && (i == 0 || params.charAt(i - 1) != '\\')) {
                if (!inQuote) {
                    inQuote = true;
                    quote = c;
                } else if (c == quote) {
                    inQuote = false;
                }
                cur.append(c);
            } else if (c == ',' && !inQuote) {
                list.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (!cur.isEmpty()) {
            list.add(cur.toString());
        }
        return list;
    }

    private static Class<?>[] getMethodParamsType(List<Object[]> methodParams) {
        Class<?>[] types = new Class<?>[methodParams.size()];
        for (int i = 0; i < methodParams.size(); i++) {
            types[i] = (Class<?>) methodParams.get(i)[1];
        }
        return types;
    }

    private static Object[] getMethodParamsValue(List<Object[]> methodParams) {
        Object[] values = new Object[methodParams.size()];
        for (int i = 0; i < methodParams.size(); i++) {
            values[i] = methodParams.get(i)[0];
        }
        return values;
    }
}
