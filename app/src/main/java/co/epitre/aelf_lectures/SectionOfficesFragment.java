package co.epitre.aelf_lectures;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import co.epitre.aelf_lectures.data.AelfDate;
import co.epitre.aelf_lectures.data.LectureItem;
import co.epitre.aelf_lectures.data.LecturesController;
import co.epitre.aelf_lectures.data.WhatWhen;

import static co.epitre.aelf_lectures.data.WhatWhen.DATE_TODAY;

/**
 * Created by jean-tiare on 05/12/17.
 */

public class SectionOfficesFragment extends SectionFragmentBase implements
        LectureLoadProgressListener,
        DatePickerFragment.CalendarDialogListener,
        NetworkStatusMonitor.NetworkStatusChangedListener
{
    public static final String TAG = "SectionOfficesFragment";

    /**
     * Internal state
     */
    WhatWhen whatwhen;
    WhatWhen whatwhen_previous = null;
    private boolean isLoading = false;
    private boolean isSuccess = true;
    DownloadXmlTask currentRefresh = null;
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
    LecturePagerAdapter lecturesPagerAdapter = null;

    // This is called number of screen rotate + 1. The last time with a null argument :/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO: pass routing argument instead of loading intent

        super.onCreateView(inflater, container, savedInstanceState);

        // Load settings
        Resources res = getResources();
        settings = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Load arguments, if any
        Bundle arguments = getArguments();
        if (arguments == null && canRestoreState(savedInstanceState)) {
            arguments = savedInstanceState;
        }

        // Select where to go from here
        whatwhen = new WhatWhen();

        Uri uri = activity.getIntent().getData();
        if (uri != null) {
            parseIntentUri(uri);
        } else if (arguments != null) {
            // Restore saved instance state. Especially useful on screen rotate on older phones
            whatwhen.what = LecturesController.WHAT.values()[arguments.getInt("what", 0)];
            whatwhen.position = arguments.getInt("position", 0);

            long timestamp = arguments.getLong("when", DATE_TODAY);
            if (timestamp == DATE_TODAY) {
                whatwhen.when = new AelfDate();
                whatwhen.today = true;
            } else {
                whatwhen.when = new AelfDate(timestamp);
                whatwhen.today = false;
            }
        } else {
            // Load the lectures for today. Based on the anonymous statistics
            whatwhen.when = new AelfDate();
            whatwhen.today = true;
            whatwhen.position = 0;

            if (settings.getString(SyncPrefActivity.KEY_PREF_SYNC_LECTURES, res.getString(R.string.pref_lectures_def)).equals("messe")) {
                whatwhen.what = LecturesController.WHAT.MESSE;
            } else {
                long hour = whatwhen.when.get(Calendar.HOUR_OF_DAY);
                if (hour < 3) {
                    whatwhen.what = LecturesController.WHAT.COMPLIES;
                    whatwhen.when.add(GregorianCalendar.DAY_OF_YEAR, -1);
                } else if (hour < 4) {
                    whatwhen.what = LecturesController.WHAT.LECTURES;
                } else if (hour < 8) {
                    whatwhen.what = LecturesController.WHAT.LAUDES;
                } else if (hour < 15) {
                    whatwhen.what = LecturesController.WHAT.MESSE;
                } else if (hour < 21) {
                    whatwhen.what = LecturesController.WHAT.VEPRES;
                } else {
                    whatwhen.what = LecturesController.WHAT.COMPLIES;
                }
            }
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_section_offices, container, false);

        // Get view handles
        mViewPager = view.findViewById(R.id.pager);

        // Install event handler
        Button cancelButton = view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelLectureLoad(true);
            }
        });

        // Load lecture
        loadLecture(whatwhen);

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
        // Legacy shortcut URLs:
        // https://www.aelf.org/office-[NOM]

        String path = uri.getPath();
        String host = uri.getHost();
        String fragment = uri.getFragment();

        // Set default values
        whatwhen.what = LecturesController.WHAT.MESSE;
        whatwhen.when = new AelfDate();
        whatwhen.today = true;
        whatwhen.position = 0; // 1st lecture of the office

        if (host.equals("www.aelf.org")) {
            // AELF Website
            String[] chunks = path.split("/");

            if (chunks.length == 2 && chunks[1].startsWith("office-")) {
                // Attempt to parse a legacy URL
                String office_name = chunks[1].substring(7).toUpperCase();
                try {
                    whatwhen.what = LecturesController.WHAT.valueOf(office_name);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Failed to parse office '" + chunks[2] + "', falling back to messe", e);
                    whatwhen.what = LecturesController.WHAT.MESSE;
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
                        whatwhen.what = LecturesController.WHAT.valueOf(office_name);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Failed to parse office '" + chunks[2] + "', falling back to messe", e);
                        whatwhen.what = LecturesController.WHAT.MESSE;
                    }
                }

                // Finally, grab anchor
                whatwhen.anchor = fragment;
            }
        }
    }

    //
    // Lifecycle
    //

    @Override
    public void onRestore() {
        if (whatwhen != null) {
            refreshUI(whatwhen);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        networkStatusMonitor.registerNetworkStatusChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        networkStatusMonitor.unregisterNetworkStatusChangeListener(this);
    }

    private boolean canRestoreState(Bundle savedInstanceState) {
        // We cant restore the state if
        // - null
        // - today AND more than an hour old
        // The rational is, when praying on a day to day basis, you want to support short pauses and
        // screen rotation but still open automatically on the most probable office, among the most
        // often prayed. On the other hand, if the date was explicitely chosen to a specific one like
        // when preparing the mass for the following sunday, we want to re-open it so that the user
        // does not have to search for it.
        if (savedInstanceState == null) {
            return false;
        }

        long whenTimestamp = savedInstanceState.getLong("when", DATE_TODAY);
        long lastUpdateTimestamp = savedInstanceState.getLong("last-update", 0);
        boolean wasToday = (whenTimestamp == DATE_TODAY);

        // Not today ? Keep it!
        if (!wasToday) {
            return true;
        }

        // Less than 1 hour old ? Keep it
        if ((System.currentTimeMillis() - lastUpdateTimestamp) < 1*3600*1000) {
            return true;
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (outState == null) return;

        int position = 0; // first slide by default
        int what = 0; // "Messe" by default
        long when = DATE_TODAY;

        if (whatwhen != null) {
            if (whatwhen.what != null) what = whatwhen.what.getPosition();
            if (mViewPager != null) position = mViewPager.getCurrentItem();
            if (whatwhen.when != null && !whatwhen.today && !whatwhen.when.isToday()) {
                when = whatwhen.when.getTimeInMillis();
            }
        }

        outState.putInt("what", what);
        outState.putInt("position", position);
        outState.putLong("when", when);
        outState.putLong("last-update", System.currentTimeMillis());
    }

    //
    // Views
    //

    public void updateCalendarButtonLabel(WhatWhen whatwhen) {
        if(mMenu == null) {
            return;
        }
        MenuItem calendarItem = mMenu.findItem(R.id.action_calendar);
        calendarItem.setTitle(whatwhen.when.toShortPrettyString());
    }

    //
    // Option menu
    //

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflate the menu; this adds items to the action bar
        inflater.inflate(R.menu.toolbar_offices, menu);

        // Make the share image white
        Drawable normalDrawable = ContextCompat.getDrawable(activity, R.drawable.ic_share_black_24dp);
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity, R.color.white));

        // Update to date button with "this.date"
        updateCalendarButtonLabel(whatwhen);
        updateMenuNetworkVisibility();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                return onRefresh("menu");
            case R.id.action_calendar:
                return onCalendar();
            case R.id.action_share:
                return onShare();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateMenuNetworkVisibility() {
        if(mMenu == null) {
            return;
        }

        boolean visible = networkStatusMonitor.isNetworkAvailable();
        mMenu.findItem(R.id.action_refresh).setVisible(visible);
        mMenu.findItem(R.id.action_sync_do).setVisible(visible);
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
        whatwhen.useCache = false;
        whatwhen.anchor = null;
        if (mViewPager != null) {
            whatwhen.position = mViewPager.getCurrentItem();
        } else {
            whatwhen.position = 0;
        }
        this.whatwhen_previous = null;
        loadLecture(whatwhen);
        return true;
    }

    public boolean onCalendar() {
        Bundle args = new Bundle();
        args.putLong("time", whatwhen.when.getTimeInMillis());

        DatePickerFragment calendarDialog = new DatePickerFragment();
        calendarDialog.setListener(this);
        calendarDialog.setArguments(args);
        calendarDialog.show(activity.getSupportFragmentManager(), "datePicker");

        return true;
    }

    public boolean onShare() {
        // Make sure we DO have something to share
        // FIXME: racy, the loader will update it and it's in a thread
        if (lecturesPagerAdapter == null || mViewPager == null) {
            return false;
        }

        // Get current position
        int position = mViewPager.getCurrentItem();
        LectureItem lecture = lecturesPagerAdapter.getLecture(position);

        // Build URL
        String url = "http://www.aelf.org/"+whatwhen.when.toIsoString()+"/romain/"+whatwhen.what.aelfUrlName();
        if (lecture.key != null) {
            url += "#"+lecture.key;
        }

        // Build the data
        String prettyDate = whatwhen.when.toPrettyString();

        // Build the subject and message
        String message;
        String subject;
        if (whatwhen.what == LecturesController.WHAT.MESSE && whatwhen.today) {
            // If this is Today's mass, let's be concise
            if (lecture.title != null) {
                message = lecture.title;
            } else {
                message = lecture.shortTitle;
            }
        } else {
            // Generic case
            message = lecture.shortTitle+" "+whatwhen.what.prettyName();

            // Append date if not today
            if (!whatwhen.today) {
                message += " " + prettyDate;
            }

            // Append title if defined
            if (lecture.title != null) {
                message += ": "+lecture.title;
            }
        }

        // Append the reference, IF defined AND not the same as the title
        if (lecture.reference != null && !lecture.reference.equals("") && !lecture.reference.equalsIgnoreCase(lecture.shortTitle)) {
            message += " ("+lecture.reference+")";
        }

        // Append the link
        message += ". "+url;

        // Generate the subject, let's be concise
        subject = lecture.shortTitle+" "+whatwhen.what.prettyName();
        if (!whatwhen.today) {
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

        // Reset pager
        this.whatwhen_previous = whatwhen.copy();
        whatwhen.today = date.isToday();
        whatwhen.when = date;
        whatwhen.position = 0;
        whatwhen.anchor = null;

        loadLecture(whatwhen);
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
        if (view == null) {
            return;
        }

        TypedValue colorValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.colorLectureAccent, colorValue, true);

        final RelativeLayout loadingOverlay = view.findViewById(R.id.loadingOverlay);
        final ProgressBar loadingIndicator = view.findViewById(R.id.loadingIndicator);
        final Button cancelButton = view.findViewById(R.id.cancelButton);
        final int colorAccent = colorValue.data;

        loadingOverlay.post(new Runnable() {
            public void run() {
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
            }
        });
    }

    // Navigate to new office, keep the current date. Used to navigate from menu
    public boolean loadLecture(LecturesController.WHAT what) {
        if (what == null) {
            return false;
        }

        whatwhen.what = what;
        whatwhen.position = 0; // on what change, move to 1st
        whatwhen.anchor = null;
        whatwhen_previous = whatwhen.copy();

        // Load
        this.loadLecture(whatwhen);
        return true;
    }

    public void loadLecture(WhatWhen whatwhen) {
        // Cancel any pending load
        cancelLectureLoad(false);

        // Refresh UI
        refreshUI(whatwhen);

        // Start Loading
        preventCancel.lock();
        try {
            DownloadXmlTask loader = new DownloadXmlTask(getContext(), whatwhen, this);
            loader.execute();
            whatwhen.useCache = true; // cache override are one-shot
            currentRefresh = loader;
        } finally {
            preventCancel.unlock();
        }
    }

    public void cancelLectureLoad(boolean restore) {
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

        // Restore readings
        if (restore && whatwhen_previous != null) {
            whatwhen = whatwhen_previous;
            whatwhen_previous = null;
            whatwhen.useCache = true; // Make it fast, we are restoring !

            // Load lectures
            loadLecture(whatwhen);
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
                Toast.makeText(getContext(), "Oups... Impossible de rafraîchir.", Toast.LENGTH_SHORT).show();
                break;
            case LOAD_DONE:
                setLoading(false);
                break;
        }
    }


    public void onLectureLoaded(List<LectureItem> lectures, boolean isSuccess) {
        preventCancel.lock();
        this.isSuccess = isSuccess;
        try {
            // If we have an anchor, attempt to find corresponding position
            if (isSuccess) {
                if (whatwhen.anchor != null && lectures != null) {
                    int position = -1;
                    for (LectureItem lecture : lectures) {
                        position++;
                        if (whatwhen.anchor.equals(lecture.key)) {
                            whatwhen.position = position;
                            break;
                        }
                    }
                }
            } else {
                whatwhen.position = 0;
            }

            // Set up the ViewPager with the sections adapter.
            try {
                // 1 slide fragment <==> 1 lecture
                lecturesPagerAdapter = new LecturePagerAdapter(activity.getSupportFragmentManager(), lectures);
                FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();

                mViewPager.setAdapter(lecturesPagerAdapter);
                mViewPager.setCurrentItem(whatwhen.position);

                transaction.commit();
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
