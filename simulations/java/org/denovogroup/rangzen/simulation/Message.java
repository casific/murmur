/*
 * Copyright (c) 2014, De Novo Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.denovogroup.rangzen.simulation;

import java.io.Serializable;

/**
 * This class represents a single message in the Rangzen system, including its
 * priority and content.
 */
public class Message implements Serializable, Comparable<Message> {
  private static final long serialVersionUID = 1;

  private static final int I_AM_GREATER_THAN = 1;
  private static final int I_AM_EQUAL_TO = 0;
  private static final int I_AM_LESS_THAN = -1;

  public double priority;
  public String content;

  /**
   * Create a new message with the given content and priority.
   *
   * @param content The content of the message.
   * @param priority The priority of the message (typically between 0.0 and 1.0).
   */
  public Message(String content, double priority) {
    this.content = content;
    this.priority = priority;
  }

  /**
   * Create a new instance of message with the same content and priority as
   * this message.
   */
  public Message clone() {
    return new Message(content, priority);
  }

  /**
   * Messages are comparable based on their priority. 
   *
   * @param other The message to be compared to.
   * @return The standard meaning for compareTo return values is used here.
   */
  public int compareTo(Message other) {
    // All non-null messages have greater priority than all null messages.
    if (other == null) {
      throw new NullPointerException();
    }
    else if (this.priority == other.priority) {
      return I_AM_EQUAL_TO;
    }
    else if (this.priority < other.priority) {
      return I_AM_LESS_THAN;
    }
    else { // (this.priority > other.priority)
      return I_AM_GREATER_THAN;
    }
  }

  /**
   * A string showing the priority and content of the message.
   */
  public String toString() {
    return "("+priority+"): "+content;
  }

  /**
   * Two messages are equal if their content is .equal()
   *
   * @param other Another message to be compared to.
   * @return True if the messages have the same content, false otherwise.
   */
  public boolean equals(Message other) {
    if (other == null) {
      return false;
    } else if (other.content.equals(this.content)) {
      return true;
    } else {
     return false;
    } 
  }
}
