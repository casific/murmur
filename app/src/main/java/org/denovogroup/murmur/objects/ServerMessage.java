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
 * Data sent by the "server" in a PSI exchange.
 */
public final class ServerMessage extends Message {

  public static final List<ByteString> DEFAULT_DOUBLEBLINDEDFRIENDS = Collections.emptyList();
  public static final List<ByteString> DEFAULT_HASHEDBLINDEDFRIENDS = Collections.emptyList();
    private static final String DBLIND = "dblind";
    private static final String DHASH = "dhash";

  /**
   * Double blinded friends of the client.
   */
  public final List<ByteString> doubleBlindedFriends;

  /**
   * Hashed blinded friends of the server.
   */
  public final List<ByteString> hashedBlindedFriends;

  public ServerMessage(ArrayList<ByteString> doubleBlindedFriends, ArrayList<ByteString> hashedBlindedFriends) {
    this.doubleBlindedFriends = doubleBlindedFriends != null ? (List<ByteString>) doubleBlindedFriends.clone() : DEFAULT_DOUBLEBLINDEDFRIENDS;
    this.hashedBlindedFriends = hashedBlindedFriends != null ? (List<ByteString>) hashedBlindedFriends.clone() : DEFAULT_HASHEDBLINDEDFRIENDS;
  }

    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        JSONArray dBlinded = new JSONArray();
        JSONArray dHashed = new JSONArray();

        for(ByteString dblind : doubleBlindedFriends){
            dBlinded.put(dblind.base64());
        }
        for(ByteString hashed : hashedBlindedFriends){
            dHashed.put(hashed.base64());
        }

        try {
            json.put(DBLIND, dBlinded);
            json.put(DHASH, dHashed);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    public static ServerMessage fromJSON(JSONObject json){
        List<ByteString> doubleBlindedFriends = new ArrayList<>();
        List<ByteString> hashedBlindedFriends = new ArrayList<>();

        try {
            JSONArray dblinded = json.getJSONArray(DBLIND);
            JSONArray dhashed = json.getJSONArray(DHASH);

            for(int i=0; i<dblinded.length();i++){
                doubleBlindedFriends.add(ByteString.decodeBase64((String) dblinded.get(i)));
            }
            for(int i=0; i<dhashed.length();i++){
                hashedBlindedFriends.add(ByteString.decodeBase64((String) dhashed.get(i)));
            }
            return new ServerMessage((ArrayList<ByteString>) doubleBlindedFriends, (ArrayList<ByteString>) hashedBlindedFriends);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}

