package org.camunda.bpm.extension.hooks.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * This class is intended to perform the data transformation from different source systems.
 * Supported sources : Orbeon
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
@RestController
@RequestMapping("/form-builder")
//@WebServlet(name = "FormBuilderPipelineController", urlPatterns = "/form-builder/*",loadOnStartup = 1)
public class FormBuilderPipelineController extends HttpServlet {

    private final Logger LOGGER = Logger.getLogger(FormBuilderPipelineController.class.getName());


    @PostMapping(value = "/orbeon/data",consumes = MediaType.APPLICATION_XML_VALUE)
    public void createProcess(HttpServletRequest request) {
        LOGGER.info("Inside Data transformation controller" +request.getParameterMap());
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        String inputxml = null;
        try(InputStream is = request.getInputStream();BufferedInputStream bis = new BufferedInputStream(is)) {
            byte[] xmlData = new byte[request.getContentLength()];
            bis.read(xmlData, 0, xmlData.length);
            if (request.getCharacterEncoding() != null) {
                inputxml = new String(xmlData, request.getCharacterEncoding());
            } else {
                inputxml = new String(xmlData);
            }
            LOGGER.info("XML Document-------->"+inputxml);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }




    @Override
    protected void doPost (HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        LOGGER.info("Inside Data transformation controller" );
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            // use the factory to create a documentbuilder
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(req.getInputStream());
            LOGGER.info("input-------->"+doc);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
