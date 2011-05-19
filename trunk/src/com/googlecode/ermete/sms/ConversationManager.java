package com.googlecode.ermete.sms;

public abstract class ConversationManager {
    public abstract SMS loadDraft(String receiver);
    public abstract void saveDraft(String receiver);
    public abstract SMS loadInbox(String sender);
    public abstract void saveOutbox(String receiver);
    public abstract SMS loadFailed(String receiver);
    public abstract void saveFailed(String receiver);
}
