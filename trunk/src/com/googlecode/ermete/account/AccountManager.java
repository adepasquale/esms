package com.googlecode.ermete.account;

import java.io.Serializable;
import java.util.List;

public abstract class AccountManager  implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // XXX create a provider interface?
    protected List<Account> providers;
    protected List<Account> accounts;
    
    public List<Account> getProviders() {
	return providers;
    }
    
    public List<Account> getAccounts() {
	return accounts;
    }
    
    public abstract void insert(Account account);
    
    public abstract void modify(int index);
    
    public void modify(Account account) {
	modify(accounts.indexOf(account));
    }
    
    public abstract void delete(int index);
    
    public void delete(Account account) {
	delete(accounts.indexOf(account));
    }
}
