package com.qiniu.appender;

import com.qiniu.pandora.common.PandoraClient;
import com.qiniu.pandora.common.ValueType;
import com.qiniu.pandora.logdb.LogDBClient;
import com.qiniu.pandora.logdb.repo.Analyzer;
import com.qiniu.pandora.pipeline.PipelineClient;
import com.qiniu.pandora.pipeline.repo.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jemy on 2018/6/19.
 */
public class QiniuAppenderClient implements ValueType, Analyzer, ExportWhence, WorkflowStatus {

    /**
     * Create appender workflow
     *
     * @param client         pandora client
     * @param workflowName   pandora workflow name
     * @param workflowRegion pandora workflow region
     * @param pipelineRepo   pandora pipeline repo name
     * @param logdbRepo      pandora logdb repo name
     * @param logdbRetention pandora logdb retention days
     */
    public static void createAppenderWorkflow(PandoraClient client, String pipelineHost, String logdbHost,
                                              String workflowName, String workflowRegion, String pipelineRepo,
                                              String logdbRepo, String logdbRetention)
            throws Exception {
        PipelineClient pipelineClient = null;
        if (pipelineHost != null && !pipelineHost.isEmpty()) {
            pipelineClient = new PipelineClient(client, pipelineHost);
        } else {
            pipelineClient = new PipelineClient(client);
        }

        LogDBClient logdbClient = null;
        if (logdbHost != null && !logdbHost.isEmpty()) {
            logdbClient = new LogDBClient(client, logdbHost);
        } else {
            logdbClient = new LogDBClient(client);
        }

        //check workflow
        if (!pipelineClient.workflowExists(workflowName)) {
            CreateWorkflowInput createWorkflowInput = new CreateWorkflowInput();
            createWorkflowInput.workflowName = workflowName;
            createWorkflowInput.region = workflowRegion;
            pipelineClient.createWorkflow(createWorkflowInput);
        }

        //check pipeline
        if (!pipelineClient.repoExists(pipelineRepo)) {
            CreateRepoInput createRepoInput = new CreateRepoInput();
            createRepoInput.workflowName = workflowName;
            createRepoInput.region = workflowRegion;
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
            com.qiniu.pandora.logdb.repo.CreateRepoInput createRepoInput = new
                    com.qiniu.pandora.logdb.repo.CreateRepoInput();
            createRepoInput.region = workflowRegion;
            createRepoInput.retention = logdbRetention;
            createRepoInput.schema = new com.qiniu.pandora.logdb.repo.RepoSchemaEntry[]{
                    new com.qiniu.pandora.logdb.repo.RepoSchemaEntry("timestamp", TypeDate),
                    new com.qiniu.pandora.logdb.repo.RepoSchemaEntry("level", TypeString, KeyWordAnalyzer),
                    new com.qiniu.pandora.logdb.repo.RepoSchemaEntry("logger", TypeString, StandardAnalyzer),
                    new com.qiniu.pandora.logdb.repo.RepoSchemaEntry("marker", TypeString, StandardAnalyzer),
                    new com.qiniu.pandora.logdb.repo.RepoSchemaEntry("message", TypeString, StandardAnalyzer),
                    new com.qiniu.pandora.logdb.repo.RepoSchemaEntry("thread_name", TypeString, StandardAnalyzer),
                    new com.qiniu.pandora.logdb.repo.RepoSchemaEntry("thread_id", TypeLong),
                    new com.qiniu.pandora.logdb.repo.RepoSchemaEntry("thread_priority", TypeLong),
                    new com.qiniu.pandora.logdb.repo.RepoSchemaEntry("exception", TypeString, StandardAnalyzer),
            };
            logdbClient.createRepo(logdbRepo, createRepoInput);
        }

        //check export
        String exportName = String.format("%s_export_to_%s", pipelineRepo, logdbRepo);
        if (!pipelineClient.exportExists(pipelineRepo, exportName)) {
            CreateExportInput<CreateExportInput.ExportLogDBSpec> createExportInput =
                    new CreateExportInput<CreateExportInput.ExportLogDBSpec>();
            createExportInput.whence = WhenceOldest;
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
            createExportInput.spec = new CreateExportInput.ExportLogDBSpec(logdbRepo, exportFields, true,
                    true);
            pipelineClient.createExport(pipelineRepo, exportName, createExportInput);
        }

        //start workflow
        GetWorkflowStatus workflowStatus = pipelineClient.getWorkflowStatus(workflowName);
        if (!(workflowStatus.status.equals(WorkflowStarted) || workflowStatus.status.equals(WorkflowStarting))) {
            pipelineClient.startWorkflow(workflowName);
        }
    }
}
