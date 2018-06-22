package com.qiniu.log4j;

import com.qiniu.pandora.common.PandoraClient;
import com.qiniu.target.common.Analyzer;
import com.qiniu.target.common.ValueType;
import com.qiniu.target.common.WFStatus;
import com.qiniu.target.common.Whence;
import com.qiniu.target.logdb.LogdbClient;
import com.qiniu.target.pipeline.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jemy on 2018/6/19.
 */
public class QiniuAppenderClient implements ValueType, Analyzer, Whence, WFStatus {

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
    public static void createAppenderWorkflow(PandoraClient client, String workflowName, String workflowRegion,
                                              String pipelineRepo, String logdbRepo, String logdbRetention)
            throws Exception {
        PipelineClient pipelineClient = new PipelineClient(client);
        LogdbClient logdbClient = new LogdbClient(client);

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
            com.qiniu.target.logdb.CreateRepoInput createRepoInput = new com.qiniu.target.logdb.CreateRepoInput();
            createRepoInput.region = workflowRegion;
            createRepoInput.retention = logdbRetention;
            createRepoInput.schema = new com.qiniu.target.logdb.RepoSchemaEntry[]{
                    new com.qiniu.target.logdb.RepoSchemaEntry("timestamp", TypeDate),
                    new com.qiniu.target.logdb.RepoSchemaEntry("level", TypeString, KeyWordAnalyzer),
                    new com.qiniu.target.logdb.RepoSchemaEntry("logger", TypeString, StandardAnalyzer),
                    new com.qiniu.target.logdb.RepoSchemaEntry("marker", TypeString, StandardAnalyzer),
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
            createExportInput.whence = WHENCE_OLDEST;
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
            //@TODO check whether workflow started
        }
    }
}
