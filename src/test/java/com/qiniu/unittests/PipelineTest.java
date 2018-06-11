package com.qiniu.unittests;

import com.qiniu.target.pipeline.CreateWorkflowInput;
import com.qiniu.target.pipeline.PipelineClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by jemy on 2018/6/11.
 */
public class PipelineTest {
    private PipelineClient client;

    @Before
    public void tearUp() {
        this.client = new PipelineClient(ConfigTest.ACCESS_KEY, ConfigTest.SECRET_KEY);
    }

    @Test
    public void testCreateWorkflow(){
        CreateWorkflowInput input=new CreateWorkflowInput();
        input.workflowName="test11111";
        input.region="nb";
        input.comment="test 11111";
        try {
            this.client.createWorkflow(input);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testGetWorkflow() {
        String workflowName="test11111";
        try {
           boolean exists= this.client.workflowExists(workflowName);
            Assert.assertEquals(exists,true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
