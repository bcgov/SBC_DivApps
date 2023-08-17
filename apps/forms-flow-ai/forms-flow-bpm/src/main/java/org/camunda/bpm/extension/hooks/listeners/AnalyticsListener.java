package org.camunda.bpm.extension.hooks.listeners;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.StringValue;
import org.camunda.bpm.extension.hooks.services.IMessageEvent;
import org.camunda.bpm.extension.hooks.services.analytics.IDataPipeline;
import org.camunda.bpm.extension.hooks.services.analytics.SimpleDBDataPipeline;
import org.glassfish.jersey.internal.util.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.camunda.bpm.extension.hooks.services.UserService;

import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java component for pushing the data to downstream analytics system.
 * This invokes the database pipeline component for publishing data.
 *
 * Futuristic place-holder to inject the appropriate pipeline using service
 * location pattern.
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
@Named("analyticsDelegate")
public class AnalyticsListener implements TaskListener, ExecutionListener, IMessageEvent {

    @Autowired
    private SimpleDBDataPipeline dbdatapipeline;

    @Autowired
    private UserService userService;

    private final Logger LOGGER = Logger.getLogger(AnalyticsListener.class.getName());

    /**
     * This would be invoked by all the user based task services.
     * Invocation point is marked with value "COMPLETE" in process diagram.
     * 
     * @param task
     */
    @Override
    public void notify(DelegateTask task) {
        LOGGER.info("\n\n  ... AnalyticsDelegate invoked by task listener for "
                + "processDefinitionId=" + task.getProcessDefinitionId()
                + ", assignee=" + task.getAssignee()
                + ", executionId=" + task.getId()
                + ", variables=" + task.getVariables()
                + " \n\n");
        DelegateExecution execution = task.getExecution();
        try {
            Map<String, Object> rspVariableMap = dbdatapipeline
                    .execute(injectAdditionalProcessingFields(execution, execution.getVariables()));
            if (IDataPipeline.ResponseStatus.FAILURE.name().equals(rspVariableMap.get("code"))) {
                notifyForAttention(execution, rspVariableMap);
            }
        } catch (Exception e) {
            LOGGER.warning("Unable to insert record into analytics DB");
            notifyForAttention(execution, e);
            throw e; // Throw the exception so that the instance is available in the cockpit.
        }
    }

    /**
     * This would be invoked during form submission.
     * Invocation point is marked with value "END" in process diagram.
     * 
     * @param execution
     * @throws Exception
     */
    @Override
    public void notify(DelegateExecution execution) throws Exception {
        LOGGER.info("\n\n  ... AnalyticsDelegate invoked by execution listener for"
                + "processDefinitionId=" + execution.getProcessDefinitionId()
                + ", executionId=" + execution.getId()
                + ", variables=" + execution.getVariables()
                + " \n\n");
        try {
            Map<String, Object> rspVariableMap = dbdatapipeline
                    .execute(injectAdditionalProcessingFields(execution, execution.getVariables()));
            if (IDataPipeline.ResponseStatus.FAILURE.name().equals(rspVariableMap.get("code"))) {
                notifyForAttention(execution, rspVariableMap);
            }
        } catch (Exception e) {
            LOGGER.warning("Unable to insert record into analytics DB");
            notifyForAttention(execution, e);
            throw e; // Throw the exception so that the instance is available in the cockpit.
        }
    }

    /**
     * This method is intended to inject additional fields for processing.
     * Injected Fields : File Name, MimeType, Size, Pid.
     * 
     * @param execution
     * @param rawMap
     * @return
     */
    private Map<String, Object> injectAdditionalProcessingFields(DelegateExecution execution,
            Map<String, Object> rawMap) {
        Map<String, Object> variables = injectPrimaryKey(execution, rawMap);
        Map<String, Object> prcMap = new HashMap<>();
        String pid = execution.getId();
        try {
            // Handles file & authenticated user information.
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                if (entry.getKey().endsWith("_idir")) {
                    String idir = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
                    if (StringUtils.isNotEmpty(idir)) {
                        prcMap.put(entry.getKey(), entry.getValue());
                        if (!execution.getVariables()
                                .containsKey(StringUtils.substringBefore(entry.getKey(), "_idir").concat("_name"))) {
                            String idirName = getName(execution, userService,
                                    variables.get("provider_idir_userid").toString());
                            execution.setVariable(StringUtils.substringBefore(entry.getKey(), "_idir").concat("_name"),
                                    idirName);
                            prcMap.put(StringUtils.substringBefore(entry.getKey(), "_idir").concat("_name"), idirName);
                        }
                    }
                } else if (!StringUtils.endsWith(entry.getKey(), "_file")) {
                    LOGGER.info("ANALYTICS_VARIABLES"
                            + "key=" + entry.getKey()
                            + ", value=" + entry.getValue());
                    prcMap.put(entry.getKey(), entry.getValue());
                }
                // Commenting out the file handling for analytics listener.
                /**
                 * if(StringUtils.endsWith(entry.getKey(),"_file")) {
                 * if(!execution.getVariables().containsKey(StringUtils.substringBefore(entry.getKey(),"_file").concat("_stream_id")))
                 * {
                 * String filePrefix = StringUtils.substringBefore(entry.getKey(), "_file");
                 * FileValue retrievedTypedFileValue =
                 * execution.getProcessEngineServices().getRuntimeService().getVariableTyped(pid,
                 * entry.getKey());
                 * if (retrievedTypedFileValue != null && retrievedTypedFileValue.getValue() !=
                 * null) {
                 * InputStream fileContent = retrievedTypedFileValue.getValue();
                 * String fileName = retrievedTypedFileValue.getFilename();
                 * String mimeType = retrievedTypedFileValue.getMimeType();
                 * byte[] fileBytes = IOUtils.toByteArray(fileContent);
                 * int fileSize = fileBytes.length;
                 * if (StringUtils.isNotEmpty(fileName) && fileSize > 0) {
                 * prcMap.put(filePrefix.concat("_name"), fileName);
                 * prcMap.put(filePrefix.concat("_mimetype"), mimeType);
                 * prcMap.put(entry.getKey(), fileBytes);
                 * prcMap.put(filePrefix.concat("_size"), fileSize);
                 * String fileId = getUniqueIdentifierForFile();
                 * prcMap.put(filePrefix.concat("_stream_id"), fileId);
                 * execution.setVariable(filePrefix.concat("_stream_id"), fileId);
                 * }
                 * }
                 * }
                 * } else if(entry.getKey().endsWith("_idir")) {
                 * String idir = entry.getValue() != null ? String.valueOf(entry.getValue()) :
                 * null;
                 * if (StringUtils.isNotEmpty(idir) &&
                 * !execution.getVariables().containsKey(StringUtils.substringBefore(entry.getKey(),
                 * "_idir").concat("_name"))) {
                 * String idirName = getName(execution, idir);
                 * execution.setVariable(StringUtils.substringBefore(entry.getKey(),
                 * "_idir").concat("_name"), idirName);
                 * prcMap.put(entry.getKey(),entry.getValue());
                 * prcMap.put(StringUtils.substringBefore(entry.getKey(),
                 * "_idir").concat("_name"),idirName);
                 * }
                 * } else {
                 * prcMap.put(entry.getKey(),entry.getValue());
                 * }
                 */
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return prcMap;
    }

    /**
     * Evaluate this being injected from process diagram.
     * 
     * @return
     */
    private Map<String, Object> injectPrimaryKey(DelegateExecution execution, Map<String, Object> variables) {
        if (execution.getVariables().containsKey("feature_by")
                && "task".equals(String.valueOf(execution.getVariable("feature_by")))) {
            variables.put("pid", execution.getVariable("pid"));
        } else {
            if (!variables.containsKey("pid")) {
                variables.put("pid", execution.getProcessInstanceId());
            }
        }
        return variables;
    }

    private void notifyForAttention(DelegateExecution execution, Exception exception) {
        Map<String, Object> variables = new HashMap<>();
        try {
            Map<String, Object> exVarMap = new HashMap<>();
            // Additional Response Fields - BEGIN
            exVarMap.put("pid", execution.getId());
            exVarMap.put("subject", ("Exception for ".concat(execution.getId())));
            exVarMap.put("category", "analytics_service_exception");
            // Additional Response Fields - END
            sendMessage(execution, exVarMap, getMessageName());
            LOGGER.info("\n\nMessage sent! " + "\n\n");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception occurred:" + ExceptionUtils.exceptionStackTraceAsString(ex));
        }
    }

    private void notifyForAttention(DelegateExecution execution, Map<String, Object> rspVariableMap) {
        if (IDataPipeline.ResponseStatus.FAILURE.name().equals(rspVariableMap.get("code"))) {
            Map<String, Object> exVarMap = new HashMap<>();
            // Additional Response Fields - BEGIN
            exVarMap.put("pid", execution.getId());
            exVarMap.put("subject",
                    (String.valueOf(rspVariableMap.get("message")).concat(" for ").concat(execution.getId())));
            exVarMap.put("category", "analytics_service_exception");
            for (Map.Entry<String, Object> entry : rspVariableMap.entrySet()) {
                if (StringUtils.startsWith(entry.getKey(), "exception")) {
                    StringValue exceptionDataValue = Variables
                            .stringValue(String.valueOf(rspVariableMap.get("exception")), true);
                    exVarMap.put(entry.getKey(), exceptionDataValue);
                }
            }
            // Additional Response Fields - END
            sendMessage(execution, exVarMap, getMessageName());
            LOGGER.info("\n\nMessage sent! " + "\n\n");
        }
    }

    private String getUniqueIdentifierForFile() {
        return UUID.randomUUID().toString();
    }

    private String getMessageName() {
        return "Service_Api_Message_Email";
    }
}
