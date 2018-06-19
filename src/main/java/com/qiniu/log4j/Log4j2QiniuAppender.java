package com.qiniu.log4j;

import com.qiniu.pandora.common.Config;
import com.qiniu.pandora.common.PandoraClient;
import com.qiniu.pandora.common.PandoraClientImpl;
import com.qiniu.pandora.common.QiniuException;
import com.qiniu.pandora.http.Response;
import com.qiniu.pandora.pipeline.points.Batch;
import com.qiniu.pandora.pipeline.points.Point;
import com.qiniu.pandora.util.Auth;
import com.qiniu.target.common.Analyzer;
import com.qiniu.target.common.ValueType;
import com.qiniu.target.common.WFStatus;
import com.qiniu.target.common.Whence;
import com.qiniu.target.logdb.LogdbClient;
import com.qiniu.target.pipeline.*;
import com.qiniu.target.sender.LogSender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by jemy on 2018/6/8.
 * <p>
 * Based on log4j version 2.x
 */
@Plugin(name = "Log4j2QiniuAppender", category = "Core", elementType = "appender", printObject = true)
public class Log4j2QiniuAppender extends AbstractAppender implements ValueType, Analyzer, Whence, WFStatus {
    private static int DefaultAutoFlushInterval = 5;//5 seconds
    private Lock rwLock;
    private Batch batch;
    private ExecutorService executorService;
    private LogSender logSender;

    private Log4j2QiniuAppender(String name, Filter filter, Layout<? extends Serializable> layout,
                                boolean ignoreExceptions, String pipelineRepo, PandoraClient client, int autoFlushInterval) {
        super(name, filter, layout, ignoreExceptions);
        this.batch = new Batch();
        this.executorService = Executors.newCachedThreadPool();
        this.logSender = new LogSender(pipelineRepo, client);

        if (autoFlushInterval <= 0) {
            autoFlushInterval = DefaultAutoFlushInterval;
        }
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
        final byte[] postBody;
        try {
            if (batch.getSize() > 0) {
                postBody = batch.toString().getBytes("utf-8");
                this.executorService.execute(new Runnable() {
                    public void run() {
                        try {
                            Response response = logSender.send(postBody);
                            response.close();
                        } catch (QiniuException e) {
                            e.printStackTrace();
                            //@TODO cache to local?
                        }
                    }
                });
                batch.clear();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        this.rwLock.unlock();
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
                exceptionBuilder.append(t.getMessage());
                for (StackTraceElement element : logEvent.getThrown().getStackTrace()) {
                    exceptionBuilder.append(element.toString());
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
        if (!batch.canAdd(point)) {
            try {
                final byte[] postBody = batch.toString().getBytes("utf-8");
                this.executorService.execute(new Runnable() {
                    public void run() {
                        try {
                            Response response = logSender.send(postBody);
                            response.close();
                        } catch (QiniuException e) {
                            e.printStackTrace();
                            //@TODO cache to local?
                        }
                    }
                });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            batch.clear();
        }
        batch.add(point);
        this.rwLock.unlock();
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
                                                     @PluginAttribute("workflowName") String workflowName,
                                                     @PluginAttribute("pipelineRepo") String pipelineRepo,
                                                     @PluginAttribute("logdbRepo") String logdbRepo,
                                                     @PluginAttribute("accessKey") String accessKey,
                                                     @PluginAttribute("secretKey") String secretKey,
                                                     @PluginAttribute("autoFlushInterval") int autoFlushInterval,
                                                     @PluginElement("filter") final Filter filter,
                                                     @PluginElement("layout") Layout<? extends Serializable> layout,
                                                     @PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {
        Auth auth = Auth.create(accessKey, secretKey);
        PandoraClient client = new PandoraClientImpl(auth);
        PipelineClient pipelineClient = new PipelineClient(client);
        LogdbClient logdbClient = new LogdbClient(client);
        try {
            //check workflow
            if (!pipelineClient.workflowExists(workflowName)) {
                CreateWorkflowInput createWorkflowInput = new CreateWorkflowInput();
                createWorkflowInput.workflowName = workflowName;
                createWorkflowInput.region = Config.region;
                pipelineClient.createWorkflow(createWorkflowInput);
            }

            //check pipeline
            if (!pipelineClient.repoExists(pipelineRepo)) {
                CreateRepoInput createRepoInput = new CreateRepoInput();
                createRepoInput.workflowName = workflowName;
                createRepoInput.region = Config.region;
                createRepoInput.schema = new RepoSchemaEntry[]{
                        new RepoSchemaEntry("timestamp", TypeLong, true),
                        new RepoSchemaEntry("level", TypeString, true),
                        new RepoSchemaEntry("logger", TypeString, true),
                        new RepoSchemaEntry("marker", TypeString, true),
                        new RepoSchemaEntry("message", TypeString, true),
                        new RepoSchemaEntry("thread_name", TypeString, true),
                        new RepoSchemaEntry("thread_id", TypeLong, true),
                        new RepoSchemaEntry("thread_priority", TypeLong, true),
                        new RepoSchemaEntry("exception", TypeString, true),
                };
                pipelineClient.createRepo(pipelineRepo, createRepoInput);
            }

            //check logdb
            if (!logdbClient.repoExists(logdbRepo)) {
                com.qiniu.target.logdb.CreateRepoInput createRepoInput = new com.qiniu.target.logdb.CreateRepoInput();
                createRepoInput.region = "nb";
                createRepoInput.retention = "30d";
                createRepoInput.schema = new com.qiniu.target.logdb.RepoSchemaEntry[]{
                        new com.qiniu.target.logdb.RepoSchemaEntry("timestamp", TypeDate),
                        new com.qiniu.target.logdb.RepoSchemaEntry("level", TypeString, KeyWordAnalyzer),
                        new com.qiniu.target.logdb.RepoSchemaEntry("logger", TypeString, KeyWordAnalyzer),
                        new com.qiniu.target.logdb.RepoSchemaEntry("marker", TypeString, KeyWordAnalyzer),
                        new com.qiniu.target.logdb.RepoSchemaEntry("message", TypeString, StandardAnalyzer),
                        new com.qiniu.target.logdb.RepoSchemaEntry("thread_name", TypeString, StandardAnalyzer),
                        new com.qiniu.target.logdb.RepoSchemaEntry("thread_id", TypeLong),
                        new com.qiniu.target.logdb.RepoSchemaEntry("thread_priority", TypeLong),
                        new com.qiniu.target.logdb.RepoSchemaEntry("exception", TypeString, StandardAnalyzer),
                };
                logdbClient.createRepo(logdbRepo, createRepoInput);
            }

            //check export
            String exportName = String.format("%s_export_to_%s", pipelineRepo, logdbRepo);
            if (!pipelineClient.exportExists(pipelineRepo, exportName)) {
                CreateExportInput<ExportLogdbSpec> createExportInput = new CreateExportInput<ExportLogdbSpec>();
                createExportInput.whence = WHENCE_NEWEST;
                createExportInput.type = "logdb";
                Map<String, Object> exportFields = new HashMap<String, Object>();
                exportFields.put("timestamp", "#timestamp");
                exportFields.put("level", "#level");
                exportFields.put("logger", "#logger");
                exportFields.put("marker", "#marker");
                exportFields.put("message", "#message");
                exportFields.put("thread_name", "#thread_name");
                exportFields.put("thread_id", "#thread_id");
                exportFields.put("thread_priority", "#thread_priority");
                exportFields.put("exception", "#exception");
                createExportInput.spec = new ExportLogdbSpec(logdbRepo, exportFields, true, true);
                pipelineClient.createExport(pipelineRepo, exportName, createExportInput);
            }

            //start workflow
            WorkflowStatus workflowStatus = pipelineClient.workflowStatus(workflowName);
            if (!workflowStatus.status.equals(WORKFLOW_STARTED)) {
                pipelineClient.startWorkflow(workflowName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //logging based on Qiniu initialization failed
            return null;
        }

        return new Log4j2QiniuAppender(name, filter, layout, ignoreExceptions, pipelineRepo, client, autoFlushInterval);
    }
}
