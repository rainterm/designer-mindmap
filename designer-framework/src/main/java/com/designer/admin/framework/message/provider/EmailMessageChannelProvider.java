package com.designer.admin.framework.message.provider;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.message.MessageChannelConsts;
import com.designer.admin.framework.message.MessageChannelProvider;
import com.designer.admin.framework.message.MessagePayload;
import com.designer.admin.framework.message.MessageRuntimeConfig;
import com.designer.admin.framework.spi.ConfigFieldMeta;
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
 *
 * @author 雨夜终端
 */
@Component
public class EmailMessageChannelProvider implements MessageChannelProvider {

    /** SMTP 服务器地址 */
    private static final String KEY_HOST = "host";

    /** SMTP 登录用户名 */
    private static final String KEY_USERNAME = "username";

    /** SMTP 登录密码 */
    private static final String KEY_PASSWORD = "password";

    /** 发件人地址 */
    private static final String KEY_FROM = "from";

    /** 是否开启 STARTTLS */
    private static final String KEY_STARTTLS = "starttls";

    /** 是否开启 SSL */
    private static final String KEY_SSL = "ssl";

    /** TCP 端口上限 */
    private static final int MAX_PORT = 65535;

    @Override
    public String type() {
        return MessageChannelConsts.TYPE_EMAIL;
    }

    @Override
    public String label() {
        return "邮件";
    }

    @Override
    public List<ConfigFieldMeta> configFields() {
        return List.of(
                ConfigFieldMeta.builder().name("host").label("SMTP Host").inputType("text").required(true)
                        .placeholder("smtp.example.com").build(),
                ConfigFieldMeta.builder().name("port").label("端口").inputType("number").required(true)
                        .defaultValue("587").placeholder("587").help("常用：587(STARTTLS) / 465(SSL) / 25").build(),
                ConfigFieldMeta.builder().name("username").label("用户名").inputType("text").required(true)
                        .placeholder("账号或完整邮箱").build(),
                ConfigFieldMeta.builder().name("password").label("密码/授权码").inputType("password").required(true)
                        .placeholder("SMTP 密码或授权码").build(),
                ConfigFieldMeta.builder().name("from").label("发件人邮箱").inputType("text").required(true)
                        .placeholder("noreply@example.com").build(),
                ConfigFieldMeta.builder().name("fromName").label("发件人名称").inputType("text").required(false)
                        .placeholder("系统通知").defaultValue("系统通知").build(),
                ConfigFieldMeta.builder().name("auth").label("启用认证").inputType("switch").required(false)
                        .defaultValue("true").build(),
                ConfigFieldMeta.builder().name("starttls").label("STARTTLS").inputType("switch").required(false)
                        .defaultValue("true").help("端口 587 通常开启").build(),
                ConfigFieldMeta.builder().name("ssl").label("SSL").inputType("switch").required(false)
                        .defaultValue("false").help("端口 465 通常开启").build()
        );
    }

    @Override
    public void validateConfig(Map<String, Object> props) {
        if (blank(props, KEY_HOST) || blank(props, KEY_USERNAME)
                || blank(props, KEY_PASSWORD) || blank(props, KEY_FROM)) {
            throw new BizException("邮件需配置 host / username / password / from");
        }
        int port = parsePort(props.get("port"));
        if (port <= 0 || port > MAX_PORT) {
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
            if (config.propBool(KEY_STARTTLS, true)) {
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
            }
            if (config.propBool(KEY_SSL, false)) {
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
