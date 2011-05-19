package com.googlecode.ermete.account.provider;

import com.googlecode.ermete.R;
import com.googlecode.ermete.account.Account;
import com.googlecode.ermete.sms.SMS;

// web, widget and business all in one account
public class Vodafone extends Account {
    private static final long serialVersionUID = 1L;
        
    static final String PROVIDER = "Vodafone";

    public Vodafone() {
	label = PROVIDER;
	provider = PROVIDER;
	logoID = R.drawable.ic_logo_vodafone;
	limit = 10;
    }
    
    @Override
    public int calcRemaining(int length) {
	if (length == 0) {
	    return 0;
	} else if (length <= 160) {
	    return 160 - length;
	} else if (length <= 307) {
	    return 307 - length;
	} else if (length <= 360) {
	    return 360 - length;
	} else {
	    return 360 - length;
	}
    }

    @Override
    public int calcFragments(int length) {
	if (length == 0) {
	    return 0;
	} else if (length <= 160) {
	    return 1;
	} else if (length <= 307) {
	    return 2;
	} else if (length <= 360) {
	    return 3;
	} else {
	    return 0;
	}
    }

    @Override
    public Result login() {
	// TODO Auto-generated method stub
	return Result.SUCCESSFUL;
    }

    @Override
    public String[] info() {
	// TODO Auto-generated method stub
	String[] senders = new String[2];
	senders[0] = "348 7654321";
	senders[1] = "340 1234567";
	return senders;
    }

    @Override
    public Result send(SMS sms) {
	// TODO Auto-generated method stub
	return Result.SUCCESSFUL;
    }

}
