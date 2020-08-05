package com.qiniu.appender;

import com.qiniu.pandora.common.*;
import com.qiniu.pandora.http.Client;
import com.qiniu.pandora.http.Response;
import com.qiniu.pandora.pipeline.points.Batch;
import com.qiniu.pandora.pipeline.points.Point;
import com.qiniu.pandora.pipeline.sender.DataSender;
import com.qiniu.pandora.util.Auth;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by jemy on 2018/6/8.
 * <p>
 * Based on log4j version 2.x
 */
@Plugin(name = "Log4j2QiniuAppender", category = "Core", elementType = "appender", printObject = true)
public class Log4j2QiniuAppender extends AbstractAppender implements Configs {
    private Lock rwLock;
    private Batch batch;
    private BlockingQueue<Runnable> queue;
    private ExecutorService executorService;
    private DataSender logPushSender;
    private QiniuLoggingGuard guard;


    private Log4j2QiniuAppender(String name, Filter filter, Layout<? extends Serializable> layout,
                                boolean ignoreExceptions, Auth auth, String pipelineHost, String pipelineRepo,
                                int autoFlushInterval, String logCacheDir, int logRotateInterval, int logRetryInterval,
                                int logPushThreadPoolSize, int logPushConnectTimeout, int logPushReadTimeout,
                                int logPushWriteTimeout, int logRetryThreadPoolSize, int logRetryConnectTimeout,
                                int logRetryReadTimeout, int logRetryWriteTimeout) {
        super(name, filter, layout, ignoreExceptions);
        if (logPushThreadPoolSize <= 0) {
            logPushThreadPoolSize = Configs.DefaultLogPushThreadPoolSize;
        }
        if (logPushConnectTimeout <= 0) {
            logPushConnectTimeout = Configs.DefaultLogPushConnectTimeout;
        }
        if (logPushReadTimeout <= 0) {
            logPushReadTimeout = Configs.DefaultLogPushReadTimeout;
        }
        if (logPushWriteTimeout <= 0) {
            logPushWriteTimeout = Configs.DefaultLogPushWriteTimeout;
        }
        if (logRetryThreadPoolSize <= 0) {
            logRetryThreadPoolSize = Configs.DefaultLogRetryThreadPoolSize;
        }
        if (logRetryConnectTimeout <= 0) {
            logRetryConnectTimeout = Configs.DefaultLogRetryConnectTimeout;
        }
        if (logRetryReadTimeout <= 0) {
            logRetryReadTimeout = Configs.DefaultLogRetryReadTimeout;
        }
        if (logRetryWriteTimeout <= 0) {
            logRetryWriteTimeout = Configs.DefaultLogRetryWriteTimeout;
        }

        if (autoFlushInterval <= 0) {
            autoFlushInterval = DefaultAutoFlushInterval;
        }

        this.batch = new Batch();
        this.queue = new ArrayBlockingQueue<>(51);
        this.executorService = new ThreadPoolExecutor(2, logPushThreadPoolSize,
                60L, TimeUnit.SECONDS, this.queue);

        //init log push
        Configuration pushCfg = new Configuration();
        pushCfg.connectTimeout = logPushConnectTimeout;
        pushCfg.readTimeout = logPushReadTimeout;
        pushCfg.writeTimeout = logPushWriteTimeout;
        Client pushClient = new Client(pushCfg);
        PandoraClient pushPandoraClient = new PandoraClientImpl(auth, pushClient);

        //init log retry client
        Configuration retryCfg = new Configuration();
        retryCfg.connectTimeout = logRetryConnectTimeout;
        retryCfg.readTimeout = logRetryReadTimeout;
        retryCfg.writeTimeout = logRetryWriteTimeout;
        Client retryClient = new Client(retryCfg);
        PandoraClient retryPandoraClient = new PandoraClientImpl(auth, retryClient);

        //create log push sender & retry sender
        DataSender logRetrySender = null;
        if (pipelineHost != null && !pipelineHost.isEmpty()) {
            logPushSender = new DataSender(pipelineRepo, pushPandoraClient, pipelineHost);
            logRetrySender = new DataSender(pipelineRepo, retryPandoraClient, pipelineHost);
        } else {
            logPushSender = new DataSender(pipelineRepo, pushPandoraClient);
            logRetrySender = new DataSender(pipelineRepo, retryPandoraClient);
        }

        //init log retry guard
        guard = QiniuLoggingGuard.getInstance(logRetryThreadPoolSize);
        if (logCacheDir != null && !logCacheDir.isEmpty()) {
            try {
                guard.setLogCacheDir(logCacheDir);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (logRotateInterval > 0) {
            guard.setLogRotateInterval(logRotateInterval);
        }
        if (logRetryInterval > 0) {
            guard.setLogRetryInterval(logRetryInterval);
        }

        this.guard.setDataSender(logRetrySender);

        this.rwLock = new ReentrantLock(true);
        final int autoFlushSeconds = autoFlushInterval;
        new Thread(new Runnable() {
            public void run() {
                for (; ; ) {
                    intervalFlush();
                    try {
                        TimeUnit.SECONDS.sleep(autoFlushSeconds);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void intervalFlush() {
        this.rwLock.lock();
        try {
            final byte[] postBody;

            if (batch.getSize() > 0) {
                postBody = batch.toString().getBytes(Constants.UTF_8);
                if (this.queue.size() < 50) {
                    try {
                        this.executorService.execute(new Runnable() {
                            public void run() {
                                try {
                                    Response response = logPushSender.send(postBody);
                                    response.close();
                                } catch (QiniuException e) {
                                    //e.printStackTrace();
                                    guard.write(postBody);
                                }
                            }
                        });
                    } catch (RejectedExecutionException ex) {
                        guard.write(postBody);
                    }
                } else {
                    guard.write(postBody);
                }

                batch.clear();
            }
        }finally {
            this.rwLock.unlock();
        }
    }

    public void append(LogEvent logEvent) {
        Point point = new Point();
        point.append("timestamp", logEvent.getTimeMillis());
        point.append("level", logEvent.getLevel().toString());
        point.append("logger", logEvent.getLoggerName());
        if (logEvent.getMarker() != null) {
            point.append("marker", logEvent.getMarker().toString());
        } else {
            point.append("marker", "");
        }
        point.append("message", logEvent.getMessage().getFormattedMessage());
        point.append("thread_name", logEvent.getThreadName());
        point.append("thread_id", logEvent.getThreadId());
        point.append("thread_priority", logEvent.getThreadPriority());
        if (logEvent.getThrown() != null) {
            Throwable t = logEvent.getThrown().getCause();
            if (t != null) {
                StringBuilder exceptionBuilder = new StringBuilder();
                exceptionBuilder.append(t.getMessage()).append("\n");
                for (StackTraceElement element : logEvent.getThrown().getStackTrace()) {
                    exceptionBuilder.append(element.toString()).append("\n");
                }
                point.append("exception", exceptionBuilder.toString());
            } else {
                point.append("exception", logEvent.getThrown().getMessage());
            }
        } else {
            point.append("exception", "");
        }

        //lock
        this.rwLock.lock();
        try {
            if (!batch.canAdd(point)) {
                final byte[] postBody = batch.toString().getBytes(Constants.UTF_8);
                if (this.queue.size() < 50) {
                    try {
                        this.executorService.execute(new Runnable() {
                            public void run() {
                                try {
                                    Response response = logPushSender.send(postBody);
                                    response.close();
                                } catch (QiniuException e) {
                                    //e.printStackTrace();
                                    guard.write(postBody);
                                }
                            }
                        });
                    } catch (RejectedExecutionException ex) {
                        guard.write(postBody);
                    }
                } else {
                    guard.write(postBody);
                }

                batch.clear();
            }
            batch.add(point);
        } finally {
            this.rwLock.unlock();
        }
    }

    /**
     * create the qiniu logdb appender
     *
     * @param name              name
     * @param workflowName      pandora workflow name
     * @param pipelineRepo      pandora pipeline repo name
     * @param logdbRepo         pandora logdb repo name
     * @param autoFlushInterval auto flush log interval in seconds
     * @param accessKey         qiniu access key
     * @param secretKey         qiniu secret key
     * @param filter            filter
     * @param layout            layout
     * @param ignoreExceptions  ignoreExceptions
     */
    @PluginFactory
    public static Log4j2QiniuAppender createAppender(@PluginAttribute("name") String name,
                                                     @PluginAttribute("pipelineHost") String pipelineHost,
                                                     @PluginAttribute("logdbHost") String logdbHost,
                                                     @PluginAttribute("workflowName") String workflowName,
                                                     @PluginAttribute("workflowDesc") String workflowDesc,
                                                     @PluginAttribute("workflowRegion") String workflowRegion,
                                                     @PluginAttribute("pipelineRepo") String pipelineRepo,
                                                     @PluginAttribute("logdbRepo") String logdbRepo,
                                                     @PluginAttribute("logdbRetention") String logdbRetention,
                                                     @PluginAttribute("accessKey") String accessKey,
                                                     @PluginAttribute("secretKey") String secretKey,
                                                     @PluginAttribute("autoFlushInterval") int autoFlushInterval,
                                                     @PluginAttribute("logCacheDir") String logCacheDir,
                                                     @PluginAttribute("logRotateInterval") int logRotateInterval,
                                                     @PluginAttribute("logRetryInterval") int logRetryInterval,
                                                     @PluginAttribute("logPushThreadPoolSize") int logPushThreadPoolSize,
                                                     @PluginAttribute("logPushConnectTimeout") int logPushConnectTimeout,
                                                     @PluginAttribute("logPushReadTimeout") int logPushReadTimeout,
                                                     @PluginAttribute("logPushWriteTimeout") int logPushWriteTimeout,
                                                     @PluginAttribute("logRetryThreadPoolSize") int logRetryThreadPoolSize,
                                                     @PluginAttribute("logRetryConnectTimeout") int logRetryConnectTimeout,
                                                     @PluginAttribute("logRetryReadTimeout") int logRetryReadTimeout,
                                                     @PluginAttribute("logRetryWriteTimeout") int logRetryWriteTimeout,
                                                     @PluginElement("filter") final Filter filter,
                                                     @PluginElement("layout") Layout<? extends Serializable> layout,
                                                     @PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {

        Auth auth = Auth.create(accessKey, secretKey);
        PandoraClient client = new PandoraClientImpl(auth);

        //check attributes
        if (workflowRegion == null || workflowRegion.isEmpty()) {
            workflowRegion = DefaultWorkflowRegion;
        }
        if (logdbRetention == null || logdbRetention.isEmpty()) {
            logdbRetention = DefaultLogdbRetention;
        }

        //try to create appender workflow
        try {
            QiniuAppenderClient.createAppenderWorkflow(client, pipelineHost, logdbHost, workflowName, workflowDesc,
                    workflowRegion, pipelineRepo, logdbRepo, logdbRetention);
        } catch (Exception e) {
            e.printStackTrace();
            return null;//logging appender initialization failed
        }

        return new Log4j2QiniuAppender(name, filter, layout, ignoreExceptions, auth, pipelineHost, pipelineRepo,
                autoFlushInterval, logCacheDir, logRotateInterval, logRetryInterval, logPushThreadPoolSize,
                logPushConnectTimeout, logPushReadTimeout, logPushWriteTimeout, logRetryThreadPoolSize,
                logRetryConnectTimeout, logRetryReadTimeout, logRetryWriteTimeout);
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        this.guard.close();
        this.executorService.shutdown();
        return super.stop(timeout, timeUnit);
    }
}
