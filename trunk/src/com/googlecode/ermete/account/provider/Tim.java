package com.googlecode.ermete.account.provider;

import com.googlecode.ermete.R;
import com.googlecode.ermete.account.Account;
import com.googlecode.ermete.sms.SMS;

public class Tim extends Account {
    private static final long serialVersionUID = 1L;
    
    static final String PROVIDER = "TIM";

    public Tim() {
	label = PROVIDER;
	provider = PROVIDER;
	logoID = R.drawable.ic_logo_vodafone;
	limit = 5;
    }
    
    @Override
    public int calcRemaining(int length) {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    public int calcFragments(int length) {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    public Result login() {
	// TODO Auto-generated method stub
	return Result.LOGIN_ERROR;
    }

    @Override
    public String[] info() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Result send(SMS sms) {
	// TODO Auto-generated method stub
	return Result.NETWORK_ERROR;
    }

}
