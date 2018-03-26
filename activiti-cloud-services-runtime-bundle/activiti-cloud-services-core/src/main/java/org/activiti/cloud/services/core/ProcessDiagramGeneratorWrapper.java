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

package org.activiti.cloud.services.core;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.ActivitiException;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static java.util.Collections.emptyList;

/**
 * Service logic for generating process diagrams
 */
@Service
public class ProcessDiagramGeneratorWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDiagramGeneratorWrapper.class);

    public static final String DEFAULT_DIAGRAM_IMAGE_FILE = "/image/na.svg";

    private final ProcessDiagramGenerator processDiagramGenerator;

    @Value("${activiti.engine.diagram.activity.font:}")
    private String activityFontName;

    @Value("${activiti.engine.diagram.label.font:}")
    private String labelFontName;

    @Value("${activiti.engine.diagram.annotation.font:}")
    private String annotationFontName;

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
        if (!hasGraphicInfo(bpmnModel)) {
            return getDefaultDiagram();
        }

        try (final InputStream imageStream = processDiagramGenerator.generateDiagram(bpmnModel,
                                                                                     highLightedActivities,
                                                                                     highLightedFlows,
                                                                                     getActivityFontName(),
                                                                                     getLabelFontName(),
                                                                                     getAnnotationFontName())) {
            return IOUtils.toByteArray(imageStream);
        } catch (Exception e) {
            throw new ActivitiException("Error occured while getting process diagram for model: " + bpmnModel,
                                        e);
        }
    }

    /**
     * Get default diagram image as bytes array
     * @return the default diagram image
     */
    protected byte[] getDefaultDiagram() {
        try (InputStream imageStream = getClass().getResourceAsStream(getDefaultDiagramImageFile())) {
            return IOUtils.toByteArray(imageStream);
        } catch (Exception e) {
            throw new ActivitiException("Error occured while getting the default diagram image",
                                        e);
        }
    }

    /**
     * Get the file name of the default diagram image
     * @return file name
     */
    protected String getDefaultDiagramImageFile() {
        return DEFAULT_DIAGRAM_IMAGE_FILE;
    }

    /**
     * Get activity font name
     * @return the activity font name
     */
    public String getActivityFontName() {
        return isFontAvailable(activityFontName) ?
                activityFontName :
                processDiagramGenerator.getDefaultActivityFontName();
    }

    /**
     * Get label font name
     * @return the label font name
     */
    public String getLabelFontName() {
        return isFontAvailable(labelFontName) ?
                labelFontName :
                processDiagramGenerator.getDefaultLabelFontName();
    }

    /**
     * Get annotation font name
     * @return the annotation font name
     */
    public String getAnnotationFontName() {
        return isFontAvailable(annotationFontName) ?
                annotationFontName :
                processDiagramGenerator.getDefaultAnnotationFontName();
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

    /**
     * Ask if a BPMN model has graphic info.
     * @param bpmnModel the model
     * @return true if the model has graphic info inside
     */
    public boolean hasGraphicInfo(final BpmnModel bpmnModel) {
        return !bpmnModel.getLocationMap().isEmpty();
    }
}
