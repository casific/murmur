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
package org.denovogroup.rangzen;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A mock of MessageStore allowing us to easily pass two separate message stores
 * to separate exchanges for testing.
 *
 * It doesn't test for duplicate messages.
 */
public class MockMessageStore extends MessageStore {
  /** Mock storage of messages, without regard to priority. */
  private ArrayList<Message> mockMessages = new ArrayList<Message>();

  /** Check how many times getKthMessage was called. */
  public int kthCalls = 0;

  /**
   * Pass-through constructor.
   */
  public MockMessageStore(Context context, int encryptionMode) { 
    super(context, encryptionMode);
  }

  @Override
  public boolean addMessage(String message, double priority) {
    mockMessages.add(new Message(priority, message));
    return true;
  }

  @Override
  public int getMessageCount() {
    return mockMessages.size();  
  }

  @Override
  public Message getKthMessage(int k) {
    kthCalls++;
    try {
      return mockMessages.get(k);
    } catch (IndexOutOfBoundsException e) {
      return null;
    }
  }
}
