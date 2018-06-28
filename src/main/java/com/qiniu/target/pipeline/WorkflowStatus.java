package com.qiniu.target.pipeline;

import com.google.gson.annotations.SerializedName;

/**
 * Created by jemy on 2018/6/19.
 */
public class WorkflowStatus {
    @SerializedName("name")
    public String name;
    @SerializedName("region")
    public String region;
    @SerializedName("status")
    public String status;
}
