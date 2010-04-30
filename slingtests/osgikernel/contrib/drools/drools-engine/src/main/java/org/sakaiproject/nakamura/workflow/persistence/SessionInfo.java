package org.sakaiproject.nakamura.workflow.persistence;

import java.util.Arrays;
import java.util.Date;


import org.drools.marshalling.impl.MarshallingConfiguration;

public class SessionInfo {
    int                        id;

    private Date               startDate;
    private Date               lastModificationDate;    
    private boolean            dirty;         
    
    private byte[]             rulesByteArray;


    private transient JcrSessionMarshallingHelper helper;
    
    public SessionInfo() {
        this.startDate = new Date();
    }

    public int getId() {
        return this.id;
    }
    
    public void setJPASessionMashallingHelper(JcrSessionMarshallingHelper helper) {
        this.helper = helper;
    }

    public byte[] getData() {
        return this.rulesByteArray;
    }
    
    public Date getStartDate() {
        return this.startDate;
    }

    public Date getLastModificationDate() {
        return this.lastModificationDate;
    }

    public void setDirty() {
        this.dirty = true;
    }
    
    
    public void update() {
        // we always increase the last modification date for each action, so we know there will be an update
        byte[] newByteArray = this.helper.getSnapshot();
        if ( !Arrays.equals( newByteArray,
                             this.rulesByteArray ) ) {
            this.lastModificationDate = new Date();
            this.rulesByteArray = newByteArray;
        }
        this.lastModificationDateShadow = this.lastModificationDate;       
        this.rulesByteArrayShadow = this.rulesByteArray;
        this.dirty = false;
    }
    
 
}
