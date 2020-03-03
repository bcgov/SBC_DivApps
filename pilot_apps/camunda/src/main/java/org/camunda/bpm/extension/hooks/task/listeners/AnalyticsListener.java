package org.camunda.bpm.extension.hooks.task.listeners;



import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.extension.hooks.services.IMessageEvent;
import org.camunda.bpm.extension.hooks.services.analytics.IDataPipeline;
import org.camunda.bpm.extension.hooks.services.analytics.SimpleDBDataPipeline;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *  Java component for pushing the data to downstream analytics system.
 *  This invokes the database pipeline component for publishing data.
 *
 *  Futuristic place-holder to inject the appropriate pipeline using service location pattern.
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
@Named("analyticsDelegate")
public class AnalyticsListener implements TaskListener, ExecutionListener, IMessageEvent {

    @Autowired
    private SimpleDBDataPipeline dbdatapipeline;

    private final Logger LOGGER = Logger.getLogger(AnalyticsListener.class.getName());


    /**
     * This would be invoked by all the user based task services.
     * Invocation point is marked with value "COMPLETE" in process diagram.
     * @param task
     */
    @Override
    public void notify(DelegateTask task) {
        LOGGER.info("\n\n  ... AnalyticsDelegate invoked by task listener for "
                + "processDefinitionId=" + task.getProcessDefinitionId()
                + ", assignee=" + task.getAssignee()
                + ", executionId=" + task.getId()
                + " \n\n");
        Map<String,Object> rspVariableMap = dbdatapipeline.execute(injectAdditionalProcessingFields(task.getExecution(),task.getExecution().getVariables()));
        notifyForAttention(task.getExecution(),rspVariableMap);
    }

    /**
     * This would be invoked during form submission.
     * Invocation point is marked with value "END" in process diagram.
     * @param execution
     * @throws Exception
     */
    @Override
    public void notify(DelegateExecution execution) throws Exception {
        LOGGER.info("\n\n  ... AnalyticsDelegate invoked by execution listener for"
                + "processDefinitionId=" + execution.getProcessDefinitionId()
                + ", executionId=" + execution.getId()
                + " \n\n");
        Map<String,Object> rspVariableMap = dbdatapipeline.execute(injectAdditionalProcessingFields(execution,execution.getVariables()));
        notifyForAttention(execution,rspVariableMap);
    }

    /**
     * This method is intended to inject additional fields for processing.
     *  Injected Fields : File Name, MimeType, Size, Pid.
     * @param execution
     * @param rawMap
     * @return
     */
    private Map<String,Object> injectAdditionalProcessingFields(DelegateExecution execution,Map<String,Object> rawMap) {
        Map<String,Object> variables = injectPrimaryKey(execution,rawMap);
        Map<String,Object> prcMap = new HashMap<>();
        String pid = execution.getId();
        try {
        for(Map.Entry<String,Object> entry : variables.entrySet()) {
            if(StringUtils.endsWith(entry.getKey(),"_file")) {
                FileValue retrievedTypedFileValue = execution.getProcessEngineServices().getRuntimeService().getVariableTyped(pid, entry.getKey());
                if(retrievedTypedFileValue != null && retrievedTypedFileValue.getValue() != null) {
                    InputStream fileContent = retrievedTypedFileValue.getValue();
                    String fileName = retrievedTypedFileValue.getFilename();
                    String mimeType = retrievedTypedFileValue.getMimeType();
                    byte[] fileBytes = IOUtils.toByteArray(fileContent);
                    int fileSize = fileBytes.length;
                    if(StringUtils.isNotEmpty(fileName) && fileSize > 0) {
                        prcMap.put(entry.getKey().concat("_name"),fileName);
                        prcMap.put(entry.getKey().concat("_mimetype"),mimeType);
                        prcMap.put(entry.getKey(), fileBytes);
                        prcMap.put(entry.getKey().concat("_size"),fileSize);
                    }
                }
            } else {
                prcMap.put(entry.getKey(),entry.getValue());
            }
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prcMap;
    }

    /**
     * Evaluate this being injected from process diagram.
     * @return
     */
    private Map<String,Object> injectPrimaryKey(DelegateExecution execution,Map<String,Object> variables) {
        variables.put("pid",execution.getId());
        return variables;
    }

    private void notifyForAttention(DelegateExecution execution,Map<String,Object> rspVariableMap){
            if(IDataPipeline.ResponseStatus.FAILURE.name().equals(rspVariableMap.get("code"))) {
                //Additional Response Fields - BEGIN
                rspVariableMap.put("pid",execution.getId());
                rspVariableMap.put("subject",(String.valueOf(rspVariableMap.get("message")).concat(" for ").concat(execution.getId())));
                rspVariableMap.put("category","analytics_service_exception");
                //Additional Response Fields - END
                sendMessage(execution,rspVariableMap);
                LOGGER.info("\n\nMessage sent! " + "\n\n");
            }
        }

}
