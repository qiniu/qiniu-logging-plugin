package com.qiniu.log4j2;

import com.qiniu.pandora.common.Config;
import com.qiniu.pandora.common.PandoraClient;
import com.qiniu.pandora.common.PandoraClientImpl;
import com.qiniu.pandora.common.QiniuException;
import com.qiniu.pandora.pipeline.points.Batch;
import com.qiniu.pandora.pipeline.points.Point;
import com.qiniu.pandora.util.Auth;
import com.qiniu.target.common.Analyzer;
import com.qiniu.target.common.ValueType;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by jemy on 2018/6/8.
 * <p>
 * Based on log4j version 2.x
 */
@Plugin(name = "Log4j2QiniuAppender", category = "Core", elementType = "appender", printObject = true)
public class Log4j2QiniuAppender extends AbstractAppender implements ValueType, Analyzer, Whence {
    private Lock rwLock;
    private Batch batch;
    private ExecutorService executorService;
    private LogSender logSender;

    private Log4j2QiniuAppender(String name, Filter filter, Layout<? extends Serializable> layout,
                                boolean ignoreExceptions, String pipelineRepo, PandoraClient client) {
        super(name, filter, layout, ignoreExceptions);
        this.batch = new Batch();
        this.executorService = Executors.newCachedThreadPool();
        this.logSender = new LogSender(pipelineRepo, client);
        this.rwLock = new ReentrantLock(true);
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
            point.append("exception", logEvent.getThrown().getCause().getMessage());
        } else {
            point.append("exception", "");
        }

        //lock
        this.rwLock.lock();
        if (!batch.canAdd(point)) {
            try {
                final byte[] postBody = batch.toString().getBytes("utf-8");
                this.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            logSender.send(postBody);
                        } catch (QiniuException e) {
                            e.printStackTrace();
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
     * @param name
     * @param pipelineRepo     - pipeline repo
     * @param accessKey        - access key
     * @param secretKey        - secret key
     * @param filter
     * @param layout
     * @param ignoreExceptions
     */
    @PluginFactory
    public static Log4j2QiniuAppender createAppender(@PluginAttribute("name") String name,
                                                     @PluginAttribute("workflowName") String workflowName,
                                                     @PluginAttribute("pipelineRepo") String pipelineRepo,
                                                     @PluginAttribute("logdbRepo") String logdbRepo,
                                                     @PluginAttribute("accessKey") String accessKey,
                                                     @PluginAttribute("secretKey") String secretKey,
                                                     @PluginElement("Filter") final Filter filter,
                                                     @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                     @PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {
        Auth auth = Auth.create(accessKey, secretKey);
        PandoraClient client = new PandoraClientImpl(auth, "");
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
            pipelineClient.startWorkflow(workflowName);
        } catch (Exception e) {
            e.printStackTrace();
            //@TODO check what to return here
        }

        return new Log4j2QiniuAppender(name, filter, layout, ignoreExceptions, pipelineRepo, client);
    }
}
