package com.designer.admin.framework.message.provider;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.message.MessageChannelProvider;
import com.designer.admin.framework.message.MessageFieldMeta;
import com.designer.admin.framework.message.MessagePayload;
import com.designer.admin.framework.message.MessageRuntimeConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.noear.solon.annotation.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件通道（SMTP）
 */
@Component
public class EmailMessageChannelProvider implements MessageChannelProvider {

    public static final String TYPE = "email";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String label() {
        return "邮件";
    }

    @Override
    public List<MessageFieldMeta> configFields() {
        return List.of(
                MessageFieldMeta.builder().name("host").label("SMTP Host").inputType("text").required(true)
                        .placeholder("smtp.example.com").build(),
                MessageFieldMeta.builder().name("port").label("端口").inputType("number").required(true)
                        .defaultValue("587").placeholder("587").help("常用：587(STARTTLS) / 465(SSL) / 25").build(),
                MessageFieldMeta.builder().name("username").label("用户名").inputType("text").required(true)
                        .placeholder("账号或完整邮箱").build(),
                MessageFieldMeta.builder().name("password").label("密码/授权码").inputType("password").required(true)
                        .placeholder("SMTP 密码或授权码").build(),
                MessageFieldMeta.builder().name("from").label("发件人邮箱").inputType("text").required(true)
                        .placeholder("noreply@example.com").build(),
                MessageFieldMeta.builder().name("fromName").label("发件人名称").inputType("text").required(false)
                        .placeholder("系统通知").defaultValue("系统通知").build(),
                MessageFieldMeta.builder().name("auth").label("启用认证").inputType("switch").required(false)
                        .defaultValue("true").build(),
                MessageFieldMeta.builder().name("starttls").label("STARTTLS").inputType("switch").required(false)
                        .defaultValue("true").help("端口 587 通常开启").build(),
                MessageFieldMeta.builder().name("ssl").label("SSL").inputType("switch").required(false)
                        .defaultValue("false").help("端口 465 通常开启").build()
        );
    }

    @Override
    public void validateConfig(Map<String, Object> props) {
        if (blank(props, "host") || blank(props, "username") || blank(props, "password") || blank(props, "from")) {
            throw new BizException("邮件需配置 host / username / password / from");
        }
        int port = parsePort(props.get("port"));
        if (port <= 0 || port > 65535) {
            throw new BizException("SMTP 端口不合法");
        }
    }

    @Override
    public void send(MessagePayload payload, MessageRuntimeConfig config) {
        if (payload == null || payload.getTo() == null || payload.getTo().isEmpty()) {
            throw new BizException("收件人不能为空");
        }
        try {
            Properties props = new Properties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.host", config.prop("host"));
            props.put("mail.smtp.port", String.valueOf(config.propInt("port", 587)));
            boolean auth = config.propBool("auth", true);
            props.put("mail.smtp.auth", String.valueOf(auth));
            if (config.propBool("starttls", true)) {
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
            }
            if (config.propBool("ssl", false)) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", String.valueOf(config.propInt("port", 465)));
            }

            Session session;
            if (auth) {
                String username = config.prop("username");
                String password = config.prop("password");
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                session = Session.getInstance(props);
            }

            MimeMessage message = new MimeMessage(session);
            String from = config.prop("from");
            String fromName = config.prop("fromName");
            if (fromName.isBlank()) {
                message.setFrom(new InternetAddress(from));
            } else {
                message.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            }
            for (String to : payload.getTo()) {
                if (to != null && !to.isBlank()) {
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(to.trim()));
                }
            }
            if (payload.getCc() != null) {
                for (String cc : payload.getCc()) {
                    if (cc != null && !cc.isBlank()) {
                        message.addRecipient(Message.RecipientType.CC, new InternetAddress(cc.trim()));
                    }
                }
            }
            message.setSubject(payload.getTitle() == null ? "" : payload.getTitle(), StandardCharsets.UTF_8.name());
            String content = payload.getContent() == null ? "" : payload.getContent();
            if (payload.isHtml()) {
                message.setContent(content, "text/html; charset=UTF-8");
            } else {
                message.setText(content, StandardCharsets.UTF_8.name());
            }
            Transport.send(message);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("邮件发送失败：" + e.getMessage());
        }
    }

    private static boolean blank(Map<String, Object> props, String key) {
        if (props == null || props.get(key) == null) {
            return true;
        }
        return String.valueOf(props.get(key)).isBlank();
    }

    private static int parsePort(Object v) {
        if (v == null) {
            return 587;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
