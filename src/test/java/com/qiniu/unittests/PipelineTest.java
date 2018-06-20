package com.qiniu.unittests;

import com.qiniu.pandora.common.PandoraClient;
import com.qiniu.pandora.common.PandoraClientImpl;
import com.qiniu.pandora.util.Auth;
import com.qiniu.target.pipeline.CreateWorkflowInput;
import com.qiniu.target.pipeline.PipelineClient;
import com.qiniu.target.pipeline.WorkflowStatus;
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
        Auth auth= Auth.create(ConfigTest.ACCESS_KEY,ConfigTest.SECRET_KEY);
        PandoraClient pandoraClient=new PandoraClientImpl(auth);
        this.client = new PipelineClient(pandoraClient);
    }

    @Test
    public void testCreateWorkflow(){
        CreateWorkflowInput input=new CreateWorkflowInput();
        input.workflowName="test1111";
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
        String workflowName="test1111";
        try {
           boolean exists= this.client.workflowExists(workflowName);
            Assert.assertEquals(exists,true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWorkflowStatus() throws Exception {
        String workflowName="logdemo1";
        WorkflowStatus status=this.client.workflowStatus(workflowName);
        System.out.println(status.status);
    }
}
