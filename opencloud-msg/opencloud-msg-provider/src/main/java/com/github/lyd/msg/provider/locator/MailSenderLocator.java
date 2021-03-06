package com.github.lyd.msg.provider.locator;

import com.github.lyd.msg.provider.configuration.MailChannelsProperties;
import com.google.common.collect.Maps;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件发送者加载器
 * 用于多个发送者随机切换.类似于路由加载器
 *
 * @author: liuyadu
 * @date: 2018/11/27 14:19
 * @description:
 */
public class MailSenderLocator {
    private MailChannelsProperties mailProperties;

    public MailSenderLocator(MailChannelsProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    private Map<String, JavaMailSenderImpl> mailSenders = Maps.newLinkedHashMap();

    public Map<String, JavaMailSenderImpl> locateSenders() {
        Map<String, JavaMailSenderImpl> senders = Maps.newLinkedHashMap();
        if (this.mailProperties != null && this.mailProperties.getChannels() != null) {
            Iterator<Map.Entry<String, MailProperties>> entries = mailProperties.getChannels().entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, MailProperties> entry = entries.next();
                MailProperties properties = entry.getValue();
                JavaMailSenderImpl sender = new JavaMailSenderImpl();
                sender.setHost(properties.getHost());
                if (properties.getPort() != null) {
                    sender.setPort(properties.getPort().intValue());
                }
                sender.setUsername(properties.getUsername());
                sender.setPassword(properties.getPassword());
                sender.setProtocol(properties.getProtocol());
                if (properties.getDefaultEncoding() != null) {
                    sender.setDefaultEncoding(properties.getDefaultEncoding().name());
                }
                if (!properties.getProperties().isEmpty()) {
                    Properties props = new Properties();
                    props.putAll(properties.getProperties());
                    sender.setJavaMailProperties(props);
                }
                senders.put(entry.getKey(), sender);
            }
        }
        return senders;
    }

    public Map<String, JavaMailSenderImpl> getMailSenders() {
        return mailSenders;
    }

    public void setMailSenders(Map<String, JavaMailSenderImpl> mailSenders) {
        this.mailSenders = mailSenders;
    }
}
