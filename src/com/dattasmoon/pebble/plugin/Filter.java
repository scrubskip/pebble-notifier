/* 
Copyright (c) 2013 Justin Koh

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.dattasmoon.pebble.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

/**
 * Represents a filter which acts on a notification.
 */
public class Filter implements Parcelable {

    private Pattern mTitleFilter;
    private Pattern mTextFilter;

    public void setTitleFilter(Pattern pattern) {
        mTitleFilter = pattern;
    }

    public void setTextFilter(Pattern pattern) {
        mTextFilter = pattern;
    }

    public boolean isFilterMatch(String title, String text) {
        if (mTitleFilter == null && mTextFilter == null) {
            // Both null, no match.
            return false;
        }

        boolean titleMatch = true;
        if (!TextUtils.isEmpty(getTitleFilterString())) {
            titleMatch = mTitleFilter.matcher(title).find();
        }
        boolean textMatch = true;
        if (!TextUtils.isEmpty(getTextFilterString())) {
            textMatch = mTextFilter.matcher(text).find();
        }

        return titleMatch && textMatch;
    }

    JSONObject toJsonObject() {
        JSONObject object = new JSONObject();
        try {
            object.put("title", mTitleFilter != null ? mTitleFilter.pattern() : null);
            object.put("text", mTextFilter != null ? mTextFilter.pattern() : null);
            return object;
        } catch (JSONException e) {
            Log.w(Constants.LOG_TAG, "Could not serialize filter");
        }
        return null;
    }

    private static Filter fromJsonObject(JSONObject object) {
        Filter filter = new Filter();
        String titleFilter = object.optString("title");
        String textFilter = object.optString("text");
        if (titleFilter != null) {
            filter.setTitleFilter(Pattern.compile(titleFilter));
        }
        if (textFilter != null) {
            filter.setTextFilter(Pattern.compile(textFilter));
        }
        return filter;
    }

    public static String toJsonString(List<Filter> filters) {
        JSONArray objects = new JSONArray();
        try {
            if (filters != null) {
                int index = 0;
                for (Filter filter : filters) {
                    objects.put(index++, filter.toJsonObject());
                }
            }
            return objects.toString();
        } catch (JSONException e) {
            Log.w(Constants.LOG_TAG, "Could not serialize filters");
        }
        return null;
    }

    public static List<Filter> fromJsonString(String serializedString) {
        List<Filter> returnList = new ArrayList<Filter>();
        try {
            JSONArray objects = new JSONArray(serializedString);
            final int size = objects.length();
            for (int index = 0; index < size; ++index) {
                returnList.add(Filter.fromJsonObject(objects.getJSONObject(index)));
            }
        } catch (JSONException e) {
            Log.w(Constants.LOG_TAG, "Could not deserialize filters");
        }
        return returnList;
    }

    public String getTitleFilterString() {
        return mTitleFilter != null ? mTitleFilter.pattern() : null;
    }

    public String getTextFilterString() {
        return mTextFilter != null ? mTextFilter.pattern() : null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(getTitleFilterString())) {
            builder.append("Title: " + mTitleFilter.pattern());
        }
        if (!TextUtils.isEmpty(getTextFilterString())) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append("Text: " + mTextFilter.pattern());
        }
        return builder.toString();
    }

    public static Parcelable.Creator<Filter> CREATOR = new Parcelable.Creator<Filter>() {

                                                         @Override
                                                         public Filter createFromParcel(Parcel source) {
                                                             Filter filter = new Filter();
                                                             String pattern = source.readString();
                                                             if (pattern != null) {
                                                                 filter.setTitleFilter(Pattern.compile(pattern));
                                                             }
                                                             pattern = source.readString();
                                                             if (pattern != null) {
                                                                 filter.setTextFilter(Pattern.compile(pattern));
                                                             }
                                                             return filter;
                                                         }

                                                         @Override
                                                         public Filter[] newArray(int size) {
                                                             return new Filter[size];
                                                         }
                                                     };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitleFilter != null ? mTitleFilter.pattern() : null);
        dest.writeString(mTextFilter != null ? mTextFilter.pattern() : null);
    }
}
