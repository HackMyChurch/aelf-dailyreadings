package co.epitre.aelf_lectures.base;

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

import co.epitre.aelf_lectures.LecturesActivity;
import co.epitre.aelf_lectures.R;

/**
 * Created by jean-tiare on 7/01/18.
 */

public abstract class SectionFragment extends Fragment {

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

    public void onSearch(String query) {}

    //
    // Option menu (TODO: move to base class + overload)
    //

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenu = menu;
    }

    protected View inflateDrawerHeaderView(@LayoutRes int res) {
        LayoutInflater inflater = getLayoutInflater();
        return inflater.inflate(res, drawerView, false);
    }

    protected void setDrawerHeaderView(@LayoutRes int res) {
        View view = inflateDrawerHeaderView(res);
        if (view != null) {
            setDrawerHeaderView(view);
        }
    }

    protected void setDrawerHeaderView(@NonNull View view) {
        while(drawerView.getHeaderCount() > 0) {
            drawerView.removeHeaderView(drawerView.getHeaderView(0));
        }
        drawerView.addHeaderView(view);
    }

    protected View getDrawerHeaderView() {
        if(drawerView.getHeaderCount() == 0) {
            return null;
        }
        return drawerView.getHeaderView(0);
    }
}
