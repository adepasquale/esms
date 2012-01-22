/*
 *  This file is part of Ermete SMS.
 *  
 *  Ermete SMS is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  Ermete SMS is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Ermete SMS.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */

package com.googlecode.esms.message;

/**
 * Load and save conversations, i.e. a set of text messages. 
 * @author Andrea De Pasquale
 */
public abstract class ConversationManager {
  // TODO change SMS to List<SMS>
  // and add default implementation for single message
  // like: loadDraft(String receiver) { loadDraft(receiver, 1); }
  
  /**
   * Load last draft of message for some receiver.
   * @param receiver to identify the conversation
   * @return SMS the SMS draft.
   */
  public abstract SMS loadDraft(String receiver);
  
  /**
   * Save draft of a message.
   * @param sms to be saved
   */
  public abstract void saveDraft(SMS sms);
  
  /**
   * Load last message received from some sender
   * @param sender to identify the conversation
   * @return SMS the SMS received.
   */
  public abstract SMS loadInbox(String sender);
  
  /**
   * Save received message.
   * @param sms to be saved
   */
  public abstract void saveInbox(SMS sms);
  
  /**
   * Load last message still to be sent to some receiver
   * @param receiver to identify the conversation
   * @return SMS the SMS still unsent.
   */
  public abstract SMS loadOutbox(String receiver);
  
  /**
   * Save message before sending.
   * @param sms to be saved
   */
  public abstract void saveOutbox(SMS sms);
  
  /**
   * Load last message sent to some receiver
   * @param receiver to identify the conversation
   * @return the SMS sent.
   */
  public abstract SMS loadSent(String receiver);
  
  /**
   * Save message after sending.
   * @param sms to be saved
   */
  public abstract void saveSent(SMS sms);
  
  /**
   * Load last message not sent due to errors
   * @param receiver to identify the conversation
   * @return the SMS not sent for errors.
   */
  public abstract SMS loadFailed(String receiver);
  
  /**
   * Save message after an error
   * @param sms to be saved
   */
  public abstract void saveFailed(SMS sms);
}
