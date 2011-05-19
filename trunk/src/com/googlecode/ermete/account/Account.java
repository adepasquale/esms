package com.googlecode.ermete.account;

import java.io.Serializable;

import com.googlecode.ermete.sms.SMS;

public abstract class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Result {
	SUCCESSFUL,
	NETWORK_ERROR, 
	LOGIN_ERROR, 
	RECEIVER_ERROR, 
	MESSAGE_ERROR, 
	LIMIT_ERROR, 
	UNKNOWN_ERROR 
    }
    
    protected String label;
    protected String provider;

    // FIXME this is platform-dependent
    protected int logoID;
    
    protected String username;
    protected String password;
    
    protected String sender;
    
    protected int count;
    protected int limit;
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
        if (label.length() > 12)
            this.label = label.substring(0, 12);
    }
    
    public String getProvider() {
        return provider;
    }
    
    public int getLogoID() {
        return logoID;
    }

    public void setLogoID(int logoID) {
        this.logoID = logoID;
    }

    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public int getCount() {
        return count;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public abstract int calcRemaining(int length);
    public abstract int calcFragments(int length);
    
    public abstract Result login();
    public abstract String[] info();
    public abstract Result send(SMS sms);
}
