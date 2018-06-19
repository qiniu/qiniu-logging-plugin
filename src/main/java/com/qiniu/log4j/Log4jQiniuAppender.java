package com.qiniu.log4j;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Created by jemy on 2018/6/11.
 * <p>
 * Based on log4j version 1.x
 */
public class Log4jQiniuAppender extends AppenderSkeleton {

    private String target;
    private String accessKey;
    private String secretKey;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    protected void append(LoggingEvent loggingEvent) {

    }

    public void close() {

    }

    public boolean requiresLayout() {
        return false;
    }
}
