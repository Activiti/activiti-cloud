package org.activiti.cloud.services.modeling.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;
import org.activiti.cloud.services.common.file.FileContent;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileContentValidator {

    private static final Logger logger = LoggerFactory.getLogger(FileContentValidator.class);

    private List<String> executableMimeType;

    public FileContentValidator(List<String> executableMimeType) {
        this.executableMimeType = executableMimeType;
    }

    public boolean checkFileIsExecutable(byte[] fileContent) {
        try {
            String fileMimeType = detectMimeTypeUsingDetector(fileContent);
            if (executableMimeType.contains(fileMimeType)){
                logger.warn("Detected executable Mime Type: "+ fileMimeType);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.warn("Unable to detect Mime type from file",e);
            return false;
        }
    }

    private String detectMimeTypeUsingDetector(byte[] fileContent) throws IOException {
        Detector detector = new DefaultDetector();
        Metadata metadata = new Metadata();
        InputStream fileContentStream = new ByteArrayInputStream(fileContent);
        MediaType mediaType = detector.detect(fileContentStream, metadata);
        return mediaType.toString();
    }

}
