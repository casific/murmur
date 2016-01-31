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

import java.util.Calendar;

/**
 * Created by Liran on 12/7/2015.
 *
 * This class parse queries sent by the client and convert them into SQL format and vise-versa
 */
public abstract class SearchHelper {

    public static String searchToSQL(String query){
        String userQuery = query;
        String sqlQuery = "";

        if(userQuery.indexOf("@") > -1 && userQuery.length() > 1){
            String[] words = userQuery.split(" ");
            for(String string: words){
                if(string.indexOf("@") > -1 && string.length() > 1){
                    String sqlMatch = matchSQL(MessageStore.COL_PSEUDONYM, string.substring(1, string.length()));

                    if(sqlMatch != null){
                        sqlQuery += " AND "+sqlMatch;
                    }
                    break;
                }
            }
        }

        while(userQuery.lastIndexOf(":") > 0){

            int labelEnd = userQuery.lastIndexOf(":");
            String prequal = userQuery.substring(0, labelEnd);

            int nextSpaceIndex = prequal.lastIndexOf(" ");
            boolean hasSpaceBeforeLabel = nextSpaceIndex >= 0;
            int labelStart = Math.max(1+prequal.lastIndexOf(" "),0);

            String label = userQuery.substring(labelStart, labelEnd);
            String value = userQuery.substring(labelEnd+1);

            userQuery = hasSpaceBeforeLabel ? userQuery.substring(0, labelStart-1) : userQuery.substring(0, labelStart);

            String sqlMatch = matchSQL(label, value);

            if(sqlMatch != null){
                sqlQuery += " AND "+sqlMatch;
            }
        }

        return sqlQuery.length() > 0 ? sqlQuery : null;
    }

    private static String matchSQL(String label, String value){

        if(value == null || value.length() == 0) return null;

        if(label.equals(MessageStore.COL_MESSAGE)){
            return MessageStore.COL_MESSAGE+" LIKE '%"+value+"%'";
        } else if(label.equals(MessageStore.COL_TRUST)){
            try{
                float asFloat = Float.parseFloat(value);
                value = String.valueOf(asFloat/100);
            } catch (Exception e){}
            return MessageStore.COL_TRUST+" >= "+value;
        } else if(label.equals(MessageStore.COL_LIKES)){
            return MessageStore.COL_LIKES +" >= "+value;
        } else if(label.equals(MessageStore.COL_TIMESTAMP)){
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(Utils.convertDateStringCompactToTimstamp(value));
                long timestamp = Utils.reduceCalendarMin(calendar).getTimeInMillis();
                return MessageStore.COL_TIMESTAMP+" <= "+timestamp;
            } catch (Exception e){}
        } else if(label.equals(MessageStore.COL_PSEUDONYM)){
            return MessageStore.COL_PSEUDONYM+" LIKE '%"+value+"%'";
        }

        return null;
    }

}
