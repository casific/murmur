package org.denovogroup.murmur.backend;

/**
 * Created by Liran on 11/17/2015.
 *
 * A simple object representing a security profile and its properties
 */
public class SecurityProfile {
    private float autodeleteTrust;
    private int autodeleteAge;
    private int cooldown;
    /** profile security strength*/
    int strength;
    /** profile display name*/
    int name;
    /** allow display/storage of timestamps*/
    boolean timestamp;
    /** allow display/storage of pseudonym from sender*/
    boolean pseudonyms;
    /** Number of messages in feed (not archived) (handled as FIFO, i.e. at any given time not more than X messages are kept, newest replaces oldest)*/
    int feedSize;
    /** can add friends from phonebook */
    boolean friendsViaBook;
    /** can add friends using QR code */
    boolean friendsViaQR;
    /**Auto-delete + decay. user can change priority threshold*/
    boolean autodelete;
    /** allow sharing location */
    boolean shareLocation;
    /** Minimum shared contacts for message exchange */
    int minSharedContacts;
    /** Maximum messages for message exchange */
    int maxMessages;
    /** amount of days a timebound message will stay alive by default */
    int timeboundPeriod;
    /** enforce lock pattern ?*/
    boolean enforceLock;
    /** if app should calculate trust based on shared friends between devices */
    boolean useTrust;
    /** if exchange should be randomized between all devices in the vicinity or as round robin */
    boolean randomExchange;
    /** minimum contacts required for restericted messages to be exchanged */
    int minContactsForHop;

    public SecurityProfile(int strength){
        this.strength = strength;
    }

    public SecurityProfile(int strength,
                           int name,
                           boolean timestamp,
                           boolean pseudonyms,
                           int feedSize,
                           boolean friendsViaBook,
                           boolean friendsViaQR,
                           boolean autodelete,
                           float autodeleteTrust,
                           int autodeleteAge,
                           boolean shareLocation,
                           int minSharedContacts,
                           int maxMessages,
                           int cooldown,
                           int timeboundPeriod,
                           boolean enforceLock,
                           boolean useTrust,
                           boolean randomExchange,
                           int minContactsForHop
                            ) {
        this.strength = strength;
        this.name = name;
        this.timestamp = timestamp;
        this.pseudonyms = pseudonyms;
        this.feedSize = feedSize;
        this.friendsViaBook = friendsViaBook;
        this.friendsViaQR = friendsViaQR;
        this.autodelete = autodelete;
        this.autodeleteTrust = autodeleteTrust;
        this.autodeleteAge = autodeleteAge;
        this.shareLocation = shareLocation;
        this.minSharedContacts = minSharedContacts;
        this.maxMessages = maxMessages;
        this.cooldown = cooldown;
        this.timeboundPeriod = timeboundPeriod;
        this.enforceLock = enforceLock;
        this.useTrust = useTrust;
        this.randomExchange = randomExchange;
        this.minContactsForHop = minContactsForHop;
    }

    public int getStrength(){
        return strength;
    }

    public int getName() {
        return name;
    }

    public SecurityProfile setName(int name) {
        this.name = name;
        return this;
    }

    public boolean isPseudonyms() {
        return pseudonyms;
    }

    public SecurityProfile setPseudonyms(boolean pseudonyms) {
        this.pseudonyms = pseudonyms;
        return this;
    }

    public boolean isTimestamp() {
        return timestamp;
    }

    public SecurityProfile setTimestamp(boolean timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public int getFeedSize() {
        return feedSize;
    }

    public SecurityProfile setFeedSize(int feedSize) {
        this.feedSize = feedSize;
        return this;
    }

    public boolean isFriendsViaBook() {
        return friendsViaBook;
    }

    public SecurityProfile setFriendsViaBook(boolean friendsViaBook) {
        this.friendsViaBook = friendsViaBook;
        return this;
    }

    public boolean isFriendsViaQR() {
        return friendsViaQR;
    }

    public SecurityProfile setFriendsViaQR(boolean friendsViaQR) {
        this.friendsViaQR = friendsViaQR;
        return this;
    }

    public boolean isAutodelete() {
        return autodelete;
    }

    public SecurityProfile setAutodelete(boolean autodelete) {
        this.autodelete = autodelete;
        return this;
    }

    public boolean isShareLocation() {
        return shareLocation;
    }

    public SecurityProfile setShareLocation(boolean shareLocation) {
        this.shareLocation = shareLocation;
        return this;
    }

    public int getMinSharedContacts() {
        return minSharedContacts;
    }

    public SecurityProfile setMinSharedContacts(int minSharedContacts) {
        this.minSharedContacts = minSharedContacts;
        return this;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public SecurityProfile setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
        return this;
    }

    public SecurityProfile clone(){
        return new SecurityProfile(
                strength,
                name,
                timestamp,
                pseudonyms,
                feedSize,
                friendsViaBook,
                friendsViaQR,
                autodelete,
                autodeleteTrust,
                autodeleteAge,
                shareLocation,
                minSharedContacts,
                maxMessages,
                cooldown,
                timeboundPeriod,
                enforceLock,
                useTrust,
                randomExchange,
                minContactsForHop);
    }

    public float getAutodeleteTrust() {
        return autodeleteTrust;
    }

    public int getAutodeleteAge() {
        return autodeleteAge;
    }

    public int getCooldown() {
        return cooldown;
    }

    public SecurityProfile setAutodeleteTrust(float autodeleteTrust) {
        this.autodeleteTrust = autodeleteTrust;
        return this;
    }

    public SecurityProfile setAutodeleteAge(int autodeleteAge) {
        this.autodeleteAge = autodeleteAge;
        return this;
    }

    public SecurityProfile setCooldown(int cooldown) {
        this.cooldown = cooldown;
        return this;
    }

    public int getTimeboundPeriod() {
        return timeboundPeriod;
    }

    public SecurityProfile setTimeboundPeriod(int timeboundPeriod) {
        this.timeboundPeriod = timeboundPeriod;
        return this;
    }

    public boolean isEnforceLock() {
        return enforceLock;
    }

    public SecurityProfile setEnforceLock(boolean enforceLock) {
        this.enforceLock = enforceLock;
        return this;
    }

    public boolean isUseTrust() {
        return useTrust;
    }

    public SecurityProfile setUseTrust(boolean useTrust) {
        this.useTrust = useTrust;
        return this;
    }

    public boolean isRandomExchange() {
        return randomExchange;
    }

    public SecurityProfile setRandomExchange(boolean randomExchange) {
        this.randomExchange = randomExchange;
        return this;
    }

    public SecurityProfile setMinContactsForHop(int minContactsForHop) {
        this.minContactsForHop = minContactsForHop;
        return this;
    }

    public int getMinContactsForHop() {
        return minContactsForHop;
    }
}
