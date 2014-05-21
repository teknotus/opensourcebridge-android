package org.opensourcebridge.navigator;
import java.util.Date;

import org.json.JSONArray;

public class Event {
    // TODO Fetch tracks from OCW.
    private static final int TRACK_BUSINESS = 26;
    private static final int TRACK_CHEMISTRY = 27;
    private static final int TRACK_COOKING = 28;
    private static final int TRACK_CULTURE = 29;
    private static final int TRACK_HACKS = 30;
    private static final int TRACK_BOF = 31;

    public Date start, end;
    public String description;
    public String title;
    public String url;
    public String location;
    public String brief;
    public String id;
    public int track;
    public String speakers;
    public JSONArray speaker_ids;

    public Event(){
        start = null;
        end = null;
        description = null;
        title = null;
        url = null;
        location = null;
        track = -1;
        speakers = null;
        speaker_ids = null;
    }

    public String getTrackName() {
        // TODO Fetch tracks from OCW.
        switch(track) {
        case TRACK_BUSINESS:
            return "Business";
        case TRACK_CHEMISTRY:
            return "Chemistry";
        case TRACK_COOKING:
            return "Cooking";
        case TRACK_CULTURE:
            return "Culture";
        case TRACK_HACKS:
            return "Hacks";
        case TRACK_BOF:
            return "BOF";
        default:
            return "";
        }
    }

    /**
     * @return the resource id for the track color
     */
    public int getTrackColor() {
        // TODO Fetch tracks from OCW.
        switch(track) {
        case TRACK_BUSINESS:
            return R.color.track_business;
        case TRACK_CHEMISTRY:
            return R.color.track_chemistry;
        case TRACK_COOKING:
            return R.color.track_cooking;
        case TRACK_CULTURE:
            return R.color.track_culture;
        case TRACK_HACKS:
            return R.color.track_hacks;
        case TRACK_BOF:
            return R.color.track_bof;
        default:
            return R.color.track_other;
        }
    }

    /**
    * @return the resource id for the track color darker shade
    */
    public int getTrackColorDark() {
       // TODO Fetch tracks from OCW.
       switch(track) {
       case TRACK_BUSINESS:
               return R.color.track_business_dark;
       case TRACK_CHEMISTRY:
               return R.color.track_chemistry_dark;
       case TRACK_COOKING:
               return R.color.track_cooking_dark;
       case TRACK_CULTURE:
               return R.color.track_culture_dark;
       case TRACK_HACKS:
               return R.color.track_hacks_dark;
       case TRACK_BOF:
               return R.color.track_bof_dark;
       default:
               return R.color.track_other_dark;
       }
    }
}
