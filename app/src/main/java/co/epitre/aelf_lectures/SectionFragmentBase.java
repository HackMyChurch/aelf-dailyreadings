package co.epitre.aelf_lectures;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

/**
 * Created by jean-tiare on 7/01/18.
 */

public abstract class SectionFragmentBase extends Fragment {

    /**
     * Global Views
     */
    protected ActionBar actionBar;
    protected NavigationView drawerView;
    protected LecturesActivity activity;
    protected Menu mMenu;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Load global views
        activity = (LecturesActivity) requireActivity();
        actionBar = activity.getSupportActionBar();
        drawerView = activity.findViewById(R.id.drawer_navigation_view);

        // Option menu
        setHasOptionsMenu(true);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    //
    // Callback
    //

    public abstract Uri getUri();

    //
    // Events
    //

    public boolean onRefresh(String reason) {
        return true;
    }

    public void onLink(Uri link) {
        // NOOP
    }

    public void onSearch(String query) {}

    //
    // Option menu (TODO: move to base class + overload)
    //

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenu = menu;
    }

    protected void setDrawerHeaderView(@LayoutRes int res) {
        while(drawerView.getHeaderCount() > 0) {
            drawerView.removeHeaderView(drawerView.getHeaderView(0));
        }
        drawerView.inflateHeaderView(res);
    }
}
