package co.epitre.aelf_lectures.components;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AelfWebView extends WebView {

    public interface ContextMenuListener {
        boolean onContextMenuCreate(ActionMode mode, Menu menu);
        boolean onContextMenuItemClicked(ActionMode mode, MenuItem item);
    }

    private ContextMenuListener contextMenuListener = null;

    public AelfWebView(@NonNull Context context) {
        super(context);
    }

    public AelfWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AelfWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void registerContextMenuListener(ContextMenuListener contextMenuListener) {
        this.contextMenuListener = contextMenuListener;
    }

    /*
    * Intercept WebView's default action mode to inject custom actions into the menu. Since Lectures
    * and Bible fragment both uses the same base and the projected "bookmark" menu only makes sense
    * in the Bible part, we have to resort to a callback mechanism to inject custom options and react
    * to event.
    *
    * Inspiration: https://stackoverflow.com/questions/22336903/use-a-custom-contextual-action-bar-for-webview-text-selection
    **/
    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        class CustomCallback extends ActionMode.Callback2 {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Call custom context menu hook
                if (contextMenuListener != null) {
                    if (!contextMenuListener.onContextMenuCreate(mode, menu)) {
                        return false;
                    }
                }
                return callback.onCreateActionMode(mode, menu);
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // Inject the custom action item here
                return callback.onPrepareActionMode(mode, menu);
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                // Call custom context menu hook
                if (contextMenuListener != null) {
                    if (contextMenuListener.onContextMenuItemClicked(mode, item)) {
                        return true;
                    }
                }
                return callback.onActionItemClicked(mode, item);
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                callback.onDestroyActionMode(mode);
            }

            @Override
            public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
                // This is required to get proper positioning
                try {
                    ActionMode.Callback2 callback2 = (ActionMode.Callback2)callback;
                    callback2.onGetContentRect(mode, view, outRect);
                } catch (ClassCastException e) {
                    super.onGetContentRect(mode, view, outRect);
                }
            }
        }

        ActionMode mode = super.startActionMode(new CustomCallback(), type);
        return mode;
    }
}
