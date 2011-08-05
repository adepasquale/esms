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
 * Class representing a text message, along with other information such as
 * receivers name and number, sending date, CAPTCHA image or text, etc 
 * @author Andrea De Pasquale
 */
public class SMS {
  String[] receiverName;
  String[] receiverNumber;
  String message;
  String date;

  byte[] captchaArray;
  String captcha;

  /**
   * Default constructor.
   * @param message SMS text
   */
  public SMS(String message) {
    this.message = message;
  }

  public String[] getReceiverName() {
    return receiverName;
  }

  public void setReceiverName(String[] receiverName) {
    this.receiverName = receiverName;
  }

  public String[] getReceiverNumber() {
    return receiverNumber;
  }

  public void setReceiverNumber(String[] receiverNumber) {
    this.receiverNumber = receiverNumber;
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
    this.captcha = "";
  }

  public String getCaptcha() {
    return captcha;
  }

  public void setCaptcha(String captcha) {
    this.captcha = captcha;
    this.captchaArray = null;
  }

}
