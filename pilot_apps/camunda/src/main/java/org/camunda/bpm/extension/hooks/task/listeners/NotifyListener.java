package org.camunda.bpm.extension.hooks.task.listeners;

import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.extension.hooks.services.IMessageEvent;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Assignment Task Listener to start a message event when a user is assigned
 *
 * @author yichun.zhao@aot-technologies.com
 */
public class NotifyListener implements TaskListener, IMessageEvent {

    private static final Logger log = Logger.getLogger(NotifyListener.class.getName());

    /**
     * This provides the necessary information to send message.
     *
     * @param delegateTask: The task which sends the message
     */
    public void notify(DelegateTask delegateTask) {
        log.info("\n\nNotify listener invoked! " + delegateTask.getId());
            sendEmailNotification(delegateTask.getExecution(), getEmailsOfUnassignedTask(delegateTask),delegateTask.getId());
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
            if(StringUtils.isNotEmpty(toAddress)) {
                sendMessage(execution, emailAttributes);
            }
        }
    }

}
