package com.googlecode.ermete.sms;

public class SMS {
    String[] receiverName;
    String[] receiverNumber;
    String message;
    String date;

    byte[] captchaArray;
    String captcha;
    
    public SMS(String message) {
	this.message = message;
    }
    
    public String[] getReceiverName() {
	return receiverName;
    }
    
    public void setReceiverName(String[] receiverName) {
	this.receiverName = receiverName;
    }
    
    public String[] getReceiverNumber() {
        return receiverNumber;
    }
    
    public void setReceiverNumber(String[] receiverNumber) {
        this.receiverNumber = receiverNumber;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
}
