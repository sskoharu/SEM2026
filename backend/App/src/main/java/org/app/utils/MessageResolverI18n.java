package org.app.utils;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class MessageResolverI18n {
    final MessageSource messageSource;

    public MessageResolverI18n(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String code, Object... args) throws NoSuchMessageException {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    public String getOrDefault(String code, String defaultMessage, Object... args) {
        return messageSource.getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
    }
}
