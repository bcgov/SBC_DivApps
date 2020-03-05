package org.camunda.bpm.extension.hooks.services;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.identity.User;

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
        Map<String,Object> eMessageVariables = new HashMap<>();
        eMessageVariables.putAll(messageVariables);
        eMessageVariables.putAll(injectFormDataInLightMode(execution));
        runtimeService.startProcessInstanceByMessage("Message_Email",eMessageVariables);
        log.info("\n\nMessage sent! " + "\n\n");
    }

    default void sendMessage(DelegateTask task, String category){
        // Get user profile
        IdentityService identityService = task.getProcessEngineServices().getIdentityService();
        User user = identityService.createUserQuery().userId(task.getAssignee()).singleResult();
        Map<String,Object> messageVariables = new HashMap<>();
        messageVariables.put("taskid",task.getId());
        messageVariables.put("category",category);
      if (user != null && StringUtils.isNotEmpty(user.getEmail())) {
          messageVariables.put("firstname",user.getFirstName());
          messageVariables.put("lastname",user.getLastName());
          messageVariables.put("to",user.getEmail());
          sendMessage(task.getExecution(), messageVariables);
        }
    }

    default Map<String,Object> injectFormDataInLightMode(DelegateExecution execution) {
        Map<String,Object> formMap = new HashMap<>();
        for(Map.Entry<String,Object> entry : execution.getVariables().entrySet()) {
                if(!StringUtils.endsWithIgnoreCase(entry.getKey(),"_file")) {
                    formMap.put(entry.getKey(),entry.getValue());
                }
        }
        return formMap;
    }

}
