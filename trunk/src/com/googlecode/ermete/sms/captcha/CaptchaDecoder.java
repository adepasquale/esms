package com.googlecode.ermete.sms.captcha;

public abstract class CaptchaDecoder {
    public abstract String decode(byte[] image);
}
