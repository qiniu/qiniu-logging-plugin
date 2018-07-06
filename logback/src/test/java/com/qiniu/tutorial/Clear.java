package com.qiniu.tutorial;

import com.qiniu.appender.LogbackQiniuAppender;
import com.qiniu.pandora.common.PandoraClient;
import com.qiniu.pandora.common.PandoraClientImpl;
import com.qiniu.pandora.common.QiniuException;
import com.qiniu.pandora.logdb.LogDBClient;
import com.qiniu.pandora.pipeline.PipelineClient;
import com.qiniu.pandora.util.Auth;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jemy on 2018/7/6.
 */
public class Clear {
    private PipelineClient pipelineClient;
    private LogDBClient logDBClient;

    private String pipelineHost;
    private String logdbHost;
    private String accessKey;
    private String secretKey;
    private String workflowName;
    private String pipelineRepo;
    private String logdbRepo;
    private String exportName;

    @Before
    public void tearUp() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                Logger.ROOT_LOGGER_NAME);
        LogbackQiniuAppender qiniuAppender = (LogbackQiniuAppender) logger.getAppender("qiniu");
        this.pipelineHost = qiniuAppender.getPipelineHost();
        this.logdbHost = qiniuAppender.getLogdbHost();
        this.accessKey = qiniuAppender.getAccessKey();
        this.secretKey = qiniuAppender.getSecretKey();
        this.workflowName = qiniuAppender.getWorkflowName();
        this.pipelineRepo = qiniuAppender.getPipelineRepo();
        this.logdbRepo = qiniuAppender.getLogdbRepo();
        this.exportName = String.format("%s_export_to_%s", this.pipelineRepo, this.logdbRepo);

        Auth auth = Auth.create(this.accessKey, this.secretKey);
        PandoraClient client = new PandoraClientImpl(auth);
        this.pipelineClient = new PipelineClient(client, this.pipelineHost);
        this.logDBClient = new LogDBClient(client, this.logdbHost);
    }

    @Test
    public void testResetWorkflow() {
        //delete export
        try {
            this.pipelineClient.deleteExport(this.pipelineRepo, this.exportName);
        } catch (QiniuException e) {
            e.printStackTrace();
        }

        //delete old logdb
        try {
            this.logDBClient.deleteRepo(this.logdbRepo);
        } catch (QiniuException e) {
            e.printStackTrace();
        }

        //delete pipeline
        try {
            this.pipelineClient.deleteRepo(this.pipelineRepo);
        } catch (QiniuException e) {
            e.printStackTrace();
        }

        //delete old workflow
        try {
            this.pipelineClient.deleteWorkflow(this.workflowName);
        } catch (QiniuException e) {
            e.printStackTrace();
        }
    }
}
