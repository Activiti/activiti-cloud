package org.activiti.cloud.services.modeling.rest.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FileSizeException extends RuntimeException{
    public FileSizeException(String message){
        super(message);
    }
}
