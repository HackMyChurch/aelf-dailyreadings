package co.epitre.aelf_lectures;

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
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import co.epitre.aelf_lectures.bible.BibleBookListAdapter;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Load settings
        settings = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Set Section title (Can be anywhere in the class !)
        actionBar.setTitle("Bible V2");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_section_biblev2, container, false);

        // Get intent link, if any
        Uri uri = activity.getIntent().getData();

        // If there is no state to restore, initialize the Bible section with the menu
        BibleMenuFragment bibleMenuFragment;
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        if (savedInstanceState == null) {
            bibleMenuFragment = new BibleMenuFragment();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.bible_container, bibleMenuFragment);
            fragmentTransaction.commit();
        } else {
            bibleMenuFragment = (BibleMenuFragment)fragmentManager.findFragmentById(R.id.bible_container);
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
            // STUB
        }
        return super.onOptionsItemSelected(item);
    }

    //
    // API
    //

    public Uri getUri() {
        // STUB
        return Uri.parse("https://www.aelf.org/bible");
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
        String item = "Click detected on item " + event.mBibleBookId;
        Toast.makeText(getContext(), item, Toast.LENGTH_LONG).show();
    }
}
