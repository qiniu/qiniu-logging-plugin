package com.qiniu.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Created by jemy on 2018/6/28.
 */
public class LogbackQiniuAppender extends AppenderBase<ILoggingEvent> {
    protected void append(ILoggingEvent logEvent) {
        System.out.println(logEvent);
    }
}
