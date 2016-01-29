package org.denovogroup.murmur.backend;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by Liran on 9/2/2015.
 */
public class Utils {

    private static Utils instance;
    private static Random random = new Random();

    private Utils(){
        random = new Random();
    }

    /** Convert a timestamp in milliseconds into a human readable string format
     *
     * @param useUTC whether or not to use UTC timezone for creating the string
     * @param milli time in milliseconds to be converted
     * @return human readable string representation of the time stamp using UTC timezone
     * and formatted as yyyy-MM-dd HH:mm:ss
     */
    public static String convertTimestampToDateString(boolean useUTC, long milli){
        Date date = new Date(milli);
        SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sourceFormat.setTimeZone(useUTC ? TimeZone.getTimeZone("UTC") : TimeZone.getDefault());
        return sourceFormat.format(date);
    }

    /** Convert a timestamp in milliseconds into a human readable string format
     *
     * @param useUTC whether or not to use UTC timezone for creating the string
     * @param milli time in milliseconds to be converted
     * @return human readable string representation of the time stamp using UTC timezone
     * and formatted as dd-MM-yyyy
     */
    public static String convertTimestampToDateStringCompact(boolean useUTC, long milli){
        Date date = new Date(milli);
        SimpleDateFormat sourceFormat = new SimpleDateFormat("dd-MM-yyyy");
        sourceFormat.setTimeZone(useUTC ? TimeZone.getTimeZone("UTC") : TimeZone.getDefault());
        return sourceFormat.format(date);
    }

    /** Calculate time difference between current time and supplied value in days
     *
     * @param milli time in milliseconds to be converted
     * @return the number of hours between today and supplied value (always positive)
     */
    public static int convertTimestampToRelativeHours(long milli){

        long todaymilli = Calendar.getInstance().getTimeInMillis();

        return (int)TimeUnit.MILLISECONDS.toHours(Math.abs(milli - todaymilli));
    }

    public static int convertTimestampToRelativeHoursRound(long milli){

        long todaymilli = Calendar.getInstance().getTimeInMillis();

        int hours = (int)TimeUnit.MILLISECONDS.toHours(Math.abs(milli - todaymilli));
        int min = (int)TimeUnit.MILLISECONDS.toMinutes(Math.abs(milli - todaymilli)) - (int)TimeUnit.HOURS.toMinutes(hours);

        float hoursFloat = hours +(min/60f);
        return Math.round(hoursFloat);
    }

    /** Convert a timestamp in compact format (dd-MM-yyyy) into milliseconds
     * @param timestring a string in the format of dd-MM-yyyy
     * @return the amount of milliseconds representing the date in the string
     */
    public static long convertDateStringCompactToTimstamp(String timestring) throws NullPointerException, NumberFormatException{
        int dayOfMonth = Integer.parseInt(timestring.substring(0, 2));
        int month = Integer.parseInt(timestring.substring(3, 5));
        int year = Integer.parseInt(timestring.substring(6));

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month-1);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        cal = reduceCalendar(cal);
        return cal.getTimeInMillis();
    }

    /** sets the supplied calendar object with 0 values for fields lower than days
     *
     * @param calendar the calendar to reduce
     * @return the reduced calendar
     */
    public static Calendar reduceCalendar(Calendar calendar){
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar;
    }

    /** sets the supplied calendar object with 0 values for fields lower than hour
     *
     * @param calendar the calendar to reduce
     * @return the reduced calendar
     */
    public static Calendar reduceCalendarMin(Calendar calendar){

        int roundedHours = Math.round(calendar.get(Calendar.MINUTE)/60f);

        calendar.add(Calendar.HOUR, roundedHours);

        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        return calendar;
    }

    /** convert numeric value (assumed to represent dp size units) into px units
     * based on the device's screen density.
     * @param dp value in dp to be converted
     * @param context the context to get device settings with
     * @return value in px after conversion from dp
     */
    public static int dpToPx(int dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        int px = Math.round(dp * density);
        return px;
    }

    /** convert numeric value (assumed to represent px size units) into dp units
     * based on the device's screen density.
     * @param px value in dp to be converted
     * @param context the context to get device settings with
     * @return value in dp after conversion from px
     */
    public static int pxToDp(int px, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        int dp = Math.round(px / density);
        return dp;
    }

    /**Check if bluetooth connection is enabled
     *
     * @return true if bluetooth connected available and enabled, false otherwise
     */
    public static boolean isBluetoothEnabled(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled());
    }

    /**Check if WIFI connection is enabled
     *
     * @return true if WIFI connected available and enabled, false otherwise
     */
    public static boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return (wifiManager != null && wifiManager.isWifiEnabled());
    }

    /** Check for hashtags in the supplied text and return an array
     * with the hashtags available in the text
     *
     * @param text the string containing hashtags
     * @return Hashtags found in the list (including the # sign)
     */
    public static Set<String> getHashtags(String text){
        Set<String> hashtags = new HashSet<>();

        while(text.contains("#")){

            String hashtag;
            int hashtagStart = text.indexOf("#");
            hashtag = text.substring(hashtagStart);
            int hashtagEnd = hashtag.length();

            charloop:
            for(int i=0; i< hashtag.length(); i++){
                char c = hashtag.charAt(i);
                if((c == ' ') || (c == '\n')){
                    hashtagEnd = i;
                    break charloop;
                }
            }

            if(hashtagEnd < hashtag.length()-1) {
                hashtag = hashtag.substring(0, hashtagEnd);
            }
            hashtags.add(hashtag);
            text = text.substring(hashtagStart + hashtag.length());
        }
        return hashtags;
    }

    /** create a gaussian noise around supplied mean value based on supplied standardDiviation
     *
     * @param mean The value that most values will tend to cluster around
     * @param standardDiviation The value representing the top and bottom limit of approximately 70% of
     *                 the results (i.e 70% of the results will be between -standardDiviation and standardDiviation)
     * @return random noise value
     */
    public static synchronized double makeNoise(double mean, double standardDiviation){
        return random.nextGaussian()*Math.sqrt(standardDiviation) + mean;
    }

    /** escape forbidden sql characters and return the reformed version*/
    public static String makeTextSafeForSQL(String source){
        if(source == null) return  null;
        String reformedText = source.replaceAll("'","''");
        return reformedText;
    }

    /** breaks a string into single words, ignoring spaces */
    public static String[] getKeywords(String string){
        //check if not just blanks
        if(string == null || string.replaceAll("\\s","").length() == 0) return null;

        //remove line breaks
        String digest = string.replaceAll("[\n\"]", " ");

        //remove spaces at the start of the string
        while(digest.charAt(0) == ' '){
            digest = digest.substring(1);
        }

        String[] words = digest.split("\\s");
        ArrayList<String> retwords = new ArrayList<>();
        for(String word : words)
        {
            if(word.length() > 0)
                retwords.add(word);
        }

        if(retwords.size() > 0)
            return retwords.toArray(new String[retwords.size()]);

        return null;
    }
}
