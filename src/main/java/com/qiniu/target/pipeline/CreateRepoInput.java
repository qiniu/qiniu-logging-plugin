package com.qiniu.target.pipeline;

import com.google.gson.annotations.SerializedName;

/**
 * Created by jemy on 2018/6/11.
 */
public class CreateRepoInput {
    @SerializedName("region")
    public String region;
    @SerializedName("schema")
    public RepoSchemaEntry[] schema;
    @SerializedName("workflow")
    public String workflowName;
}

