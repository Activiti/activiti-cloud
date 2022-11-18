/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.core;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static java.util.Collections.emptyList;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.ActivitiException;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.exception.ActivitiImageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StreamUtils;
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

    @Value("${activiti.diagram.generate.default:false}")
    private boolean generateDefaultDiagram;

    public ProcessDiagramGeneratorWrapper(ProcessDiagramGenerator processDiagramGenerator) {
        this.processDiagramGenerator = processDiagramGenerator;
    }

    /**
     * Generate the diagram for a BPNM model
     * @param bpmnModel the BPNM model
     * @return the diagram for the given model
     */
    public byte[] generateDiagram(BpmnModel bpmnModel) {
        return generateDiagram(bpmnModel, emptyList(), emptyList(), emptyList());
    }

    /**
     * Generate the diagram for a BPNM model
     * @param bpmnModel the BPNM model
     * @param highLightedActivities the activity ids to highlight in diagram
     * @param highLightedFlows the flow ids to highlight in diagram
     * @return the diagram for the given model
     */
    public byte[] generateDiagram(
        BpmnModel bpmnModel,
        List<String> highLightedActivities,
        List<String> highLightedFlows,
        List<String> currentActivities
    ) {
        try (
            final InputStream imageStream = processDiagramGenerator.generateDiagram(
                bpmnModel,
                highLightedActivities,
                highLightedFlows,
                currentActivities,
                emptyList(),
                getActivityFontName(),
                getLabelFontName(),
                getAnnotationFontName(),
                isGenerateDefaultDiagram(),
                getDiagramImageFileName()
            )
        ) {
            return StreamUtils.copyToByteArray(imageStream);
        } catch (ActivitiImageException e) {
            throw e;
        } catch (Exception e) {
            throw new ActivitiException("Error occurred while getting process diagram for model: " + bpmnModel, e);
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
        return !StringUtils.isEmpty(getDefaultDiagramImageFileName())
            ? getDefaultDiagramImageFileName()
            : processDiagramGenerator.getDefaultDiagramImageFileName();
    }

    /**
     * Get activity font name
     * @return the activity font name
     */
    public String getActivityFontName() {
        return isFontAvailable(activityFontName)
            ? activityFontName
            : processDiagramGenerator.getDefaultActivityFontName();
    }

    /**
     * Get label font name
     * @return the label font name
     */
    public String getLabelFontName() {
        return isFontAvailable(labelFontName) ? labelFontName : processDiagramGenerator.getDefaultLabelFontName();
    }

    /**
     * Get annotation font name
     * @return the annotation font name
     */
    public String getAnnotationFontName() {
        return isFontAvailable(annotationFontName)
            ? annotationFontName
            : processDiagramGenerator.getDefaultAnnotationFontName();
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
}
