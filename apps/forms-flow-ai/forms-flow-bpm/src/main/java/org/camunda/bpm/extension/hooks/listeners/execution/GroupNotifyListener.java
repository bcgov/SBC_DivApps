package org.camunda.bpm.extension.hooks.listeners.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.*;
import org.camunda.bpm.extension.hooks.services.IMessageEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Assignment Task Listener to start a message event when a task is created
 * Extended to emails to additional groups of interest.
 *
 * @author yichun.zhao@aot-technologies.com, sumathi.thirumani@aot-technologies.com
 */
@Component
public class GroupNotifyListener implements ExecutionListener, IMessageEvent {

    private static final Logger LOGGER = Logger.getLogger(GroupNotifyListener.class.getName());

    private Expression messageId;
    private Expression category;

    private Expression emailGroups;

    /**
     * This provides the necessary information to send message.
     *
     * @param execution: The task which sends the message
     */
    @Override
    public void notify(DelegateExecution execution) throws Exception {
        List<String> toEmails =  new ArrayList<>();

        if (CollectionUtils.isNotEmpty(getEmailGroups(execution))) {
            for (String entry : getEmailGroups(execution)) {
                toEmails.addAll(getEmailsForGroup(execution, entry));
            }
        }
        sendEmailNotification(execution, toEmails ,String.valueOf(execution.getVariables().get("taskId")));


    }
    /**
     *
     * @param execution
     * @param toEmails
     * @param taskId
     */
    private void sendEmailNotification(DelegateExecution execution,List<String> toEmails,String taskId) {
        String toAddress = CollectionUtils.isNotEmpty(toEmails) ? StringUtils.join(toEmails,",") : null;
        if(StringUtils.isNotEmpty(toAddress)) {
            Map<String, Object> emailAttributes = new HashMap<>();
            emailAttributes.put("email_to", toAddress);
            emailAttributes.put("category", getCategory(execution));
            emailAttributes.put("name",getDefaultAddresseName());
            emailAttributes.put("taskid",taskId);
            if(StringUtils.isNotBlank(toAddress) && StringUtils.indexOf(toAddress,"@") > 0) {
                sendMessage(execution, emailAttributes,getMessageId(execution));
            }
        }
    }
    private String getCategory(DelegateExecution delegateExecution){
        return String.valueOf(this.category.getValue(delegateExecution));
    }

    /**
     *
     * @param delegateExecution
     * @return
     */
    private String getMessageId(DelegateExecution delegateExecution){
        return String.valueOf(this.messageId.getValue(delegateExecution));
    }

    private List<String> getEmailGroups(DelegateExecution delegateExecution){
        List<String> emailGroups = new ArrayList<>();
        try {
            if(this.emailGroups != null &&
                    StringUtils.isNotBlank(String.valueOf(this.emailGroups.getValue(delegateExecution)))) {
                emailGroups = this.emailGroups != null && this.emailGroups.getValue(delegateExecution) != null ?
                        getObjectMapper().readValue(String.valueOf(this.emailGroups.getValue(delegateExecution)), List.class) : null;
            }
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Exception occured in reading additionalEmailGroups" , e);
        }
        return  emailGroups;
    }

    /**
     * Returns Object Mapper Instance
     * @return
     */
    private ObjectMapper getObjectMapper(){
        return new ObjectMapper();
    }


}
