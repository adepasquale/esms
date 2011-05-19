package com.googlecode.ermete.sms;

public class ConversationManagerAndroid extends ConversationManager {

    @Override
    public SMS loadDraft(String receiver) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public void saveDraft(String receiver) {
	// TODO Auto-generated method stub
	
    }

    @Override
    public SMS loadInbox(String sender) {
	SMS inbox = new SMS(
		"Messaggio di prova a cui devo rispondere. " +
		"Messaggio di prova a cui devo rispondere. " +
		"Messaggio di prova a cui devo rispondere. "
	);
	inbox.setDate("12:00");
	
	return inbox;
    }

    @Override
    public void saveOutbox(String receiver) {
	// TODO Auto-generated method stub
	
    }

    @Override
    public SMS loadFailed(String receiver) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public void saveFailed(String receiver) {
	// TODO Auto-generated method stub
	
    }

}
