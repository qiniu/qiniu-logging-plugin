package com.qiniu.log4j2;

import com.qiniu.pandora.pipeline.points.Batch;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;

/**
 * Created by jemy on 2018/6/8.
 * <p>
 * Based on log4j version 2.x
 */
@Plugin(name = "Log4j2QiniuAppender", category = "Core", elementType = "appender", printObject = true)
public class Log4j2QiniuAppender extends AbstractAppender {
    private StringBuffer logBuffer;

    protected Log4j2QiniuAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        this(name, filter, layout, false);
    }

    protected Log4j2QiniuAppender(String name, Filter filter, Layout<? extends Serializable> layout,
                                  boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        this.logBuffer = new StringBuffer(Batch.MAX_BATCH_SIZE);
    }

    public void append(LogEvent logEvent) {
        //@TODO push data when necessray
    }

    @PluginFactory
    public static Log4j2QiniuAppender createAppender(@PluginAttribute("name") String name,
                                                     @PluginAttribute("target") String target,
                                                     @PluginAttribute("accessKey") String accessKey,
                                                     @PluginAttribute("secretKey") String secretKey,
                                                     @PluginElement("Filter") final Filter filter,
                                                     @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                     @PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {
        System.out.println(accessKey);
        System.out.println(secretKey);
        System.out.println(target);
        //@TODO create the pipeline repo


        return new Log4j2QiniuAppender(name, filter, layout, ignoreExceptions);
    }
}
