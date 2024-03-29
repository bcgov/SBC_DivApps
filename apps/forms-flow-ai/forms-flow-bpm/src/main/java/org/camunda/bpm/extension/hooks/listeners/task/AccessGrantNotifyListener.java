package org.camunda.bpm.extension.hooks.listeners.task;

import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.extension.hooks.services.IMessageEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This component is aimed at sending notification to Access Groups
 *
 * @author  sumathi.thirumani@aot-technologies.com
 */
@Component
public class AccessGrantNotifyListener implements TaskListener, IMessageEvent {

    private Expression excludeGroup;
    private Expression messageId;


    private static final Logger LOGGER = Logger.getLogger(AccessGrantNotifyListener.class.getName());

    private Expression category;

    /**
     * This provides the necessary information to send message.
     *
     * @param delegateTask: The task which sends the message
     */
    public void notify(DelegateTask delegateTask) {
        LOGGER.info("\n\nAccessGrantNotify listener invoked! " + delegateTask.getId());
        List<String> notifyGroup = new ArrayList<>();
        String excludeGroupValue = this.excludeGroup != null && this.excludeGroup.getValue(delegateTask.getExecution()) != null ?
                String.valueOf(this.excludeGroup.getValue(delegateTask.getExecution())) : null;
        List<String> exclusionGroupList = new ArrayList<>();
        LOGGER.info("Excluded group::" + excludeGroupValue);
        if(StringUtils.isNotBlank(excludeGroupValue)) {
            exclusionGroupList.add(excludeGroupValue.trim());
        }
        List<String> accessGroupList = getModifiedGroupsForTask(delegateTask, exclusionGroupList);
        String accessGroupListString = String.join("|",accessGroupList);
        for (String entry : accessGroupList) {
            List<String> emailsForGroup = getEmailsForGroup(delegateTask.getExecution(), entry);
            notifyGroup.addAll(emailsForGroup);
        }
        if (isNotify(delegateTask) && StringUtils.isBlank(delegateTask.getAssignee())) {
            if (CollectionUtils.isNotEmpty(notifyGroup)) {
                LOGGER.info("Sending an email to ::" + accessGroupListString);
                sendEmailNotification(delegateTask.getExecution(), notifyGroup, delegateTask.getId(), getCategory(delegateTask.getExecution()));
                delegateTask.getExecution().removeVariable("isNotify");
            }
        }
    }

    /**
     * Check if the current update event is a result of Notify action
     * @param delegateTask The current task in context
     * @return true - if the update event is a result of Notify; false - else
     */
    private boolean isNotify(DelegateTask delegateTask) {
        Object shouldSendEmail = delegateTask.getExecution().getVariable("isNotify");
        return shouldSendEmail != null && (boolean) shouldSendEmail;
    }

    /**
     * Sends an email.
     * @param execution The current execution instance.
     * @param toEmails The recipients.
     * @param taskId The task id.
     * @param category The email category for the DMN.
     */
    private void sendEmailNotification(DelegateExecution execution, List<String> toEmails, String taskId, String category) {
        Set<String> emails = new HashSet<>(toEmails);
        String toAddress = CollectionUtils.isNotEmpty(toEmails) ? StringUtils.join(emails,",") : null;
        if(StringUtils.isNotEmpty(toAddress)) {
            Map<String, Object> emailAttributes = new HashMap<>();
            emailAttributes.put("to", toAddress);
            emailAttributes.put("category", category);
            emailAttributes.put("name",getDefaultAddresseName());
            emailAttributes.put("taskid",taskId);
            log.info("Inside notify attributes:" + emailAttributes);
            if(StringUtils.isNotBlank(toAddress) && StringUtils.indexOf(toAddress,"@") > 0) {
                sendMessage(execution, emailAttributes,getMessageId(execution));
            }
        }
    }

    /**
     * @param execution The current execution instance
     * @return Returns the message category
     */
    private String getCategory(DelegateExecution execution){
        return String.valueOf(this.category.getValue(execution));
    }

    /**
     * @param delegateTask The task instance to send an email for.
     * @param exclusionGroup The groups to be excluded from the emails.
     * @return The list of groups after removing the excluded groups.
     */
    private List<String> getModifiedGroupsForTask(DelegateTask delegateTask, List<String> exclusionGroup) {
        Set<IdentityLink> identityLinks = delegateTask.getCandidates();
        List<String> newGroupsAdded = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(identityLinks)) {
            for (IdentityLink entry : identityLinks) {
                String grpId = entry.getGroupId().trim();
                if (!exclusionGroup.contains(grpId)) {
                    newGroupsAdded.add(entry.getGroupId().trim());
                }
            }
        }
        return newGroupsAdded;
    }

    /**
     * Get the name of the variable that keeps track of who the email has been sent to previously
     * @param delegateTask The current task
     * @return The variable name
     */
    private String getTrackVariable(DelegateTask delegateTask) {
        return delegateTask.getTaskDefinitionKey()+"_notify_sent_to";
    }

    private String getMessageId(DelegateExecution execution){
        return String.valueOf(this.messageId.getValue(execution));
    }
}
