package org.camunda.bpm.extension.hooks.services;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Interface containing method to send message to start a message event
 *
 * @author yichun.zhao@aot-technologies.com
 */
public interface IMessageEvent {

    Logger log = Logger.getLogger(IMessageEvent.class.getName());


    default void sendMessage(DelegateExecution execution, Map<String,Object> messageVariables){
        RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();
        runtimeService.startProcessInstanceByMessage("Message_Email",messageVariables);
        log.info("\n\nMessage sent! " + "\n\n");
    }

    default void sendMessage(DelegateTask task, String category){
        // Get user profile
        IdentityService identityService = task.getProcessEngineServices().getIdentityService();
        User user = identityService.createUserQuery().userId(task.getAssignee()).singleResult();
        Map<String,Object> messageVariables = new HashMap<>();
        messageVariables.put("pid",task.getExecution().getId());
        messageVariables.put("taskid",task.getId());
        messageVariables.put("category",category);
      if (user != null && StringUtils.isNotEmpty(user.getEmail())) {
            messageVariables.put("firstname",user.getFirstName());
            messageVariables.put("lastname",user.getLastName());
            messageVariables.put("to",user.getEmail());
            sendMessage(task.getExecution(), messageVariables);
        }

    }
}
