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

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import org.denovogroup.murmur.backend.*;
import org.denovogroup.murmur.backend.SecurityManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Representation of a single Murmur message with text and priority.
 */
public final class MurmurMessage extends Message {

  public static final String DEFAULT_TEXT = "";
  public static final Double DEFAULT_TRUST = 0.01D;
    public static final int DEFAULT_PRIORITY = 0;
    public static final String DEFAULT_PSEUDONYM = "";

    public static final String MESSAGE_ID_KEY = "messageId";
    public static final String TEXT_KEY = "text";
    public static final String TRUST_KEY = "trust";
    public static final String PRIORITY_KEY = "priority";
    public static final String PSEUDONYM_KEY = "pseudonym";
    public static final String LATLONG_KEY = "latlang";
    public static final String TIMEBOUND_KEY = "timebound";
    public static final String PARENT_KEY = "parent";
    public static final String BIGPARENT_KEY = "bigparent";
    public static final String HOP_KEY = "hop";
    public static final String MIN_USERS_P_HOP_KEY = "min_users_p_hop";

    private static final float MEAN = 0.0f;
    private static final float VAR = 0.003f;//paper suggest 0.1
    private static final float EPSILON_TRUST = 0.001f;

    private static final boolean USE_SIMPLE_NOISE = false;

    /**
     * The message's id, as a String.
     */
    public final String messageid;

  /**
   * The message's text, as a String.
   */
  public final String text;

  /**
   * The message's trust, as a double.
   */
  public final Double trust;

    /**
     * The message's priority, as a double.
     */
    public final Integer priority;

    /**
     * The message's sender name, as a String.
     */
    public final String pseudonym;

    /**
     * The message's timestamp, as a long.
     */
    public final long timestamp;

    /**
     * The message's location, as a long.
     */
    public final String latlong;

    /**
     * The message's timestamp, as a long.
     */
    public final long timebound;

    /**
     * The message's parent message id, as a long.
     */
    public final String parent;

    /**
     * The message's parent message id, as a long.
     */
    public final String bigparent;

    /**
     * The message's hop count
     */
    public final int hop;

    /**
     * The amount of shared contacts required to perform hop
     */
    public final int contacts_hop;

  public MurmurMessage(String messageid, String text, Double trust, Integer priority, String pseudonym, long timestamp, String latlong, long timebound, String parent, int hop, String bigparent, int contacts_hop) {
      this.messageid = messageid;
    this.text = text;
    this.trust = trust;
    this.priority = priority;
      this.pseudonym = pseudonym;
      this.timestamp = timestamp;
      this.latlong = latlong;
      this.timebound = timebound;
      this.parent = parent;
      this.hop = hop;
      this.bigparent = bigparent;
      this.contacts_hop = contacts_hop;
  }

    public MurmurMessage(String messageid, String text, Double trust, Integer priority, String pseudonym, String latlong, long timebound, String parent, int hop, String bigparent, int contacts_hop) {
        this.messageid = messageid;
        this.text = text;
        this.trust = trust;
        this.priority = priority;
        this.pseudonym = pseudonym;
        this.timestamp = 0;
        this.latlong = latlong;
        this.timebound = timebound;
        this.parent = parent;
        this.hop = hop;
        this.bigparent = bigparent;
        this.contacts_hop = contacts_hop;
    }

    public MurmurMessage(String messageid, String text, Double trust) {
        this.messageid = messageid;
        this.text = text;
        this.trust = trust;
        this.priority = DEFAULT_PRIORITY;
        this.pseudonym = DEFAULT_PSEUDONYM;
        this.timestamp = 0;
        this.latlong = null;
        this.timebound = -1;
        this.parent = null;
        this.hop = 0;
        this.bigparent = null;
        this.contacts_hop = 0;
    }

    public static MurmurMessage fromJSON(Context context, JSONObject json){

        SecurityProfile securityProfile = SecurityManager.getCurrentProfile(context);

        Calendar currentTime = Utils.reduceCalendarMin(Calendar.getInstance());

        return new MurmurMessage(
                json.optString(MESSAGE_ID_KEY, DEFAULT_TEXT),
                json.optString(TEXT_KEY, DEFAULT_TEXT),
                json.optDouble(TRUST_KEY,DEFAULT_TRUST),
                json.optInt(PRIORITY_KEY, DEFAULT_PRIORITY),
                json.optString(PSEUDONYM_KEY, DEFAULT_PSEUDONYM),
                securityProfile.isTimestamp() ?
                        currentTime.getTimeInMillis() : 0L,
                securityProfile.isShareLocation() ?
                        json.optString(LATLONG_KEY, null) : null,
                json.optLong(TIMEBOUND_KEY, -1L),
                json.optString(PARENT_KEY, null),
                json.optInt(HOP_KEY, 0),
                json.optString(BIGPARENT_KEY, null),
                json.optInt(MIN_USERS_P_HOP_KEY,0)
        );
    }

    public static MurmurMessage fromJSON(Context context, String jsonString){
        JSONObject json;
        try {
            json = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
            json = new JSONObject();
        }
        return fromJSON(context, json);
    }

    /** convert the message into a json based on current security profile settings */
    public JSONObject toJSON(Context context, int sharedFriends, int myFriends){
        JSONObject result = new JSONObject();
        try {
            result.put(MESSAGE_ID_KEY, this.messageid);
            result.put(TEXT_KEY, this.text);
            result.put(PRIORITY_KEY, this.priority);
            result.put(HOP_KEY, this.hop + 1);
            result.put(MIN_USERS_P_HOP_KEY, this.contacts_hop);
            if(parent != null) result.put(PARENT_KEY, this.parent);
            if(bigparent != null) result.put(BIGPARENT_KEY, this.bigparent);
            if(timebound > 0) result.put(TIMEBOUND_KEY, this.timebound);

            SecurityProfile profile = SecurityManager.getCurrentProfile(context);

            //put optional items based on security profile settings
            if(profile.isUseTrust()) result.put(TRUST_KEY, (profile.isUseTrust() ?
                    makeNoise(trust, sharedFriends, myFriends)
                    : 0));
            if(profile.isPseudonyms()) result.put(PSEUDONYM_KEY, this.pseudonym);
            if(profile.isShareLocation()) result.put(LATLONG_KEY, this.latlong);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    public Location getLocation(){
        if(latlong == null) return null;

        try {
            Location location = new Location(LocationManager.GPS_PROVIDER);

            double lat = Double.parseDouble(latlong.substring(0,latlong.indexOf(" ")));
            double lng = Double.parseDouble(latlong.substring(latlong.indexOf(" ") + 1));

            location.setLatitude(lat);
            location.setLongitude(lng);

            return location;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /** Compute the priority score for a person normalized by his number of friends, and passed
     * through a sigmoid function.
     *
     * @param priority The priority of the message before computing trust.
     * @param sharedFriends Number of friends shared between this person and the message sender.
     * @param myFriends The number of friends this person has.
     */
    private float makeNoise(double priority,
                            int sharedFriends,
                            int myFriends){
        if(USE_SIMPLE_NOISE){
            return (float)(priority + Utils.makeNoise(MEAN, VAR));
        } else {
            return (float) computeNewPriority_sigmoidFractionOfFriends(priority,
                    sharedFriends,
                    myFriends);
        }
    }

    /** Compute the priority score for a person normalized by his number of friends, and passed
     * through a sigmoid function.
     *
     * @param priority The priority of the message before computing trust.
     * @param sharedFriends Number of friends shared between this person and the message sender.
     * @param myFriends The number of friends this person has.
     */
    public static double computeNewPriority_sigmoidFractionOfFriends(double priority,
                                                                     int sharedFriends,
                                                                     int myFriends) {
        double trustMultiplier = sigmoid(sharedFriends / (double) myFriends, 0.3, 13.0);
        // add noise
        trustMultiplier = trustMultiplier + getGaussian(MEAN,VAR);

        // truncate range
        trustMultiplier = Math.min(trustMultiplier,1);
        trustMultiplier = Math.max(trustMultiplier,0);

        if (sharedFriends == 0) {
            trustMultiplier = EPSILON_TRUST;
        }
        return priority * trustMultiplier;
    }

    /** Pass an input trust score through a sigmoid between 0 and 1, and return the result.
     *
     * @param input The input trust score.
     * @param cutoff The transition point of the sigmoid.
     * @param rate The rate at which the sigmoid grows.
     */
    public static double sigmoid(double input, double cutoff, double rate) {
        return 1.0/(1+Math.pow(Math.E,-rate*(input-cutoff)));
    }

    /** Returns a number drawn from a Gaussian distribution determined by the input parameters.
     *
     * @param mean The mean of the Gaussian distribution.
     * @param variance The variance of the Gaussian distribution.
     */
    private static double getGaussian(double mean, double variance){
        return Utils.makeNoise(mean, variance);
    }
}
