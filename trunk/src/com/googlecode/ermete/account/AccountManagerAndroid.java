package com.googlecode.ermete.account;

import java.util.LinkedList;

import com.googlecode.ermete.R;
import com.googlecode.ermete.account.provider.Tim;
import com.googlecode.ermete.account.provider.Vodafone;

public class AccountManagerAndroid extends AccountManager {
    private static final long serialVersionUID = 1L;

    public AccountManagerAndroid() {
	providers = new LinkedList<Account>();
	providers.add(new Vodafone());
	providers.add(new Tim());
	
	accounts = new LinkedList<Account>();
	// TODO load accounts from the DB
	Account test1 = new Vodafone();
	test1.setLabel("Lavoro V.");
	test1.setSender("340 1234567");
	test1.setUsername("vodafone");
	test1.setPassword("password");
	test1.setLogoID(R.drawable.ic_logo_vodafone);
	accounts.add(test1);
	Account test2 = new Tim();
	test2.setLabel("Personale T.");
	test2.setSender("333 1234567");
	test2.setUsername("telecom");
	test2.setPassword("password");
	test2.setLogoID(R.drawable.ic_logo_vodafone);
	accounts.add(test2);
    }

    @Override
    public void insert(Account account) {
	// TODO Auto-generated method stub
	accounts.add(account);
    }

    @Override
    public void modify(int index) {
	// TODO Auto-generated method stub
	
    }

    @Override
    public void delete(int index) {
	// TODO Auto-generated method stub
	accounts.remove(index);
    }

}
