package com.stardevllc.config;

import java.util.Collections;
import java.util.List;

public final class SectionPathData {

    private Object data;
    private List<String> comments;
    private List<String> inlineComments;

    public SectionPathData(Object data) {
        this.data = data;
        comments = Collections.emptyList();
        inlineComments = Collections.emptyList();
    }

    public Object getData() {
        return data;
    }

    public void setData(final Object data) {
        this.data = data;
    }

    public List<String> getComments() {
        return comments;
    }
    
    public void setComments(final List<String> comments) {
        this.comments = comments == null ? Collections.emptyList() : Collections.unmodifiableList(comments);
    }
    
    public List<String> getInlineComments() {
        return inlineComments;
    }
    
    public void setInlineComments(final List<String> inlineComments) {
        this.inlineComments = inlineComments == null ? Collections.emptyList() : Collections.unmodifiableList(inlineComments);
    }
}
