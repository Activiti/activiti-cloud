package org.activiti.cloud.services.modeling.validation;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("executable")
public class ExecutableMimeTypeProperties {

    private List<String> mimeTypes;

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(List<String> mimeTypes) {
        this.mimeTypes = mimeTypes;
    }
}
