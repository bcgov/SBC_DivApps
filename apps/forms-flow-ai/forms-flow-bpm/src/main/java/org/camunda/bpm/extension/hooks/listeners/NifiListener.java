package org.camunda.bpm.extension.hooks.listeners;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.extension.hooks.services.FormSubmissionService;
import org.glassfish.jersey.internal.util.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
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
    private WebClient webClient;

    @Autowired
    private FormSubmissionService formSubmissionService;

    @Override
    public void notify(DelegateExecution execution) {
        try {
            postToNiFi(execution);
        } catch (Exception e) {
            handleException(execution, BaseListener.ExceptionSource.EXECUTION, e);
            notifyForAttention(execution);
            execution.createIncident("Nifi-HTTP", execution.getId(), "Unable to connect with Nifi");
        }
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            postToNiFi(delegateTask.getExecution());
        } catch (Exception e) {
            handleException(delegateTask.getExecution(), BaseListener.ExceptionSource.TASK, e);
            notifyForAttention(delegateTask.getExecution());
            delegateTask.getExecution().createIncident("Nifi-HTTP", "", "Unable to connect with Nifi");
        }
    }

    private void postToNiFi(DelegateExecution execution) throws Exception {
        Mono<ResponseEntity<String>> entityMono = webClient.method(HttpMethod.POST).uri(getNifiURL())
                .accept(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(formSubmissionService.createFormSubmissionData(execution.getVariables())), String.class)
                .retrieve()
                .toEntity(String.class);

        ResponseEntity<String> response = entityMono.block();
        if(response != null && response.getStatusCodeValue() != HttpStatus.OK.value()) {
            notifyForAttention(execution);
        }
    }

    private Map<String,Object> injectAdditionalProcessingFields(DelegateExecution execution,Map<String,Object> rawMap) {
        Map<String,Object> variables = injectPrimaryKey(execution,rawMap);
        Map<String,Object> prcMap = new HashMap<>();
        String pid = execution.getId();
        try {
            // Handles file & authenticated user information.
            for(Map.Entry<String,Object> entry : variables.entrySet()) {
                if(StringUtils.endsWith(entry.getKey(),"_file")) {
                    if(!execution.getVariables().containsKey(StringUtils.substringBefore(entry.getKey(),"_file").concat("_stream_id"))) {
                        String filePrefix = StringUtils.substringBefore(entry.getKey(), "_file");
                        FileValue retrievedTypedFileValue = execution.getProcessEngineServices().getRuntimeService().getVariableTyped(pid, entry.getKey());
                        if (retrievedTypedFileValue != null && retrievedTypedFileValue.getValue() != null) {
                            InputStream fileContent = retrievedTypedFileValue.getValue();
                            String fileName = retrievedTypedFileValue.getFilename();
                            String mimeType = retrievedTypedFileValue.getMimeType();
                            byte[] fileBytes = IOUtils.toByteArray(fileContent);
                            int fileSize = fileBytes.length;
                            if (StringUtils.isNotEmpty(fileName) && fileSize > 0) {
                                prcMap.put(filePrefix.concat("_name"), fileName);
                                prcMap.put(filePrefix.concat("_mimetype"), mimeType);
                                prcMap.put(entry.getKey(), fileBytes);
                                prcMap.put(filePrefix.concat("_size"), fileSize);
                                String fileId = getUniqueIdentifierForFile();
                                prcMap.put(filePrefix.concat("_stream_id"), fileId);
                                execution.setVariable(filePrefix.concat("_stream_id"), fileId);
                            }
                        }
                    }
                } else if(entry.getKey().endsWith("_idir")) {
                    String idir = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
                    if (StringUtils.isNotEmpty(idir) &&
                            !execution.getVariables().containsKey(StringUtils.substringBefore(entry.getKey(), "_idir").concat("_name"))) {
                        String idirName = getName(execution, idir);
                        execution.setVariable(StringUtils.substringBefore(entry.getKey(), "_idir").concat("_name"), idirName);
                        prcMap.put(entry.getKey(),entry.getValue());
                        prcMap.put(StringUtils.substringBefore(entry.getKey(), "_idir").concat("_name"),idirName);
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

    private Map<String,Object> injectPrimaryKey(DelegateExecution execution,Map<String,Object> variables) {
        if(execution.getVariables().containsKey("feature_by") && "task".equals(String.valueOf(execution.getVariable("feature_by")))) {
            variables.put("pid",execution.getVariable("pid"));
        } else {
            if(!variables.containsKey("pid")) {
                variables.put("pid", execution.getProcessInstanceId());
            }
        }
        return variables;
    }

    private String getUniqueIdentifierForFile() {
        return UUID.randomUUID().toString();
    }

    private String getNifiURL(){
        return clientCredentialProperties.getProperty("analytics.nifi.url");
    }

    private String getAccessToken(){
        return clientCredentialProperties.getProperty("analytics.nifi.accesstoken");
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
