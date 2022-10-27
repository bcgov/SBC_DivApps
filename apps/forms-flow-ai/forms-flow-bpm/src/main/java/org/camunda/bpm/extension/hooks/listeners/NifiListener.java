package org.camunda.bpm.extension.hooks.listeners;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.extension.commons.connector.HTTPServiceInvoker;
import org.camunda.bpm.extension.hooks.services.FormSubmissionService;
import org.glassfish.jersey.internal.util.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom Listener to interact with Nifi endpoint to post data for analytics.
 */
public class NifiListener extends BaseListener implements TaskListener, ExecutionListener {
    private final Logger LOGGER = Logger.getLogger(NifiListener.class.getName());

    @Autowired
    private Properties clientCredentialProperties;

    @Autowired
    private HTTPServiceInvoker httpServiceInvoker;

    @Autowired
    private FormSubmissionService formSubmissionService;

    @Override
    public void notify(DelegateExecution execution) {
        try {
            postToNiFi(execution);
        } catch (Exception e) {
            handleException(execution, BaseListener.ExceptionSource.EXECUTION, e);
            notifyForAttention(execution);
        }
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            postToNiFi(delegateTask.getExecution());
        } catch (Exception e) {
            handleException(delegateTask.getExecution(), BaseListener.ExceptionSource.TASK, e);
            notifyForAttention(delegateTask.getExecution());
        }
    }

    private void postToNiFi(DelegateExecution execution) throws Exception {
        try {
            ResponseEntity<String> response =
                    httpServiceInvoker
                            .execute(getNifiURL(),
                                    HttpMethod.POST,
                                    formSubmissionService.createFormSubmissionData(execution.getVariables()));
            if(response.getStatusCodeValue() != HttpStatus.OK.value()) {
                notifyForAttention(execution);
            }
        } catch (Exception e) {

        }
    }

    private String getNifiURL(){
        return clientCredentialProperties.getProperty("nifi-url");
    }

    private void notifyForAttention(DelegateExecution execution){
        Map<String,Object> variables = new HashMap<>();
        try {
            Map<String,Object> exVarMap = new HashMap<>();
            //Additional Response Fields - BEGIN
            exVarMap.put("pid",execution.getId());
            exVarMap.put("subject",("Unable to communicate with Nifi: ".concat(execution.getId())));
            exVarMap.put("category","analytics_service_exception");
            //Additional Response Fields - END
            sendMessage(execution,exVarMap,getMessageName());
            LOGGER.info("\n\nMessage sent! " + "\n\n");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE,"Exception occurred:"+ ExceptionUtils.exceptionStackTraceAsString(ex));
        }
    }

    private String getMessageName(){
        return "Service_Api_Message_Email";
    }
}
