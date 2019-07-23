package co.epitre.aelf_lectures;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import co.epitre.aelf_lectures.bible.BibleBookEntryLayout;
import co.epitre.aelf_lectures.bible.BibleBookFragment;
import co.epitre.aelf_lectures.bible.BibleBookListAdapter;
import co.epitre.aelf_lectures.bible.BibleFragment;
import co.epitre.aelf_lectures.bible.BibleMenuFragment;


/**
 * Created by jean-tiare on 12/03/18.
 */

public class SectionBibleV2Fragment extends SectionFragmentBase {
    public static final String TAG = "SectionBibleV2Fragment";

    public SectionBibleV2Fragment(){
        // Required empty public constructor
    }

    /**
     * Global managers / resources
     */
    SharedPreferences settings = null;
    BibleFragment mCurrentBibleFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Load settings
        settings = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_section_biblev2, container, false);

        // Get intent link, if any
        Uri uri = activity.getIntent().getData();

        // If there is no state to restore, initialize the Bible section with the menu
        BibleMenuFragment bibleMenuFragment;
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        if (savedInstanceState == null) {
            mCurrentBibleFragment = new BibleMenuFragment();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.bible_container, mCurrentBibleFragment);
            fragmentTransaction.commit();
        } else {
            mCurrentBibleFragment = (BibleFragment)fragmentManager.findFragmentById(R.id.bible_container);
        }

        return view;
    }

    @Override
    public void onLink(Uri uri) {
        // STUB
    }

    //
    // Option menu
    //

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflate the menu; this adds items to the action bar
        inflater.inflate(R.menu.toolbar_biblev2, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                return onShare();
        }
        return super.onOptionsItemSelected(item);
    }

    //
    // API
    //

    public Uri getUri() {
        String route = "";
        if (mCurrentBibleFragment != null) {
            route = mCurrentBibleFragment.getRoute();
        }
        return Uri.parse("https://www.aelf.org/bible"+route);
    }

    public String getTitle() {
        if (mCurrentBibleFragment != null) {
            return mCurrentBibleFragment.getTitle();
        }
        return "Bible de la liturgie";
    }

    public void openBook(int biblePartId, int bibleBookId, BibleBookEntryLayout bibleBookEntryLayout) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        mCurrentBibleFragment = BibleBookFragment.newInstance(biblePartId, bibleBookId);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.bible_container, mCurrentBibleFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    //
    // Lifecycle
    //

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // STUB
    }

    //
    // Events
    //

    @Subscribe
    public void onBibleEntryClick(BibleBookListAdapter.OnBibleEntryClickEvent event) {
        openBook(event.mBiblePartId, event.mBibleBookId, event.mBibleBookEntryLayout);
    }

    public boolean onShare() {
        // Get current URL
        String uri = getUri().toString();

        // Get current webview title
        String title = getTitle();

        // Build share message
        String message = title + ": " + uri;

        // Create the intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)));

        // All done !
        return true;
    }
}
