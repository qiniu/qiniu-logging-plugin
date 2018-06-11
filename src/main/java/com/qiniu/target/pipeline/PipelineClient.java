package com.qiniu.target.pipeline;

import com.qiniu.pandora.common.Config;
import com.qiniu.pandora.common.PandoraClientImpl;
import com.qiniu.pandora.common.QiniuException;
import com.qiniu.pandora.http.Client;
import com.qiniu.pandora.util.Auth;
import com.qiniu.pandora.util.Json;
import com.qiniu.pandora.util.StringMap;

/**
 * Created by jemy on 2018/6/11.
 */
public class PipelineClient {
    private PandoraClientImpl client;
    private String pipelineHost;

    public PipelineClient(String accessKey, String secretKey) {
        this(accessKey, secretKey, "https://pipeline.qiniu.com");
    }

    public PipelineClient(String accessKey, String secretKey, String pipelineHost) {
        Auth auth = Auth.create(accessKey, secretKey);
        this.pipelineHost = pipelineHost;
        this.client = new PandoraClientImpl(auth, "");
    }

    /**
     * Create workflow
     */
    public void createWorkflow(CreateWorkflowInput workflowInput) throws Exception {
        String postUrl = String.format("%s/v2/workflows/%s", this.pipelineHost, workflowInput.workflowName);
        String postBody = Json.encode(workflowInput);
        this.client.post(postUrl, postBody.getBytes(Config.UTF_8), new StringMap(), Client.JsonMime);
    }

    public boolean workflowExists(String workflowName) throws Exception {
        boolean exists = false;
        String getUrl = String.format("%s/v2/workflows/%s", this.pipelineHost, workflowName);
        try {
            this.client.get(getUrl, new StringMap());
            exists = true;
        } catch (QiniuException e) {
            //pass
        }
        return exists;
    }

    /**
     * Create pipeline repo
     */
    public void createRepo(String repoName, CreateRepoInput repoInput) throws Exception {
        String postUrl = String.format("%s/v2/repos/%s", this.pipelineHost, repoName);
        String postBody = Json.encode(repoInput);
        this.client.post(postUrl, postBody.getBytes(Config.UTF_8), new StringMap(), Client.JsonMime);
    }

    /**
     * Check repo exists or not
     */
    public boolean repoExists(String repoName) {
        boolean exists = false;
        String getUrl = String.format("%s/v2/repos/%s", this.pipelineHost, repoName);
        try {
            this.client.get(getUrl, new StringMap());
            exists = true;
        } catch (QiniuException e) {
            //pass
        }
        return exists;
    }


    /**
     * Create export to logdb
     */
    public void createExportToLogdb() {

    }
}
