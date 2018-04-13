package org.activit.cloud.services.metadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class MetadataService {

    private MetadataProperties metadataProperties;

    private final String servicePrefix = "activiti-cloud-service-";
    private final String applicationPrefix = "activiti-cloud-application-";

    @Autowired
    public MetadataService(MetadataProperties metadataProperties){
        this.metadataProperties = metadataProperties;
    }

    public String getKeyAsMetaData(String key, String keyPrefix){
        return keyPrefix+key.replace(".","-");
    }

    public Map<String,String> getMetadata(){
        Map<String,String> metadata = new HashMap<>();

        Iterator<String> applicationIterator =  metadataProperties.getApplication().keySet().iterator();
        while(applicationIterator.hasNext()){
            String key = applicationIterator.next();
            metadata.put(getKeyAsMetaData(key,applicationPrefix),metadataProperties.getApplication().get(key));
        }

        Iterator<String> serviceIterator = metadataProperties.getService().keySet().iterator();
        while(serviceIterator.hasNext()){
            String key = serviceIterator.next();
            metadata.put(getKeyAsMetaData(key,servicePrefix),metadataProperties.getApplication().get(key));
        }
        return metadata;
    }
}
