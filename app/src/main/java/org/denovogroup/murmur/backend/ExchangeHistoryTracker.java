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
package org.denovogroup.murmur.backend;

import android.content.Context;
import android.content.SharedPreferences;

import org.apache.log4j.Logger;
import org.denovogroup.murmur.ui.MurmurApplication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Liran on 11/15/2015.
 *
 * This class is a simple controller class used for optimizing exchanges between two known
 * peers.
 */
public class ExchangeHistoryTracker {

    private static final String TAG = "ExchangeHistoryTracker";

    private static final Logger log = Logger.getLogger(TAG);

    private static ExchangeHistoryTracker instance;

    private List<ExchangeHistoryItem> history = new ArrayList<>();

    private int exchangeCount = 0;

    /** Get an instance of the tracker, create one if necessary */
    public static ExchangeHistoryTracker getInstance() {
        if (instance == null){
            instance = new ExchangeHistoryTracker();
        }
        return instance;
    }

    private ExchangeHistoryTracker() {
        //an empty private constructor to enforce singleton pattern.
        exchangeCount = MurmurApplication.getContext().getSharedPreferences("count",Context.MODE_PRIVATE).getInt("count",0);

    }

    /** Remove items from history based on passed collection. Any item not found in
     * in the collection will be removed
     * @param availablePeers peers to keep in history. discard the rest
     */
    public void cleanHistory(Collection<Peer> availablePeers) {
        log.debug( "cleaning history");
        //get a list of peer addresses to cross reference with available history
        List<String> newPeerAddresses = new ArrayList<>();
        if (availablePeers != null){
            for (Peer peer : availablePeers) {
                if (peer != null && peer.address != null) {
                    newPeerAddresses.add(peer.address);
                }
            }
        }

        //remove irrelevant peers from history
        List<ExchangeHistoryItem> updatedHistory = new ArrayList<>();
        for(ExchangeHistoryItem peer : history){
            if(newPeerAddresses.contains(peer.address)){
                updatedHistory.add(peer);
            }
        }

        history = updatedHistory;
    }

    /** update the history track with supplied details, if address is already in the list
     * it will be updated, if not than it will be added
      * @param address WifiP2p device address with which interacted
     */
    public void updateHistory(Context context ,String address){
        log.debug( "history updated for:"+address);
        for(ExchangeHistoryItem item : history){
            if(item.address.equals(address)){
                item.attempts = 0;
                item.storeVersion = MessageStore.getInstance(context ).getStoreVersion();
                item.lastExchangeTime = System.currentTimeMillis();
                item.lastPicked = System.currentTimeMillis();
                return;
            }
        }

        history.add(new ExchangeHistoryItem(address, MessageStore.getInstance(context ).getStoreVersion(), System.currentTimeMillis()));
    }

    public void updatePickHistory(String address){
        for(ExchangeHistoryItem item : history){
            if(item.address.equals(address)){
                item.lastPicked = System.currentTimeMillis();
            }
        }
    }

    /** update the history item attempts counter
     * @param address WifiP2p device address with which interacted
     */
    public void updateAttemptsHistory(String address){
        for(ExchangeHistoryItem item : history){
            if(item.address.equals(address)){
                item.attempts++;
            }
        }
    }

    public ExchangeHistoryItem getHistoryItem(String address){
        for(ExchangeHistoryItem item : history){
            if(item.address.equals(address)){
                return item;
            }
        }
        return null;
    }

    public int getExchangeHistory(){
        return exchangeCount;
    }

    public void incrementExchangeCount(){
        exchangeCount++;
        SharedPreferences preferences = MurmurApplication.getContext().getSharedPreferences("count",Context.MODE_PRIVATE);
        preferences.edit().putInt("count",exchangeCount).commit();
    }

    public void resetExchangeCount(){
        exchangeCount = 0;
        SharedPreferences preferences = MurmurApplication.getContext().getSharedPreferences("count",Context.MODE_PRIVATE);
        preferences.edit().putInt("count",exchangeCount).commit();
    }

    public class ExchangeHistoryItem{
        /** The device bluetooth address of the partner with which an exchange was made*/
        String address;
        /** The local message store version after the exchange*/
        String storeVersion;
        /** Time in millis when exchange performed*/
        long lastExchangeTime;
        /** Number of attempts taken during which local store wasn't changed*/
        int attempts;
        /** the last time this peer was picked for exchange */
        long lastPicked;

        public String getAddress() {
            return address;
        }

        public String getStoreVersion() {
            return storeVersion;
        }

        public long getLastExchangeTime() {
            return lastExchangeTime;
        }

        public int getAttempts() {
            return attempts;
        }

        public long getLastPicked() {
            return lastPicked;
        }

        public void updateLastPicked(){
            lastPicked = System.currentTimeMillis();
        }

        public ExchangeHistoryItem(String address, String storeVersion, long lastExchangeTime) {
            this.address = address;
            this.storeVersion = storeVersion;
            this.lastExchangeTime = lastExchangeTime;
            this.attempts = 0;
            this.lastPicked=0;
        }
    }
}
