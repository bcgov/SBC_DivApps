package org.camunda.bpm.extension.hooks.listeners;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.extension.hooks.services.IMessageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class to apply default behavior to listeners.
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
public class BaseListener implements IMessageEvent {

    private final Logger LOGGER = Logger.getLogger(BaseListener.class.getName());

    protected void handleException(DelegateExecution execution, ExceptionSource category, Exception e) {
        notifyForAttention(execution, ExceptionUtils.getRootCauseMessage(e));
        if(ExceptionSource.EXECUTION.name().equals(category.name())) {
            handleExecutionException(e);
        }
        if(ExceptionSource.TASK.name().equals(category.name())) {
            handleTaskException(e);
        }
    }

    private void handleExecutionException(Exception e) {
        throw new RuntimeException(ExceptionUtils.getRootCause(e));
    }

    private void handleTaskException(Exception e) {
        LOGGER.log(Level.SEVERE, "Exception Occurred" , e);
    }


    private void notifyForAttention(DelegateExecution execution, String exceptionMessage){
        Map<String,Object> exVarMap = new HashMap<>();
        //Additional Response Fields - BEGIN
        exVarMap.put("pid",execution.getProcessInstanceId());
        exVarMap.put("subject",("Exception Occurred ").concat(" for id: ").concat(execution.getProcessInstanceId()));
        exVarMap.put("category","api_service_exception");
        exVarMap.put("exception", Variables.stringValue(exceptionMessage,true));
        //Additional Response Fields - END
        sendMessage(execution,exVarMap,getMessageName());
        LOGGER.info("\n\nMessage sent! " + "\n\n");
    }

    private String getMessageName(){
        return "Service_Api_Message_Email";
    }

    public enum ExceptionSource {
        TASK,
        EXECUTION;
    }
}
