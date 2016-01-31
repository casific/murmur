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
package org.denovogroup.murmur.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.denovogroup.murmur.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Liran on 1/19/2016.
 */
public class DebugAdapter extends ArrayAdapter<String[]>{

    int direction = 0;
    String connecting;

    public DebugAdapter(Context context, List<String[]> objects, int direction, String connecting) {
        super(context, R.layout.debug_list_item, objects);
        this.direction = direction;
        this.connecting = connecting;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.debug_list_item, parent, false);
        }

        String[] currentItem = getItem(position);

        ((TextView) convertView.findViewById(R.id.peer_mac)).setText(currentItem[0]);
        ((TextView) convertView.findViewById(R.id.peer_backoff)).setText(currentItem[2] != null ? currentItem[2] : "No backoff");

        boolean speakTo = currentItem[1] != null;

        convertView.findViewById(R.id.speak_to).setVisibility(speakTo ? View.VISIBLE : View.GONE);
        convertView.findViewById(R.id.spoken_of).setVisibility(!speakTo ? View.VISIBLE : View.GONE);

        long lastExchangeTime = currentItem[3] != null ? Long.parseLong(currentItem[3]) : -1;

        String lastExchangeTimeString = "Last exchange:";

        if(lastExchangeTime < 0){
            lastExchangeTimeString+=" Unknown";
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(lastExchangeTime);
            SimpleDateFormat format = new SimpleDateFormat("h:mm:ss a");
            lastExchangeTimeString += format.format(cal.getTime());
        }

        ((TextView)convertView.findViewById(R.id.last_exchange)).setText(lastExchangeTimeString);

        if(direction > 0 && connecting != null && currentItem[0].contains(connecting)){
            convertView.setBackground(new ColorDrawable(Color.parseColor("#00DD00")));
        } else if(direction < 0 && connecting != null && currentItem[0].contains(connecting)){
            convertView.setBackground(new ColorDrawable(Color.parseColor("#DD0000")));
        } else {
            convertView.setBackground(new ColorDrawable(Color.parseColor("#00000000")));
        }

        return convertView;
    }
}
