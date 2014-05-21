package org.opensourcebridge.navigator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class Schedule extends Activity {

    // Cache files for 2 hours (in milliseconds)
    private static final long CACHE_TIMEOUT = 7200000;

    // TODO Fetch dates from OCW.
    // TODO Refactor dates as array.
    private static final Date DAY1 = new Date(114, 5, 24);
    private static final Date DAY2 = new Date(114, 5, 25);
    private static final Date DAY3 = new Date(114, 5, 26);
    private static final Date DAY4 = new Date(114, 5, 27);

    // TODO Generate menu items from dates fetched from OCW.
    private static final int MENU_DAY1 = 1;
    private static final int MENU_DAY2 = 2;
    private static final int MENU_DAY3 = 3;
    private static final int MENU_DAY4 = 4;

    private static final int MENU_NEXT = 5;
    private static final int MENU_PREV = 6;
    private static final int MENU_ABOUT = 7;
    private static final int MENU_NOW = 8;
    private static final int MENU_REFRESH = 9;

    // state
    Date mCurrentDate;
    TextView mDate;
    boolean mDetail = false;
    private Handler mHandler;

    // session list
    EventAdapter mAdapter;
    ListView mEvents;

    // screen animation
    ViewFlipper mFlipper;
    Animation mInLeft;
    Animation mInRight;
    Animation mOutLeft;
    Animation mOutRight;

    // session details
    Event mEvent = null;
    HashMap<Integer, Speaker> mSpeakers;
    View mHeader;
    TextView mTitle;
    TextView mTime;
    TextView mLocation;
    View mTimeLocation;
    TextView mSpeaker;
    ScrollView mDescriptionScroller;
    TextView mDescription;
    LinearLayout mBio;

    // session detail actions
    Button mShare;
    Button mShowDescription;
    Button mShowBio;

    private static final String SCHEDULE_URI = "http://opensourcebridge.org/events/2014/schedule.json";
    private static final String SPEAKER_URI_BASE = "http://opensourcebridge.org/users/";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mHandler = new Handler();

        mSpeakers = new HashMap<Integer, Speaker>();

        mDate = (TextView) findViewById(R.id.date);
        mEvents = (ListView) findViewById(R.id.events);
        mFlipper = (ViewFlipper) findViewById(R.id.flipper);
        Context context = getApplicationContext();
        mInLeft = AnimationUtils.loadAnimation(context, R.anim.slide_in_left);
        mInRight = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
        mOutLeft = AnimationUtils.loadAnimation(context, R.anim.slide_out_left);
        mOutRight = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);

        // grab views for details
        View detail = findViewById(R.id.detail);
        mHeader = findViewById(R.id.detail_header);
        mSpeaker = (TextView) findViewById(R.id.speaker);
        mTitle = (TextView) detail.findViewById(R.id.title);
        mTimeLocation = detail.findViewById(R.id.time_location);
        mTime = (TextView) detail.findViewById(R.id.time);
        mLocation = (TextView) detail.findViewById(R.id.location);
        mDescription = (TextView) detail.findViewById(R.id.description);
        mDescriptionScroller = (ScrollView) detail.findViewById(R.id.description_scroller);
        mBio = (LinearLayout) detail.findViewById(R.id.bio);

        // detail action buttons
        mShare = (Button) findViewById(R.id.share);
        mShowDescription = (Button) findViewById(R.id.show_description);
        mShowBio = (Button) findViewById(R.id.show_bio);

        mEvents.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
                Object item = mAdapter.mFiltered.get(position);
                if (item instanceof Date) {
                    return;// ignore clicks on the dates
                }
                Event event = (Event) item;
                Context context = getApplicationContext();
                mHeader.setBackgroundColor(context.getResources().getColor(event.getTrackColor()));
                mTitle.setText(event.title);
                mLocation.setText(event.location);
                DateFormat startFormat = new SimpleDateFormat("E, h:mm");
                DateFormat endFormat = new SimpleDateFormat("h:mm a");
                String timeString = startFormat.format(event.start) + " - " + endFormat.format(event.end);
                mTime.setText(timeString);
                mSpeaker.setText(event.speakers);
                mTimeLocation.setBackgroundColor(context.getResources().getColor(event.getTrackColorDark()));
                mDescription.setText(event.description);
                show_description();
                mDescriptionScroller.scrollTo(0, 0);
                mFlipper.setInAnimation(mInRight);
                mFlipper.setOutAnimation(mOutLeft);
                mFlipper.showNext();
                mEvent = event;
                mDetail = true;
            }
        });

        mShowDescription.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                show_description();
            }
        });

        mShowBio.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mBio.removeAllViews();
                JSONArray speaker_ids = mEvent.speaker_ids;
                if (speaker_ids != null) {
                    for (int i=0; i<speaker_ids.length(); i++) {
                        try {
                            View view = loadBioView(speaker_ids.getInt(i));
                            if (view != null) {
                                if (i>0){
                                    view.setPadding(0, 30, 0, 0);
                                }
                                mBio.addView(view);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    mDescription.setVisibility(View.GONE);
                    mBio.setVisibility(View.VISIBLE);
                }
            }

            /**
             * loads a view populated with the speakers info
             * @param id
             * @return
             */
            private View loadBioView(int sid) {
                Integer id = new Integer(sid);
                Speaker speaker = null;
                View view = null;
                // check memory to see if speaker had already been loaded
                // else load the speaker from persistent storage
                if (mSpeakers.containsKey(id)){
                    speaker = mSpeakers.get(id);
                } else {
                    try {
                        String raw = getURL(SPEAKER_URI_BASE + id + ".json", false);
                        JSONObject json = new JSONObject(raw);
                        speaker = new Speaker(json.getJSONObject("user"));
                        mSpeakers.put(id, speaker);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        // file couldn't be loaded
                    }
                }

                // create view
                if (speaker != null) {
                    LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = vi.inflate(R.layout.bio, null);
                    TextView name = (TextView) view.findViewById(R.id.name);
                    name.setText(speaker.name);
                    TextView biography = (TextView) view.findViewById(R.id.biography);
                    biography.setText(speaker.biography);

                    String twitter = speaker.twitter;
                    if (twitter != null && twitter != ""  && twitter != "null"){
                        TextView text = (TextView) view.findViewById(R.id.twitter);
                        text.setText(twitter);
                        View parent = (View) text.getParent();
                        parent.setVisibility(View.VISIBLE);
                    }

                    String website = speaker.website;
                    if (website != null && website != "" && website != "null"){
                        TextView text = (TextView) view.findViewById(R.id.website);
                        text.setText(speaker.website);
                        View parent = (View) text.getParent();
                        parent.setVisibility(View.VISIBLE);
                    }

                    String blog = speaker.blog;
                    if (blog != null && blog != "" && blog != "null"){
                        TextView text = (TextView) view.findViewById(R.id.blog);
                        text.setText(speaker.blog);
                        View parent = (View) text.getParent();
                        parent.setVisibility(View.VISIBLE);
                    }

                    if (speaker.affiliation != null){
                        TextView text = (TextView) view.findViewById(R.id.affiliation);
                        text.setText(speaker.affiliation);
                    }

                    String identica = speaker.identica;
                    if (identica != null && identica != "" && identica != "null"){
                        TextView text = (TextView) view.findViewById(R.id.identica);
                        text.setText(speaker.identica);
                        View parent = (View) text.getParent();
                        parent.setVisibility(View.VISIBLE);
                    }
                }

                return view;
            }
        });

        mShare.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                Resources r = getApplicationContext().getResources();
                intent.putExtra(Intent.EXTRA_SUBJECT, r.getString(R.string.share_subject));
                intent.putExtra(Intent.EXTRA_TEXT, r.getString(R.string.share_text) + mTitle.getText() + r.getString(R.string.share_text2));
                startActivity(Intent.createChooser(intent, "Share"));
            }
        });

        // spawn loading into separate thread
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadSchedule(true);
                now();
                }
        });
    }

    /**
     * Shows the session description, hides all other subviews
     */
    private void show_description(){
        mBio.setVisibility(View.GONE);
        mDescription.setVisibility(View.VISIBLE);
    }

    /**
     * overridden to hook back button when on the detail page
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent  event){
        if (mDetail && keyCode == KeyEvent.KEYCODE_BACK){
            showList();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /* Creates the menu items */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_PREV, 0, "Previous Day").setIcon(R.drawable.ic_menu_back);
        SubMenu dayMenu = menu.addSubMenu("Day").setIcon(android.R.drawable.ic_menu_today);

        // TODO Generate days menu from data fetched from OCW.
        dayMenu.add(0, MENU_DAY1, 0, this.getDayAsString(DAY1));
        dayMenu.add(0, MENU_DAY2, 0, this.getDayAsString(DAY2));
        dayMenu.add(0, MENU_DAY3, 0, this.getDayAsString(DAY3));
        dayMenu.add(0, MENU_DAY4, 0, this.getDayAsString(DAY4));

        menu.add(0, MENU_NEXT, 0, "Next Day").setIcon(R.drawable.ic_menu_forward);
        menu.add(0, MENU_NOW, 0, "Now").setIcon(android.R.drawable.ic_menu_mylocation);
        menu.add(0, MENU_REFRESH, 0, "Refresh").setIcon(R.drawable.ic_menu_refresh);
        menu.add(0, MENU_ABOUT, 0, "About").setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    /* Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Manage per-day schedules dynamically, rather than hard coding them in.
        switch (item.getItemId()) {
        case MENU_NOW:
            now();
            return true;
        case MENU_DAY1:
            setDay(DAY1);
            return true;
        case MENU_DAY2:
            setDay(DAY2);
            return true;
        case MENU_DAY3:
            setDay(DAY3);
            return true;
        case MENU_DAY4:
            setDay(DAY4);
            return true;
        case MENU_PREV:
            previous();
            return true;
        case MENU_NEXT:
            next();
            return true;
        case MENU_ABOUT:
            showDialog(0);
            return true;
        case MENU_REFRESH:
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    loadSchedule(true);
                    now();
                    }
            });
            return true;
        }
        return false;
    }

    public static final DateFormat date_formatter = new SimpleDateFormat("E, MMMM d");

    /* Returns string with date formatted like "Friday, June 24". */
    public String getDayAsString(Date date) {
        return date_formatter.format(date);
    }

    /* sets the current day, filtering the list if need be */
    public void setDay(Date date) {
        if (isSameDay(mCurrentDate, date)) {
            // same day, just jump to current time
            mAdapter.now(date);
        } else {
            // different day, update the list
            mCurrentDate = date;
            mAdapter.filterDay(date);
            mDate.setText(this.getDayAsString(mCurrentDate));
        }

        // take user back to the listings if not already there
        showList();
    }

    /**
     * Jumps the user to right now in the event list:
     *
     *    - if its before or after the conference, it shows the beginning
     *      of day 1
     *    - if its during the conference it will show the first event
     *      currently underway
     */
    public void now(){
        Date now = new Date();
        if (now.before(DAY1) || now.after(DAY4)) {
            setDay(DAY1);
        } else {
            // use now, since it will have the time of day for
            // jumping to the right time
            setDay(now);
        }
    }

    /**
     * Jumps to the next day, if not already at the end
     */
    public void next() {
        if (isSameDay(mCurrentDate, DAY1)) {
            setDay(DAY2);
        } else if (isSameDay(mCurrentDate, DAY2)) {
            setDay(DAY3);
        } else if (isSameDay(mCurrentDate, DAY3)) {
            setDay(DAY4);
        }
    }

    /**
     * Jumps to the previous day if now already at the beginning
     */
    public void previous() {
        if (isSameDay(mCurrentDate, DAY4)) {
            setDay(DAY3);
        } else if (isSameDay(mCurrentDate, DAY3)) {
            setDay(DAY2);
        } else if (isSameDay(mCurrentDate, DAY2)) {
            setDay(DAY1);
        }
    }

    /**
     * Shows the event listing
     */
    public void showList() {
        if (mDetail) {
            mFlipper.setInAnimation(mInLeft);
            mFlipper.setOutAnimation(mOutRight);
            mFlipper.showPrevious();
            mDetail=false;
        }
    }

    /**
     * Loads the osbridge schedule from a combination of ICal and json data
     * @param force - force reload
     */
    private void loadSchedule(boolean force) {
        //XXX set date to a day that is definitely, not now.
        //    This will cause it to update the list immediately.
        mCurrentDate = new Date(1900, 0, 0);
        ICal calendar = new ICal();
        parseProposals(calendar, force);
        mAdapter = new EventAdapter(this, R.layout.listevent, calendar.getEvents());
        mEvents.setAdapter(mAdapter);
    }

    /**
     * fetches a url and returns it as a string.  This method will cache the
     * result locally and use the cache on repeat loads
     * @param uri - a uri beginning with http://
     * @param force - force refresh of data
     * @return
     */
    private String getURL(String uri, boolean force) throws IOException{
        InputStream is = null;
        OutputStream os = null;
        Context context = getApplicationContext();

        // get file path for cached file
        String dir = context.getFilesDir().getAbsolutePath();
        String path = uri.substring(uri.lastIndexOf("/")+1);
        File file = new File(dir+"/"+path);
        String line;
        StringBuilder sb = new StringBuilder();
        try {
            // determine whether to open local file or remote file
            if (file.exists() && file.lastModified()+CACHE_TIMEOUT > System.currentTimeMillis() && !force){
                is = new FileInputStream(file);
            } else {
                URL url = new URL(uri);
                URLConnection conn = null;
                try {
                    conn = url.openConnection();
                    conn.setDoInput(true);
                    conn.setUseCaches(false);
                    is = conn.getInputStream();
                    os = context.openFileOutput(path, Context.MODE_PRIVATE);
                } catch (IOException e) {
                    // fall back to local file if exists, regardless of age
                    if (file.exists()) {
                        is = new FileInputStream(file);
                    } else {
                        throw e;
                    }
                }
            }

            // read entire file, write cache at same time if we are fetching from the remote uri
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8192);
            OutputStreamWriter bw = null;
            if (os != null) {
                bw = new OutputStreamWriter(os);
            }
            while ((line = br.readLine()) != null) {
                sb.append(line);
                if (bw != null) {
                    bw.append(line);
                }
            }
            if (bw != null) {
                bw.flush();
            }

        } catch (IOException e) {
            // failure to get file, throw this higher
            throw e;

        } finally {
            if (is!=null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os!=null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }

    /**
     * parse events from json file and update the given calendar
     * @param calendar
     * @param force - force refresh
     */
    private void parseProposals(ICal calendar, boolean force){
        ArrayList<Event> events = new ArrayList<Event>();
        try{
            String raw_json = getURL(SCHEDULE_URI, force);
            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss-'07:00'");
            JSONObject schedule = new JSONObject(raw_json);
            JSONArray json_events = schedule.getJSONArray("items");
            int size = json_events.length();
            for(int i=0; i<size; i++){
                JSONObject json = json_events.getJSONObject(i);
                if(json.has("schedule_item")) {
                    Event event = parseScheduleItem(dateFormatter, json.getJSONObject("schedule_item"));
                    events.add(event);
                }
                if(json.has("proposal")) {
                    Event event = parseScheduleItem(dateFormatter, json.getJSONObject("proposal"));
                    events.add(event);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // unable to get file, show error to user
            // TODO
        }
        calendar.setEvents(events);
    }

    public Event parseScheduleItem(DateFormat formatter, JSONObject json)
        throws JSONException, ParseException {

        Event event = new Event();

        event.id = json.getString("event_id");
        event.title = json.getString("title");
        event.description = json.getString("description")
                    .replace("\r","")
                    .replace("<br>","\n")
                    .replace("<blockquote>","")
                    .replace("</blockquote>","")
                    .replace("<b>","")
                    .replace("</b>","");
        if (event.description.equals("")){
            //XXX fill description with spaces, fixes a bug where android will
            //    center the logo on the detail page without content in description
            event.description = "                                                                                  ";
        }
        event.start = formatter.parse(json.getString("start_time"));
        event.end = formatter.parse(json.getString("end_time"));
        event.location = json.getString("room_title");
        if (event.location == "null"){
            event.location = "";
        }
        if (json.has("track_id")){
            event.track = json.getInt("track_id");
        } else {
            event.track = -1;
        }
        if (json.has("user_titles")){
            StringBuilder speakers = new StringBuilder();
            JSONArray speakers_json = json.getJSONArray("user_titles");
            for(int z=0; z<speakers_json.length(); z++){
                String speaker = speakers_json.getString(z);
                if (z>0){
                    speakers.append(", ");
                }
                speakers.append(speaker);
            }
            event.speakers = speakers.toString();
        }
        if (json.has("user_ids")){
            event.speaker_ids = json.getJSONArray("user_ids");
        }
        return event;
    }

    @Override
    protected Dialog onCreateDialog(int id){
        Context context = getApplicationContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.about, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About");
        builder.setCancelable(true);
        builder.setView(v);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        final AlertDialog alert = builder.create();
        return alert;
    }

    /**
     * EventAdapter used for displaying a list of events
     *
     */
    private class EventAdapter extends ArrayAdapter<Event> {
        private final ArrayList<Event> mItems;
        private ArrayList<Object> mFiltered;

        public EventAdapter(Context context, int textViewResourceId,
                ArrayList<Event> items) {
            super(context, textViewResourceId, items);
            this.mItems = items;
            mFiltered = new ArrayList<Object>();
        }

        /**
         * Filters the list to contain only events for the given date.
         * @param date - date to filter by
         */
        public void filterDay(Date date){
            ArrayList<Event> items = mItems;
            ArrayList<Object> filtered = new ArrayList<Object>();
            int size = mItems.size();
            Date currentStart = null;
            for (int i=0; i<size; i++){
                Event event = items.get(i);
                if(isSameDay(date, event.start)){
                    if(currentStart == null || event.start.after(currentStart)) {
                        currentStart = event.start;
                        filtered.add(currentStart);
                    }
                    filtered.add(event);
                }
            }
            mFiltered = filtered;
            notifyDataSetChanged();
            now(date);
        }

        /**
         * sets the position to the current time
         * @param date
         */
        public void now(Date date) {
            ArrayList<Object> filtered = mFiltered;
            int size = filtered.size();
            for (int i=0; i<size; i++){
                Object item = filtered.get(i);

                // find either the first session that hasn't ended yet
                // or the first time marker that hasn't occured yet.
                if (item instanceof Date ){
                    Date slot = (Date) item;
                    if (date.before(slot)) {
                        mEvents.setSelection(i);
                        return;
                    }
                } else {
                    Event event = (Event) item;
                    if (event.end.after(date)) {
                        // should display the time marker instead of the
                        // session
                        mEvents.setSelection(i-1);
                        return;
                    }
                }
            }

            // no current event was found, jump to the next day
            next();
        }

        @Override
        public int getCount(){
            return mFiltered.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            Object item = mFiltered.get(position);
            if (item instanceof Date) {
                Date date = (Date)item;
                v = vi.inflate(R.layout.list_slot, null);
                TextView time = (TextView) v.findViewById(R.id.time);
                DateFormat formatter = new SimpleDateFormat("h:mm a");
                time.setText(formatter.format(date));
            } else {
                Event e = (Event) item;
                v = vi.inflate(R.layout.listevent, null);
                if (e != null) {
                    TextView title = (TextView) v.findViewById(R.id.title);
                    TextView location = (TextView) v.findViewById(R.id.location);
                    TextView time = (TextView) v.findViewById(R.id.time);
                    if (title != null) {
                        title.setText(e.title);
                    }
                    if (location != null) {
                        location.setText(e.location);
                    }
                    if (time != null) {
                        DateFormat formatter = new SimpleDateFormat("h:mm");
                        time.setText(formatter.format(e.start) + "-" + formatter.format(e.end));
                    }
                    if (e.track != -1) {
                        Context context = getApplicationContext();
                        TextView track = (TextView) v.findViewById(R.id.track);
                        track.setTextColor(context.getResources().getColor(e.getTrackColor()));
                        track.setText(e.getTrackName());
                    }
                }
            }
            return v;
        }
    }

    /**
     * Checks if two dates are the same day
     * @param date1
     * @param date2
     * @return
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }

    /**
     * Checks if two calendars are the same day
     * @param cal1
     * @param cal2
     * @return
     */
    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }
}
