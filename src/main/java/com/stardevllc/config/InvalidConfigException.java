package com.stardevllc.config;

public class InvalidConfigException extends Exception {
    
    public InvalidConfigException() {}
    
    public InvalidConfigException(String msg) {
        super(msg);
    }
    
    public InvalidConfigException(Throwable cause) {
        super(cause);
    }
    
    public InvalidConfigException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
