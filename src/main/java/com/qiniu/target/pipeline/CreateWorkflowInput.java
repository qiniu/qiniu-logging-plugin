package com.qiniu.target.pipeline;

import com.google.gson.annotations.SerializedName;

/**
 * Created by jemy on 2018/6/11.
 */
public class CreateWorkflowInput {
    @SerializedName("name")
    public String workflowName;
    @SerializedName("region")
    public String region;
    @SerializedName("comment")
    public String comment;
}
