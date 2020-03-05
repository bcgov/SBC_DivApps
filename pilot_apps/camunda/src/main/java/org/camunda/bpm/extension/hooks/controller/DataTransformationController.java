package org.camunda.bpm.extension.hooks.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

/**
 * This class is intended to perform the data transformation from different source systems.
 * Supported sources : Orbeon
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
@RestController
@RequestMapping("/form-adapter")
public class DataTransformationController {

    private final Logger LOGGER = Logger.getLogger(DataTransformationController.class.getName());

    @PostMapping("/orbeon")
    public void createProcess(@RequestBody String data) {
            LOGGER.info("Inside Data transformation controller" + data);
    }
}
