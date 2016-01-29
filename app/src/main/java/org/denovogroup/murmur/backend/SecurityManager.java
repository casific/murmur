package org.denovogroup.murmur.backend;

import android.content.Context;
import android.content.SharedPreferences;

import org.denovogroup.murmur.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liran on 11/17/2015.
 *
 * The controller class in charge of managing various security profiles available through the app
 * and provid the methods for setting and reading the currently set security related settings
 */
public class SecurityManager {

    private static SecurityManager instance;

    /** list of privacy profiles available */
    private static List<SecurityProfile> profiles;

    /** the shared preference file where security settings are stored */
    public static final String SETTINGS_FILE = "Settings";
    /** the key under which pseudonym is set in the file*/
    public static final String PSEUDONYM_KEY = "pseudonym";
    /** the key under which mac is set in the file*/
    public static final String MAC_KEY = "mac";

    // profile settings
    public static final int CUSTOM_PROFILE_NAME = R.id.radio_profile_custom;
    private static final String PROFILE_NAME_KEY = "name";
    private static final String PROFILE_TIMESTAMP_KEY = "useTimestamp";
    private static final String PROFILE_PSEUDONYM_KEY = "usePseudonym";
    private static final String PROFILE_FEED_SIZE_KEY = "maxFeedSize";
    private static final String PROFILE_FRIEND_VIA_BOOK_KEY = "addFromBook";
    private static final String PROFILE_FRIEND_VIA_QR_KEY = "addFromQR";
    private static final String PROFILE_AUTO_DELETE_KEY = "useAutoDecay";
    private static final String PROFILE_AUTO_DELETE_TRUST_KEY = "AutoDecayTrust";
    private static final String PROFILE_AUTO_DELETE_AGE_KEY = "AutoDecayAge";
    private static final String PROFILE_SHARE_LOCATIONS_KEY = "shareLocations";
    private static final String PROFILE_MIN_SHARED_CONTACTS_KEY = "minSharedContacts";
    private static final String PROFILE_MAX_MESSAGES_KEY = "maxMessagesPerExchange";
    private static final String PROFILE_COOLDOWN_KEY = "exchangeCooldown";
    private static final String PROFILE_TIMEBOUND_KEY = "timebound";
    private static final String PROFILE_ENFORCE_LOCK_KEY = "enforceLock";
    private static final String PROFILE_USE_TRUST_KEY = "useTrust";
    private static final String PROFILE_RANDOM_EXCHANGE_KEY = "randomExchange";
    private static final String PROFILE_MIN_CONTACTS_FOR_HOP_KEY = "minContactsForHop";


    /** Default security profile value if none is stored */
    public static final int DEFAULT_SECURITY_PROFILE = 0;
    /** Default trust threshold value if none is stored */
    public static final float DEFAULT_TRUST_THRESHOLD = 0.0f;
    /** Default pseudonym value if none is stored */
    public static final String DEFAULT_PSEUDONYM = "";

    /** initiate the managers parameters such as the profiles list */
    private void init(){
        profiles = new ArrayList<>();
        profiles.add(new SecurityProfile(1)
                .setName(R.id.radio_profile_flexible)
                .setTimestamp(true)
                .setPseudonyms(true)
                .setFeedSize(0)
                .setFriendsViaBook(true)
                .setFriendsViaQR(true)
                .setAutodelete(true)
                .setAutodeleteTrust(0f)
                .setAutodeleteAge(0)
                .setShareLocation(true)
                .setMinSharedContacts(0)
                .setMaxMessages(1000)
                .setCooldown(5)
                .setEnforceLock(false)
                .setUseTrust(false)
                .setRandomExchange(true) // roundrobin was causing network issues so it is disabled for all profiles
                .setTimeboundPeriod(3)
                .setMinContactsForHop(3)
        );
        profiles.add(new SecurityProfile(2)
                .setName(R.id.radio_profile_strict)
                .setTimestamp(false)
                .setPseudonyms(false)
                .setFeedSize(0)
                .setFriendsViaBook(false)
                .setFriendsViaQR(true)
                .setAutodelete(true)
                .setAutodeleteTrust(0.05f)
                .setAutodeleteAge(14)
                .setShareLocation(false)
                .setMinSharedContacts(5)
                .setMaxMessages(250)
                .setCooldown(30)
                .setEnforceLock(true)
                .setUseTrust(true)
                .setRandomExchange(true) // roundrobin was causing network issues so it is disabled for all profiles
                .setTimeboundPeriod(3)
                .setMinContactsForHop(5)
        );
    }

    /** return the existing instance of the manager if exists. create new if not*/
    public static SecurityManager getInstance(){
        if(instance == null){
            instance = new SecurityManager();
            instance.init();
        }
        return instance;
    }

    private SecurityManager() {
        //an empty constructor
    }

    /** return the SecurityProfile object with the supplied name in the profiles list of this adapter or null if using custom profile*/
    public SecurityProfile getProfile(int name){

        for(SecurityProfile profile : profiles) {
            if(profile.name == name) return profile.clone();
        }
        return null;
    }

    /** read the currently set privacy profile from local storage */
    public static SecurityProfile getCurrentProfile(Context context){
        if(instance == null) getInstance();

        SharedPreferences pref = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);

        int profile_name = pref.getInt(PROFILE_NAME_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getName());

        if(profile_name != CUSTOM_PROFILE_NAME){
            for(SecurityProfile profile : profiles) {
                if(profile.name == profile_name){
                    return profile.clone();
                }
            }
        }

        SecurityProfile customProfile = new SecurityProfile(
                0,
                CUSTOM_PROFILE_NAME,
                pref.getBoolean(PROFILE_TIMESTAMP_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).isTimestamp()),
                pref.getBoolean(PROFILE_PSEUDONYM_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).isPseudonyms()),
                pref.getInt(PROFILE_FEED_SIZE_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getFeedSize()),
                pref.getBoolean(PROFILE_FRIEND_VIA_BOOK_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).friendsViaBook),
                pref.getBoolean(PROFILE_FRIEND_VIA_QR_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).friendsViaQR),
                pref.getBoolean(PROFILE_AUTO_DELETE_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).isAutodelete()),
                pref.getFloat(PROFILE_AUTO_DELETE_TRUST_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getAutodeleteTrust()),
                pref.getInt(PROFILE_AUTO_DELETE_AGE_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getAutodeleteAge()),
                pref.getBoolean(PROFILE_SHARE_LOCATIONS_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).isShareLocation()),
                pref.getInt(PROFILE_MIN_SHARED_CONTACTS_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getMinSharedContacts()),
                pref.getInt(PROFILE_MAX_MESSAGES_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getMaxMessages()),
                pref.getInt(PROFILE_COOLDOWN_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getCooldown()),
                pref.getInt(PROFILE_TIMEBOUND_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getTimeboundPeriod()),
                pref.getBoolean(PROFILE_ENFORCE_LOCK_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).isEnforceLock()),
                pref.getBoolean(PROFILE_USE_TRUST_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).isUseTrust()),
                pref.getBoolean(PROFILE_RANDOM_EXCHANGE_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).isRandomExchange()),
                pref.getInt(PROFILE_MIN_CONTACTS_FOR_HOP_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getMinContactsForHop())
        );

        return customProfile;
    }

    /** write the properties of specified profile as the current profile in the local storage.
     * return true if specified name recognized and saved, false otherwise */
    public static boolean setCurrentProfile(Context context, int profileName){
        if(instance == null) getInstance();

        for(SecurityProfile profile : profiles){
            if(profile.getName() == profileName){
                setCurrentProfile(context, profile);
                return true;
            }
        }
        return false;
    }

    /** write the supplied privacy profile to local storage */
    public static void setCurrentProfile(Context context, SecurityProfile profile){
        if(instance == null) getInstance();

        SharedPreferences.Editor pref = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE).edit();
            pref.putInt(PROFILE_NAME_KEY, profile.getName());
            pref.putBoolean(PROFILE_TIMESTAMP_KEY, profile.isTimestamp());
            pref.putBoolean(PROFILE_PSEUDONYM_KEY, profile.isPseudonyms());
            pref.putInt(PROFILE_FEED_SIZE_KEY, profile.getFeedSize());
            pref.putBoolean(PROFILE_FRIEND_VIA_BOOK_KEY, profile.friendsViaBook);
            pref.putBoolean(PROFILE_FRIEND_VIA_QR_KEY, profile.friendsViaQR);
            pref.putBoolean(PROFILE_AUTO_DELETE_KEY, profile.isAutodelete());
            pref.putFloat(PROFILE_AUTO_DELETE_TRUST_KEY, profile.getAutodeleteTrust());
            pref.putInt(PROFILE_AUTO_DELETE_AGE_KEY, profile.getAutodeleteAge());
            pref.putBoolean(PROFILE_SHARE_LOCATIONS_KEY, profile.isShareLocation());
            pref.putInt(PROFILE_MIN_SHARED_CONTACTS_KEY, profile.getMinSharedContacts());
            pref.putInt(PROFILE_MAX_MESSAGES_KEY, profile.getMaxMessages());
            pref.putInt(PROFILE_COOLDOWN_KEY, profile.getCooldown());
            pref.putInt(PROFILE_TIMEBOUND_KEY, profile.getTimeboundPeriod());
            pref.putBoolean(PROFILE_ENFORCE_LOCK_KEY, profile.isEnforceLock());
            pref.putBoolean(PROFILE_USE_TRUST_KEY, profile.isUseTrust());
            pref.putBoolean(PROFILE_RANDOM_EXCHANGE_KEY, profile.isRandomExchange());
            pref.putInt(PROFILE_MIN_CONTACTS_FOR_HOP_KEY, profile.getMinContactsForHop());
            pref.commit();

        MurmurService.TIME_BETWEEN_EXCHANGES_MILLIS = profile.getCooldown() * 1000;
    }

    /** read the currently set user pseudonym from local storage */
    public static String getCurrentPseudonym(Context context){
        if(instance == null) getInstance();

        SharedPreferences pref = context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE);

        if(!pref.contains(PSEUDONYM_KEY)){
            setCurrentPseudonym(context, DEFAULT_PSEUDONYM/*+(System.nanoTime()/System.currentTimeMillis())*/);
        }
        return pref.getString(PSEUDONYM_KEY, DEFAULT_PSEUDONYM);
    }

    /** write the supplied pseudonym to local storage */
    public static void setCurrentPseudonym(Context context, String name){
        if(instance == null) getInstance();

        context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE).edit()
                .putString(PSEUDONYM_KEY, name)
                .commit();
    }

    /** compare the supplied profiles and return the index of the most secure one of the two */
    public int getMostSecureProfile(int profileA, int profileB){
        SecurityProfile profA = getProfile(profileA);
        SecurityProfile profB = getProfile(profileB);

        return (profA.getStrength() > profB.getStrength()) ? profileA : profileB;
    }

    public void clearProfileData(Context context){
        SharedPreferences pref = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);
        pref.edit().clear().commit();
    }

    public static void setStoredMAC(Context context, String MAC){
        if(instance == null) getInstance();

        context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE).edit()
                .putString(MAC_KEY, MAC.toUpperCase())
                .commit();
    }

    public static String getStoredMAC(Context context){
        if(instance == null) getInstance();

        SharedPreferences pref = context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE);

        return pref.getString(MAC_KEY, "");
    }
}