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

/**
 * Protobuf representation of a list of friends (represented as strings) to be
 * sent in the clear over the wire to a communication partner. This is used in
 * the simplified implementation without crypto and SHOULD NOT BE USED when
 * private-set intersection is in use.
 */
public final class CleartextFriends extends Message {

  public static final List<String> DEFAULT_FRIENDS = Collections.emptyList();
    private static final String LIST_KEY = "friends";

  /**
   * A list of friends, represented as opaque String-encoded friend ids.
   */
  public final List<String> friends;

  public CleartextFriends(ArrayList<String> friends) {
    this.friends = (List<String>) friends.clone();
  }

    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        try {
            json.put(LIST_KEY, new JSONArray(friends));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static CleartextFriends fromJSON(JSONObject json){

        List<String> friends = new ArrayList<>();
        if(json.has(LIST_KEY)){
            try {
                JSONArray jsonArray = json.getJSONArray(LIST_KEY);
                for(int i=0; i<jsonArray.length(); i++){
                    friends.add((String) jsonArray.get(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return new CleartextFriends((ArrayList<String>) friends);
    }
}
