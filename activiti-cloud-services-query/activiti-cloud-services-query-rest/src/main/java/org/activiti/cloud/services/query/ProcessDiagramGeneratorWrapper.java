/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.query;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.exceptions.XMLException;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.exception.ActivitiImageException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

/**
 * Service logic for generating process diagrams
 */
public class ProcessDiagramGeneratorWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDiagramGeneratorWrapper.class);

    private final ProcessDiagramGenerator processDiagramGenerator;

    @Value("${activiti.diagram.activity.font:}")
    private String activityFontName;

    @Value("${activiti.diagram.label.font:}")
    private String labelFontName;

    @Value("${activiti.diagram.annotation.font:}")
    private String annotationFontName;

    @Value("${activiti.diagram.default.image.file:}")
    private String defaultDiagramImageFileName;

    @Value("${activiti.diagram.generate.default:true}")
    private boolean generateDefaultDiagram;

    @Value("${activiti.diagram.font.default:Serif}")
    private String diagramDefaultFont;
    
    @Autowired
    public ProcessDiagramGeneratorWrapper(ProcessDiagramGenerator processDiagramGenerator) {
        this.processDiagramGenerator = processDiagramGenerator;
    }

    /**
     * Generate the diagram for a BPNM model
     * @param bpmnModel the BPNM model
     * @return the diagram for the given model
     */
    public byte[] generateDiagram(BpmnModel bpmnModel) {
        return generateDiagram(bpmnModel,
                               emptyList(),
                               emptyList());
    }

    /**
     * Generate the diagram for a BPNM model
     * @param bpmnModel the BPNM model
     * @param highLightedActivities the activity ids to highlight in diagram
     * @param highLightedFlows the flow ids to highlight in diagram
     * @return the diagram for the given model
     */
    public byte[] generateDiagram(BpmnModel bpmnModel,
                                  List<String> highLightedActivities,
                                  List<String> highLightedFlows) {
        try (final InputStream imageStream = processDiagramGenerator.generateDiagram(bpmnModel,
                                                                                     highLightedActivities,
                                                                                     highLightedFlows,
                                                                                     getActivityFontName(),
                                                                                     getLabelFontName(),
                                                                                     getAnnotationFontName(),
                                                                                     isGenerateDefaultDiagram(),
                                                                                     getDiagramImageFileName())) {
            return IOUtils.toByteArray(imageStream);
        } catch (ActivitiImageException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting process diagram for model: " + bpmnModel,
                                        e);
        }
    }

    public boolean isGenerateDefaultDiagram() {
        return generateDefaultDiagram;
    }

    public String getDefaultDiagramImageFileName() {
        return defaultDiagramImageFileName;
    }

    /**
     * Get diagram file name to use when there is no diagram graphic info inside model.
     * @return the file name
     */
    public String getDiagramImageFileName() {
        return !StringUtils.isEmpty(getDefaultDiagramImageFileName()) ?
                getDefaultDiagramImageFileName() :
                processDiagramGenerator.getDefaultDiagramImageFileName();
    }

    /**
     * Get activity font name
     * @return the activity font name
     */
    public String getActivityFontName() {
        return isFontAvailable(activityFontName) ?
                activityFontName :
                getDiagramDefaultFont();
    }

    /**
     * Get label font name
     * @return the label font name
     */
    public String getLabelFontName() {
        return isFontAvailable(labelFontName) ?
                labelFontName :
                getDiagramDefaultFont();
    }

    /**
     * Get annotation font name
     * @return the annotation font name
     */
    public String getAnnotationFontName() {
        return isFontAvailable(annotationFontName) ?
                annotationFontName :
                getDiagramDefaultFont();
    }

    /**
     * Check if a given font is available in the current system
     * @param fontName the font name to check
     * @return true if the specified font name exists
     */
    private boolean isFontAvailable(String fontName) {
        if (StringUtils.isEmpty(fontName)) {
            return false;
        }

        boolean available = Arrays
                .stream(getAvailableFonts())
                .anyMatch(availbleFontName -> availbleFontName.toLowerCase().startsWith(fontName.toLowerCase()));

        if (!available) {
            LOGGER.debug("Font not available while generating process diagram: " + fontName);
        }

        return available;
    }

    protected String[] getAvailableFonts() {
        return getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    }

    
    public String getDiagramDefaultFont() {
        return diagramDefaultFont;
    }

    
    public void setDiagramDefaultFont(String diagramDefaultFont) {
        this.diagramDefaultFont = diagramDefaultFont;
    }
    
    public BpmnModel parseBpmnModelXml(InputStream inputStream) {
        InputStreamReader in = null;

        try {
            in = new InputStreamReader(inputStream);

            XMLInputFactory xif = XMLInputFactory.newInstance();

            if (xif.isPropertySupported(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES)) {
                xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
            }

            if (xif.isPropertySupported(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)) {
                xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            }

            if (xif.isPropertySupported(XMLInputFactory.SUPPORT_DTD)) {
                xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            }

            XMLStreamReader xtr = xif.createXMLStreamReader(in);

            return new BpmnXMLConverter().convertToBpmnModel(xtr);
            
        } catch (XMLStreamException e) {
            throw new XMLException("Error while reading the BPMN 2.0 XML", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // LOGGER.debug("Problem closing BPMN input stream", e);
                }
            }
        }
        
    }
    
}
