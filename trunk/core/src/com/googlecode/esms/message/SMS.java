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

import java.util.LinkedList;
import java.util.List;

/**
 * Class representing a text message, along with other information such as
 * receivers name and number, sending date, CAPTCHA image or text, etc 
 * @author Andrea De Pasquale
 */
public class SMS implements Cloneable {
  
  List<Receiver> receivers;
  String message;
  String date;

  byte[] captchaArray;
  String captchaText;

  /**
   * Message with no receivers.
   * @param message SMS text
   */
  public SMS(String message) {
    this(message, new LinkedList<Receiver>());
  }
  
  /**
   * Message with receivers.
   * @param message SMS text
   * @param receivers List of receivers
   */
  public SMS(String message, List<Receiver> receivers) {
    this.message = message;
    this.receivers = receivers;
  }

  public List<Receiver> getReceivers() {
    return receivers;
  }

  public void setReceivers(List<Receiver> receivers) {
    this.receivers = receivers;
  }

  public void addReceiver(Receiver receiver) {
    this.receivers.add(receiver);
  }
  
  public void removeReceiver(Receiver receiver) {
    this.receivers.remove(receiver);
  }
  
  public void clearReceivers() {
    this.receivers.clear();
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public byte[] getCaptchaArray() {
    return captchaArray;
  }

  public void setCaptchaArray(byte[] captchaArray) {
    this.captchaArray = captchaArray;
    this.captchaText = "";
  }

  public String getCaptchaText() {
    return captchaText;
  }

  public void setCaptchaText(String captchaText) {
    this.captchaText = captchaText;
    this.captchaArray = null;
  }

  public SMS clone() {
    try {
      SMS clone = (SMS) super.clone();
      clone.setReceivers(new LinkedList<Receiver>(this.receivers));
      return clone;
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return null;
    }
  }
}
