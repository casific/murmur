/*
* Copyright (c) 2016, De Novo Group
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
* AND ANY EXPRES S OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
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
package org.denovogroup.murmur.objects;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import okio.ByteString;

/**
 * Data sent by the "client" in a PSI exchange.
 */
public final class ClientMessage extends Message {

  public static final List<JSONObject> DEFAULT_MESSAGES = Collections.emptyList();
  public static final List<ByteString> DEFAULT_BLINDEDFRIENDS = Collections.emptyList();
    private static final String MESSAGES = "messages";
    private static final String FRIENDS = "friends";

  /**
   * The client's messages to propagate.
   */
  public List<JSONObject> messages;

  /**
   * The client's friends, blinded.
   */
  public List<ByteString> blindedFriends;

  public ClientMessage(ArrayList<JSONObject> messages, ArrayList<ByteString> blindedFriends) {
    this.messages = (messages != null) ?(List<JSONObject>)messages.clone() : DEFAULT_MESSAGES;
    this.blindedFriends = (blindedFriends != null) ? (List<ByteString>) blindedFriends.clone() :DEFAULT_BLINDEDFRIENDS;
  }

    public JSONObject toJSON(){
        JSONObject json = new JSONObject();
        JSONArray messagesArray = new JSONArray();
        JSONArray friendsArray = new JSONArray();

        for(JSONObject message : messages){
            messagesArray.put(message);
        }
        for(ByteString friend : blindedFriends){
            friendsArray.put(friend.base64());
        }
        try {
            json.put(MESSAGES,messagesArray);
            json.put(FRIENDS,friendsArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static ClientMessage fromJSON(JSONObject json){
        try {
            JSONArray messagesArray = json.getJSONArray(MESSAGES);
            JSONArray friendsArray = json.getJSONArray(FRIENDS);

            List<JSONObject> messages = new ArrayList<>();
            List<ByteString> friends = new ArrayList<>();

            for(int i=0; i<messagesArray.length(); i++){
                messages.add((JSONObject) messagesArray.get(i));
            }
            for(int i=0; i<friendsArray.length(); i++){
                friends.add(ByteString.decodeBase64((String) friendsArray.get(i)));
            }
            return new ClientMessage((ArrayList<JSONObject>)messages,(ArrayList<ByteString>)friends);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
