package org.camunda.bpm.extension.hooks.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.StringValue;
import org.glassfish.jersey.internal.util.ExceptionUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is intended to perform the data transformation from different source systems.
 * Supported sources : Orbeon - Customer Feedback Form
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
@RestController
@RequestMapping("/form-builder")
public class FormBuilderPipelineController {

    private final Logger LOGGER = Logger.getLogger(FormBuilderPipelineController.class.getName());

    @Value("${formbuilder.pipeline.service.bpm-url}")
    private String appcontexturl;

    @Autowired
    private Properties clientCredentialProperties;

    /**
     * Creates a camunda process instance for the orbeon form data given.
     * @param request The request object containing the CCII form data.
     */
    @PostMapping(value = "/orbeon/data",consumes = MediaType.APPLICATION_XML_VALUE)
    public void createProcess(HttpServletRequest request) {
        LOGGER.info("Inside Data transformation controller" +request.getParameterMap());
        String formXML = null;
        try(InputStream is = request.getInputStream();BufferedInputStream bis = new BufferedInputStream(is)) {
            byte[] xmlData = new byte[request.getContentLength()];
            bis.read(xmlData, 0, xmlData.length);
            if (request.getCharacterEncoding() != null) {
                formXML = new String(xmlData, request.getCharacterEncoding());
            } else {
                formXML = new String(xmlData);
            }
            LOGGER.info("Received XML Document-------->"+formXML);
            Map<String,Object> processVariables = prepareRequestVariableMap(formXML);
            for (String key: processVariables.keySet()) {
                if(StringUtils.endsWith(key,"_date") || StringUtils.endsWith(key,"_date_time")) {
                    VariableData valueVariableData = (VariableData) processVariables.get(key);
                    if(!isDateValid((String) valueVariableData.getValue())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The value for " + key + " is invalid");
                    }
                }
            }
            Boolean status = createProcessInstance(processVariables);
            if(status == false) {
                //Email the form to support group for manual processing
                sendEmail(formXML,request.getParameter("document"),null);
                LOGGER.log(Level.SEVERE,"Unable to create process instance");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create process instance");
            }
        } catch (IOException exception) {
            sendEmail(formXML,request.getParameter("document"), null);
            LOGGER.log(Level.SEVERE,"Unable to parse the XML from orbeon");
            LOGGER.log(Level.SEVERE,"Exception occurred:"+ ExceptionUtils.exceptionStackTraceAsString(exception));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to parse the XML from orbeon");
        } catch (ResponseStatusException exception) {
            sendEmail(formXML,request.getParameter("document"), null);
            LOGGER.log(Level.SEVERE,exception.getMessage());
            LOGGER.log(Level.SEVERE,"Exception occurred:"+ ExceptionUtils.exceptionStackTraceAsString(exception));
            throw exception;
        } catch (Exception ex) {
            sendEmail(formXML,request.getParameter("document"), null);
            LOGGER.log(Level.SEVERE, ex.getMessage());
            LOGGER.log(Level.SEVERE,"Exception occurred:"+ ExceptionUtils.exceptionStackTraceAsString(ex));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    private void sendEmail(String formXML,String documentId, String exceptionTrace){
        Map<String,Object> variables = new HashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            ObjectMapper mapper = new ObjectMapper();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + getOAuth2RestTemplate().getAccessToken());
            CreateProcessMessageRequest msgRequest = new CreateProcessMessageRequest();
            variables.put("category", new VariableData("api_start_failure"));
            variables.put("orbeon_document_id", new VariableData(documentId));
            variables.put("formXML", new VariableData(formXML));
            //Include exception if any
            if(StringUtils.isNotBlank(exceptionTrace)) {
                StringValue exceptionDataValue = Variables.stringValue(exceptionTrace,true);
                variables.put("exception", exceptionDataValue);
            }
            msgRequest.setMessageName("Service_Api_Message_Email");
            msgRequest.setProcessVariables(variables);
            HttpEntity<String> msgReq = new HttpEntity<String>(mapper.writeValueAsString(msgRequest), headers);
            ResponseEntity<String> msgResponse = getOAuth2RestTemplate().postForEntity(
                    getAPIContextURL() + "/engine-rest/message", msgReq, String.class);
            LOGGER.info("Message response code:"+msgResponse.getStatusCode());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE,"Exception occurred:"+ExceptionUtils.exceptionStackTraceAsString(ex));
        }
    }

    private boolean isDateValid(String dateStr) {
        if (dateStr == null) {
            return false;
        }
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            simpleDateFormat.setLenient(false);
            simpleDateFormat.parse(dateStr);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    private Boolean createProcessInstance(Map<String,Object> processVariables) throws JsonProcessingException {
        //HTTP Headers
        HttpHeaders headers = new HttpHeaders();
        ObjectMapper mapper = new ObjectMapper();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getOAuth2RestTemplate().getAccessToken());
        CreateProcessRequest procReq = new CreateProcessRequest();
        procReq.setVariables(processVariables);
        HttpEntity<String> prcReq =
                new HttpEntity<String>(mapper.writeValueAsString(procReq), headers);

        ResponseEntity<String> wrsp = getOAuth2RestTemplate().postForEntity(
                getAPIContextURL() + "/engine-rest/process-definition/key/CC_Process/start", prcReq, String.class);
        Map<String, Object> responseMap = mapper.readValue(wrsp.getBody(), HashMap.class);
        LOGGER.info("Response Map post instance creation-------->" + responseMap);
        String instanceId = responseMap != null && responseMap.containsKey("id") ? String.valueOf(responseMap.get("id")) : null;
        if (StringUtils.isNotBlank(instanceId)) {
            return true;
        }
        return false;
    }


    private OAuth2RestTemplate getOAuth2RestTemplate() {
        ResourceOwnerPasswordResourceDetails  resourceDetails = new ResourceOwnerPasswordResourceDetails ();
        resourceDetails.setClientId(clientCredentialProperties.getProperty("registration.keycloak.client-id"));
        resourceDetails.setClientSecret(clientCredentialProperties.getProperty("registration.keycloak.client-secret"));
        resourceDetails.setAccessTokenUri(clientCredentialProperties.getProperty("registration.keycloak.token-uri"));
        LOGGER.info("token-uri::" + clientCredentialProperties.getProperty("registration.keycloak.token-uri"));
        resourceDetails.setUsername(getAPIClientUsername());
        resourceDetails.setPassword(getAPIClientPassword());
        resourceDetails.setGrantType("password");
        return new OAuth2RestTemplate(resourceDetails);
    }

    private Map<String,Object> prepareRequestVariableMap(String formXML) throws IOException {
        Map<String,Object> variables = new HashMap<>();
        if(StringUtils.isNotBlank(formXML)) {
            XmlMapper xmlMapper = new XmlMapper();
            JsonNode node = xmlMapper.readTree(formXML.getBytes());
            ObjectMapper mapper = new ObjectMapper();
            Map<String,Object> values = mapper.readValue(node.get("Main").toString(), HashMap.class);
            for(Map.Entry<String, Object> entry : values.entrySet()) {
                LOGGER.info("KEY: "+entry.getKey()+" VALUE : "+entry.getValue().toString());
                variables.put(entry.getKey(),new VariableData(entry.getValue()));
            }
            //Inject custom attributes
            variables.put("form_key", new VariableData("CCII"));
            variables.put("entity_key", new VariableData("CCII"));
            variables.put("subprocess_entity_key", new VariableData("cciiissue"));
            variables.put("files_entity_key", new VariableData("cciifiles"));
            variables.put("submit_date_time", new VariableData(new DateTime().toString()));
            variables.put("entered_by", new VariableData("orbeon"));
            VariableData serviceMethodData = (VariableData)variables.get("service_method");
            variables.put("engagement_source", new VariableData(serviceMethodData.getValue().toString()));
            VariableData serviceChannelData = (VariableData)variables.get("service_channel");
            if(serviceChannelData.getValue().toString().equals("Service BC Location")){
                variables.put("service_location_type", new VariableData("service_centre"));
            }else if(serviceChannelData.getValue().equals("Mobile Outreach Location")){
                variables.put("service_location_type", new VariableData("mobile_outreach"));
            }
            variables.put("service_channel", new VariableData("Service BC Location"));
            // Check if Orbeon is submitted with a value for "mobile-location" 
            if(variables.containsKey("mobile_location")) {
                // Set location parameter to "Mobile Outreach"
                variables.put("location", new VariableData("Mobile Outreach"));
            }
        }
        return variables;
    }

    public class CreateProcessRequest{
        Map<String,Object> variables;
        public Map<String, Object> getVariables() { return variables; }
        public void setVariables(Map<String, Object> variables) { this.variables = variables; }
    }

    public class CreateProcessMessageRequest{
        private String messageName;
        Map<String,Object> processVariables;
        public String getMessageName() { return messageName; }
        public void setMessageName(String messageName) { this.messageName = messageName; }
        public Map<String, Object> getProcessVariables() { return processVariables; }
        public void setProcessVariables(Map<String, Object> processVariables) { this.processVariables = processVariables; }
    }

    public class VariableData {
        private Object value;
        VariableData(Object value) {
            // To handle incoming boolean parameters as string values
            if(value!=null && (value.toString().toLowerCase().equals("true") || value.toString().toLowerCase().equals("false"))) {
                this.value = Boolean.parseBoolean(value.toString());
            }else {
                this.value=value;   
            }
        }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
    }

    private String getAPIClientUsername() {
        return StringUtils.substringBefore(StringUtils.substringBetween(appcontexturl,"://","@"),":");
    }

    private String getAPIClientPassword() {
        return StringUtils.substringAfter(StringUtils.substringBetween(appcontexturl,"://","@"),":");
    }

    private String getAPIContextURL() {
        return StringUtils.remove(StringUtils.remove(appcontexturl, StringUtils.substringBetween(appcontexturl,"://","@")),"@");
    }


}
