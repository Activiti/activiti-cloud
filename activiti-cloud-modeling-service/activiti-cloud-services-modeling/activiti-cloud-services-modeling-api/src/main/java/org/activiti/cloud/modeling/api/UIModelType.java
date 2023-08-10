package org.activiti.cloud.modeling.api;

public class UIModelType extends JsonModelType {

    public static final String NAME = "UI";

    public static final String FOLDER_NAME = "ui";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getFolderName() {
        return FOLDER_NAME;
    }
}
