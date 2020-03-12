package org.camunda.bpm.extension.hooks.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.logging.Logger;

/**
 * This class is intended to perform the data transformation from different source systems.
 * Supported sources : Orbeon
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
@RestController
@RequestMapping("/form-builder")
public class FormBuilderPipelineController {

    private final Logger LOGGER = Logger.getLogger(FormBuilderPipelineController.class.getName());

    @PostMapping(value = "/orbeon/data",consumes = MediaType.APPLICATION_XML_VALUE)
    public void createProcess(@RequestParam Map<String, String> reqParam) {
        LOGGER.info("Inside Data transformation controller" + reqParam);
    }
}
