package com.googlecode.awsms.provider;

import java.util.List;

import com.googlecode.esms.account.Account;
import com.googlecode.esms.message.SMS;

/**
 * Default account to use SIM messaging service. 
 * @author Andrea De Pasquale
 */
public class Telephony extends Account {
  private static final long serialVersionUID = 1L;
  
  static final String PROVIDER = "Telefono";
  
  public Telephony() {
    super(null);
    
    label = PROVIDER;
    provider = PROVIDER;
    limit = 0;
  }
  
  @Override
  public int calcRemaining(int length) {
    // TODO use same method of default app
    return 0;
  }

  @Override
  public int calcFragments(int length) {
    // TODO use same method of default app
    return 0;
  }

  @Override
  public List<String> getSenderList() {
    // TODO retrieve SIM phone number
    return null;
  }

  @Override
  public Result login() {
    return Result.SUCCESSFUL;
  }

  @Override
  public Result logout() {
    return Result.SUCCESSFUL;
  }

  @Override
  public Result send(SMS sms) {
    // TODO use telephony provider
    return Result.UNSUPPORTED_ERROR;
  }

}
