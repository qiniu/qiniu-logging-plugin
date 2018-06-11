package com.qiniu.target.pipeline;

import com.google.gson.annotations.SerializedName;

/**
 * Created by jemy on 2018/6/11.
 */
public class RepoSchemaEntry {
    @SerializedName("key")
    public String key;
    @SerializedName("valtype")
    public String valType;
    @SerializedName("required")
    public boolean required;
    @SerializedName("elemtype")
    public String elemType;
}
