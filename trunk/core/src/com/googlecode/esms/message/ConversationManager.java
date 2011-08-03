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

public abstract class ConversationManager {
  public abstract SMS loadDraft(String receiver);
  public abstract void saveDraft(SMS sms);
  public abstract SMS loadInbox(String sender);
  public abstract void saveInbox(SMS sms);
  public abstract SMS loadOutbox(String receiver);
  public abstract void saveOutbox(SMS sms);
  public abstract SMS loadSent(String receiver);
  public abstract void saveSent(SMS sms);
  public abstract SMS loadFailed(String receiver);
  public abstract void saveFailed(SMS sms);
}
