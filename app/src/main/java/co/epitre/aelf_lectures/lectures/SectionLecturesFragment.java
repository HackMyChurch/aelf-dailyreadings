package co.epitre.aelf_lectures.lectures;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.SectionFragmentBase;
import co.epitre.aelf_lectures.components.DatePickerFragment;
import co.epitre.aelf_lectures.components.NetworkStatusMonitor;
import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.OfficeTypes;
import co.epitre.aelf_lectures.lectures.data.WhatWhen;
import co.epitre.aelf_lectures.lectures.data.office.Lecture;
import co.epitre.aelf_lectures.lectures.data.office.LectureVariants;
import co.epitre.aelf_lectures.lectures.data.office.Office;
import co.epitre.aelf_lectures.settings.SettingsActivity;

/**
 * Created by jean-tiare on 05/12/17.
 */

public class SectionLecturesFragment extends SectionFragmentBase implements
        LectureLoadProgressListener,
        DatePickerFragment.CalendarDialogListener,
        NetworkStatusMonitor.NetworkStatusChangedListener
{
    public static final String TAG = "SectionOfficesFragment";

    /**
     * Shared internal state
     */
    private static AelfDate defaultDate = new AelfDate();

    /**
     * Internal state
     */
    WhatWhen whatwhen = null;
    private Office office;
    private boolean isLoading = false;
    private boolean isSuccess = true;
    DownloadOfficeTask currentRefresh = null;
    Lock preventCancel = new ReentrantLock();

    /**
     * Global managers / resources
     */
    NetworkStatusMonitor networkStatusMonitor = NetworkStatusMonitor.getInstance();
    SharedPreferences settings = null;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    protected TabLayout mTabLayout;
    LecturePagerAdapter lecturesPagerAdapter = null;

    // This is called number of screen rotate + 1. The last time with a null argument :/
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Load settings
        Resources res = getResources();
        settings = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());

        // Select where to go from here
        Uri uri = activity.getIntent().getData();
        if (whatwhen != null) {
            // Coming from "back" button. Nothing to do.
        } else if (savedInstanceState != null) {
            whatwhen = new WhatWhen();

            // Restore saved instance state. Especially useful on screen rotate on older phones
            whatwhen.what = OfficeTypes.values()[savedInstanceState.getInt("what", 0)];
            whatwhen.position = savedInstanceState.getInt("position", 0);

            long timestamp = savedInstanceState.getLong("when", 0);
            if (timestamp == 0) {
                Log.e(TAG, "onCreateView: RESTORING TODAY");
                whatwhen.when = new AelfDate();
            } else {
                Log.e(TAG, "onCreateView: restore date");
                whatwhen.when = new AelfDate(timestamp);
            }
        } else if (uri != null) {
            parseIntentUri(uri);
        } else {
            whatwhen = new WhatWhen();

            // Load the lectures for now.
            whatwhen.when = new AelfDate();
            whatwhen.position = 0;

            if (settings.getString(SettingsActivity.KEY_PREF_SYNC_LECTURES, res.getString(R.string.pref_lectures_def)).equals("messe")) {
                whatwhen.what = OfficeTypes.MESSE;
            } else {
                computeCurrentOffice(true);
            }
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_section_offices, container, false);

        // Set the drawer header
        setDrawerHeaderView(R.layout.navigation_drawer_header_offices);

        // Get view handles
        mViewPager = view.findViewById(R.id.pager);
        mTabLayout = view.findViewById(R.id.pager_title_strip);
        mTabLayout.setTabIndicatorFullWidth(true);

        // Setup the chapter selection menu
        mTabLayout.addOnTabSelectedListener(new LectureVariantSelectionListener());

        // Populate the tabs
        mTabLayout.setupWithViewPager(mViewPager, false);

        // Install event handler
        Button cancelButton = view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelLectureLoad();
            }
        });

        // Reset menu handle in case we come from the 'back' button
        mMenu = null;

        // Do we have a set of lectures to restore or do we need to load them ?
        if (this.office == null) {
            // Asynchronously load lecture
            loadLecture(whatwhen);
        } else {
            // Apply the cached lectures
            this.applyOffice();
        }

        // Return view
        return view;
    }

    //
    // Routing
    //

    // TODO: make real routing with "master" router in main activity, then delegate to fragments
    // FIXME: this method has a huge side-effect (sets the whatwhen). Make it a loader ?
    private void parseIntentUri(Uri uri) {
        // Parse intent URI, update whatwhen in place
        // http://www.aelf.org/                                        --> messe du jour, 1ère lecture
        // http://www.aelf.org/#messe1_lecture4                        --> messe du jour, lecture N
        // http://www.aelf.org/2017-01-27/romain/messe                 --> messe du 2017-01-27, calendrier romain
        // http://www.aelf.org/2017-01-27/romain/messe#messe1_lecture3 --> messe du 2017-01-27, calendrier romain, lecture N
        // http://www.aelf.org/2017-01-27/romain/complies              --> office des complies du 2017-01-27
        // http://www.aelf.org/2017-01-27/romain/complies#office_psaume1 --> office_TYPE[N]
        // Shortcut URLs:
        // http://www.aelf.org/shortcut/messe
        // http://www.aelf.org/shortcut/office
        // Legacy shortcut URLs:
        // https://www.aelf.org/office-[NOM]

        String path = uri.getPath();
        if (path == null) {
            path = "";
        }
        String host = uri.getHost();
        if (host == null) {
            host = "";
        }
        String fragment = uri.getFragment();

        // Set default values
        whatwhen = new WhatWhen();
        whatwhen.what = OfficeTypes.MESSE;
        whatwhen.when = new AelfDate();
        whatwhen.position = 0; // 1st lecture of the office

        if (host.equals("www.aelf.org")) {
            // AELF Website
            String[] chunks = path.split("/");

            if (chunks.length == 2 && chunks[1].startsWith("office-")) {
                // Attempt to parse a legacy URL
                String office_name = chunks[1].substring(7).toUpperCase();
                try {
                    whatwhen.what = OfficeTypes.valueOf(office_name);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Failed to parse office '" + chunks[1] + "', falling back to messe", e);
                    whatwhen.what = OfficeTypes.MESSE;
                }
            } else if (chunks[1].equals("shortcut")) {
                if (chunks[2].equals("messe")) {
                    whatwhen.what = OfficeTypes.MESSE;
                } else if (chunks[2].equals("office")) {
                    computeCurrentOffice(false);
                }
            } else {
                // Attempt to parse NEW url format, starting with a date
                if (chunks.length >= 2) {
                    // Does it look like a date ?
                    String potential_date = chunks[1];
                    if (potential_date.matches("20[0-9]{2}-[0-9]{2}-[0-9]{2}")) {
                        String[] date_chunks = potential_date.split("-");
                        whatwhen.when.set(
                                Integer.parseInt(date_chunks[0]),
                                Integer.parseInt(date_chunks[1]) - 1,
                                Integer.parseInt(date_chunks[2])
                        );
                    } else {
                        Log.w(TAG, "String '" + potential_date + "' should look like a date, but it does not!");
                    }
                }

                // Attempt to parse office
                if (chunks.length >= 4) {
                    String office_name = chunks[3].toUpperCase();
                    try {
                        whatwhen.what = OfficeTypes.valueOf(office_name);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Failed to parse office '" + chunks[2] + "', falling back to messe", e);
                        whatwhen.what = OfficeTypes.MESSE;
                    }
                }

                // Finally, grab anchor
                whatwhen.anchor = fragment;
            }
        }
    }

    // Set the office based on the time of day
    void computeCurrentOffice(boolean includeMass) {
        long hour = whatwhen.when.get(Calendar.HOUR_OF_DAY);
        if (hour < 3) {
            whatwhen.what = OfficeTypes.COMPLIES;
            whatwhen.when.add(GregorianCalendar.DAY_OF_YEAR, -1);
        } else if (hour < 4) {
            whatwhen.what = OfficeTypes.LECTURES;
        } else if (hour < 8) {
            whatwhen.what = OfficeTypes.LAUDES;
        } else if (hour < 15 && includeMass) {
            whatwhen.what = OfficeTypes.MESSE;
        } else if (hour < 21) {
            whatwhen.what = OfficeTypes.VEPRES;
        } else {
            whatwhen.what = OfficeTypes.COMPLIES;
        }
    }

    //
    // API
    //

    public Uri getUri() {
        // Make sure we DO have something to share
        // FIXME: racy, the loader will update it and it's in a thread
        if (lecturesPagerAdapter == null || mViewPager == null) {
            return null;
        }

        // Get current lecture
        int position = mViewPager.getCurrentItem();
        Lecture lecture = lecturesPagerAdapter.getLecture(position);

        // Build URL
        return buildUri(whatwhen.what, whatwhen.when, lecture.getKey());
    }

    public static Uri buildUri(OfficeTypes what) {
        return buildUri(what, defaultDate, null);
    }

    public static Uri buildUri(OfficeTypes what, AelfDate when, String key) {
        String url = "http://www.aelf.org/"+when.toIsoString()+"/romain/"+what.urlName();
        if (key != null) {
            url += "#"+key;
        }
        return Uri.parse(url);
    }

    LectureVariants getLectureVariants(int position) {
        if (lecturesPagerAdapter == null) {
            return null;
        }

        return lecturesPagerAdapter.getLectureVariants(position);
    }

    //
    // Lifecycle
    //

    @Override
    public void onResume() {
        super.onResume();
        networkStatusMonitor.registerNetworkStatusChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        networkStatusMonitor.unregisterNetworkStatusChangeListener(this);
        if (mViewPager != null) {
            whatwhen.anchor = null;
            whatwhen.position = mViewPager.getCurrentItem();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        int position = 0; // first slide by default
        int what = 0; // "Messe" by default
        long when = 0;

        if (whatwhen != null) {
            if (whatwhen.what != null) what = whatwhen.what.getPosition();
            if (mViewPager != null) position = mViewPager.getCurrentItem();
            if (whatwhen.when != null) when = whatwhen.when.getTimeInMillis();
        }

        outState.putInt("what", what);
        outState.putInt("position", position);
        outState.putLong("when", when);
        outState.putLong("last-update", System.currentTimeMillis());
    }

    //
    // Views
    //

    private void applyOffice() {
        lecturesPagerAdapter = new LecturePagerAdapter(getChildFragmentManager(), office);
        mViewPager.setAdapter(lecturesPagerAdapter);
        mViewPager.setCurrentItem(whatwhen.position);

        refreshUI(whatwhen);
    }

    public void updateCalendarButtonLabel(WhatWhen whatwhen) {
        if(mMenu == null) {
            return;
        }

        MenuItem calendarItem = mMenu.findItem(R.id.action_calendar);
        if (calendarItem == null) {
            return;
        }

        calendarItem.setTitle(whatwhen.when.toShortPrettyString());
    }

    //
    // Option menu
    //

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflate the menu; this adds items to the action bar
        inflater.inflate(R.menu.toolbar_offices, menu);

        // Update to date button with "this.date"
        updateCalendarButtonLabel(whatwhen);
        updateMenuNetworkVisibility();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_refresh) {
            return onRefresh("menu");
        } else if (itemId == R.id.action_calendar) {
            return onCalendar();
        } else if (itemId == R.id.action_share) {
            return onShare();
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void updateMenuNetworkVisibility() {
        if(mMenu == null) {
            return;
        }

        boolean visible = networkStatusMonitor.isNetworkAvailable();

        for (int menu_id: new int[]{R.id.action_refresh, R.id.action_sync_do}) {
            MenuItem item = mMenu.findItem(menu_id);
            if (item == null) {
                return;
            }
            item.setVisible(visible);
        }
    }

    //
    // Lecture variant selection
    //

    private void showLectureVariantSelectionMenu(View menuAnchor, final int position) {
        Context ctx = getContext();
        if (ctx == null || menuAnchor == null || mTabLayout == null || lecturesPagerAdapter == null) {
            return;
        }

        // Build menu
        final ListPopupWindow listPopupWindow = new ListPopupWindow(ctx);
        listPopupWindow.setAnchorView(menuAnchor);
        listPopupWindow.setDropDownGravity(Gravity.CENTER);
        listPopupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);
        listPopupWindow.setWidth(menuAnchor.getWidth());
        listPopupWindow.setAdapter(new ArrayAdapter(getContext(),
                android.R.layout.simple_list_item_1, lecturesPagerAdapter.getVariantTitles(position)));

        // Handle events
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int variant, long id) {
                listPopupWindow.dismiss();
                lecturesPagerAdapter.setLectureVariantId(position, variant);
            }
        });

        // Display menu
        listPopupWindow.show();
    }

    private class LectureVariantSelectionListener implements TabLayout.OnTabSelectedListener {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (lecturesPagerAdapter == null) {
                return;
            }

            if (!lecturesPagerAdapter.hasVariants(tab.getPosition())) {
                return;
            }

            tab.setIcon(R.drawable.ic_drop_down);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            tab.setIcon(null);
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            if (lecturesPagerAdapter == null) {
                return;
            }

            if (!lecturesPagerAdapter.hasVariants(tab.getPosition())) {
                return;
            }

            showLectureVariantSelectionMenu(tab.view, tab.getPosition());
        }
    }

    //
    // Network
    //

    @Override
    public void onNetworkStatusChanged(NetworkStatusMonitor.NetworkStatusEvent networkStatusEvent) {
        updateMenuNetworkVisibility();
        switch (networkStatusEvent) {
            case NETWORK_ON:
                // Attempt to reload current slide, If there was an error.
                if (!this.isSuccess) {
                    onRefresh("networkUp");
                }
                break;
        }
    }

    //
    // Events
    //

    public boolean onRefresh(String reason) {
        if (whatwhen == null) {
            return true;
        }

        whatwhen.useCache = false;
        whatwhen.anchor = null;
        if (mViewPager != null) {
            whatwhen.position = mViewPager.getCurrentItem();
        } else {
            whatwhen.position = 0;
        }
        loadLecture(whatwhen);
        return true;
    }

    public boolean onCalendar() {
        Bundle args = new Bundle();
        args.putLong("time", whatwhen.when.getTimeInMillis());

        DatePickerFragment calendarDialog = new DatePickerFragment();
        calendarDialog.setListener(this);
        calendarDialog.setArguments(args);
        calendarDialog.show(getChildFragmentManager(), "datePicker");

        return true;
    }

    public boolean onShare() {
        // Make sure we DO have something to share
        // FIXME: racy, the loader will update it and it's in a thread
        if (lecturesPagerAdapter == null || mViewPager == null) {
            return false;
        }

        // Get current lecture
        int position = mViewPager.getCurrentItem();
        Lecture lecture = lecturesPagerAdapter.getLecture(position);

        // Build URL
        String url = getUri().toString();

        // Build the data
        String prettyDate = whatwhen.when.toPrettyString();

        // Build the subject and message
        String message;
        String subject;
        if (whatwhen.what == OfficeTypes.MESSE && whatwhen.when.isToday()) {
            // If this is Today's mass, let's be concise
            message = lecture.getShortTitle();
        } else {
            // Generic case
            message = lecture.getShortTitle()+" "+whatwhen.what.prettyName();

            // Append date if not today
            if (!whatwhen.when.isToday()) {
                message += " " + prettyDate;
            }

            // Append title if defined
            String title = lecture.getTitle();
            if (title != null) {
                message += ": "+title;
            }
        }

        // Append the reference, IF defined AND not the same as the title
        String reference = lecture.getReference();
        if (reference != null) {
            message += " ("+reference+")";
        }

        // Append the link
        message += ". "+url;

        // Generate the subject, let's be concise
        subject = lecture.getShortTitle()+" "+whatwhen.what.prettyName();
        if (!whatwhen.when.isToday()) {
            subject += " " + prettyDate;
        }

        // Create the intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)));

        // All done !
        return true;
    }

    public void onCalendarDialogPicked(int year, int month, int day) {
        AelfDate date = new AelfDate(year, month, day);

        // do not refresh if date did not change to avoid unnecessary flickering
        if (whatwhen.when.isSameDay(date))
            return;

        // Send the new URI to the activity
        Uri uri = buildUri(whatwhen.what, date, null);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        activity.onIntent(intent);
    }

    public void onLink(Uri link) {
        parseIntentUri(link);
        loadLecture(whatwhen);
    }

    //
    // Loader
    //

    protected void refreshUI(WhatWhen whatwhen) {
        actionBar.setTitle(whatwhen.what.actionBarName());
        drawerView.setCheckedItem(whatwhen.what.getMenuId());
        updateCalendarButtonLabel(whatwhen);
    }

    protected void setLoading(final boolean loading) {
        // Do not trigger animations. That causes flickering.
        if (isLoading == loading) {
            return;
        }
        isLoading = loading;

        View view = getView();
        Context ctx = getContext();
        if (ctx == null || view == null) {
            return;
        }

        TypedValue colorValue = new TypedValue();
        ctx.getTheme().resolveAttribute(R.attr.colorLectureAccent, colorValue, true);

        final RelativeLayout loadingOverlay = view.findViewById(R.id.loadingOverlay);
        final ProgressBar loadingIndicator = view.findViewById(R.id.loadingIndicator);
        final Button cancelButton = view.findViewById(R.id.cancelButton);
        final int colorAccent = colorValue.data;

        if(loadingOverlay == null || loadingIndicator == null || cancelButton == null) {
            return;
        }

        loadingOverlay.post(() -> {
            if(loading) {
                Animation fadeIn = new AlphaAnimation(0, 1);
                fadeIn.setInterpolator(new DecelerateInterpolator());
                fadeIn.setStartOffset(500);
                fadeIn.setDuration(500);

                Animation buttonFadeIn = new AlphaAnimation(0, 1);
                buttonFadeIn.setInterpolator(new DecelerateInterpolator());
                buttonFadeIn.setStartOffset(2500);
                buttonFadeIn.setDuration(500);

                loadingIndicator.getIndeterminateDrawable().setColorFilter(colorAccent, android.graphics.PorterDuff.Mode.MULTIPLY);
                cancelButton.setVisibility(View.VISIBLE);
                cancelButton.setAnimation(buttonFadeIn);
                loadingOverlay.setVisibility(View.VISIBLE);
                loadingOverlay.setAnimation(fadeIn);
            } else {
                Animation fadeOut = new AlphaAnimation(1, 0);
                fadeOut.setInterpolator(new DecelerateInterpolator());
                fadeOut.setDuration(250);

                cancelButton.setVisibility(View.GONE);
                loadingOverlay.setVisibility(View.INVISIBLE);
                loadingOverlay.setAnimation(fadeOut);
            }
        });
    }

    public void loadLecture(WhatWhen whatwhen) {
        // Cancel any pending load
        cancelLectureLoad();

        // Refresh UI
        refreshUI(whatwhen);

        // Set the new date as the default (manual action)
        defaultDate = whatwhen.when;

        // Start Loading
        preventCancel.lock();
        try {
            DownloadOfficeTask loader = new DownloadOfficeTask(getContext(), whatwhen, this);
            loader.execute();
            whatwhen.useCache = true; // cache override are one-shot
            currentRefresh = loader;
        } finally {
            preventCancel.unlock();
        }
    }

    public void cancelLectureLoad() {
        preventCancel.lock();
        try {
            currentRefresh.cancel(true);
            Thread.sleep(100); // FIXME!!
        } catch (NullPointerException e) {
            // Asking for permission is racy
        } catch (InterruptedException e) {
        } finally {
            currentRefresh = null;
            setLoading(false); // FIXME: should be in the cancel code path in the task imho
            preventCancel.unlock();
        }
    }

    //
    // Async load callbacks. Guaranted to be called on main UI thread
    //

    public void onLectureLoadProgress(LectureLoadProgress progress) {
        switch (progress) {
            case LOAD_START:
                setLoading(true);
                break;
            case LOAD_FAIL:
                setLoading(false);
                Context context = getContext();
                if(context != null) {
                    Toast.makeText(context, "Oups... Impossible de charger cet office.", Toast.LENGTH_SHORT).show();
                }
                break;
            case LOAD_DONE:
                setLoading(false);
                break;
        }
    }


    public void onLectureLoaded(Office office, boolean isSuccess) {
        preventCancel.lock();
        this.isSuccess = isSuccess;

        try {
            whatwhen.position = -1;

            // If we have an anchor, attempt to find corresponding position
            if (isSuccess) {
                if (whatwhen.anchor != null && office != null) {
                    whatwhen.position = office.getLecturePosition(whatwhen.anchor);
                }
            }

            if (whatwhen.position == -1) {
                whatwhen.position = 0;
            }

            // Set up the ViewPager with the sections adapter.
            try {
                // 1 slide fragment <==> 1 lecture
                this.office = office;
                this.applyOffice();
                setLoading(false);
            } catch (IllegalStateException e) {
                // Fragment manager has gone away, will reload anyway so silently give up
            } finally {
                currentRefresh = null;
                preventCancel.unlock();
            }
        } catch (Exception e) {
            throw e;
        }
    }


}
