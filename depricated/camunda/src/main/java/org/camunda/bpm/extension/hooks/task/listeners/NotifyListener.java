package org.camunda.bpm.extension.hooks.task.listeners;

import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.extension.hooks.services.IMessageEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Assignment Task Listener to start a message event when a user is assigned
 *
 * @author yichun.zhao@aot-technologies.com
 */
@Component
public class NotifyListener implements TaskListener, IMessageEvent {

    private static final Logger log = Logger.getLogger(NotifyListener.class.getName());

    private Expression messageName;

    @Value("${formbuilder.pipeline.service.bpm-url}")
    private String appcontexturl;

    /**
     * This provides the necessary information to send message.
     *
     * @param delegateTask: The task which sends the message
     */
    public void notify(DelegateTask delegateTask) {
    	if(delegateTask.getExecution().getVariable("isServiceGroupNotNeeded") != null && (Boolean)delegateTask.getExecution().getVariable("isServiceGroupNotNeeded")) {
        log.info("\n\nNotify listener invoked with isServiceGroupNotNeeded! " + delegateTask.getId());
            sendEmailNotification(delegateTask.getExecution(), getEmailsWOServiceGroup(delegateTask),delegateTask.getId());
    	} else {
    		log.info("\n\nNotify listener invoked! " + delegateTask.getId());
            	sendEmailNotification(delegateTask.getExecution(), getEmailsOfUnassignedTask(delegateTask),delegateTask.getId());
    	}
    }

    private void sendEmailNotification(DelegateExecution execution,List<String> toEmails,String taskId) {
        String toAddress = CollectionUtils.isNotEmpty(toEmails) ? StringUtils.join(toEmails,",") : null;
        if(StringUtils.isNotEmpty(toAddress)) {
            Map<String, Object> emailAttributes = new HashMap<>();
            emailAttributes.put("to", toAddress);
            emailAttributes.put("category", "assignment_notification");
            emailAttributes.put("name",getDefaultAddresseName());
            emailAttributes.put("taskid",taskId);
            log.info("Inside notify attributes:" + emailAttributes);
            execution.setVariable("taskurl", getAPIContextURL()+"/app/tasklist/default/#/?task="+taskId);
            if(StringUtils.isNotBlank(toAddress) && StringUtils.indexOf(toAddress,"@") > 0) {
                sendMessage(execution, emailAttributes,getMessageName(execution));
            }
        }
    }

    private String getAPIContextURL() {
        return StringUtils.remove(StringUtils.remove(appcontexturl, StringUtils.substringBetween(appcontexturl,"://","@")),"@");
    }



    private String getMessageName(DelegateExecution execution){
        return String.valueOf(this.messageName.getValue(execution));
    }
}
