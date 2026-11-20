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
        boolean onContextMenuPrepare(ActionMode mode, Menu menu);
        boolean onContextMenuItemClicked(ActionMode mode, MenuItem item);
        void onContextMenuDestroy(ActionMode mode);
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

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        class CustomCallback extends ActionMode.Callback2 {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                boolean res = callback.onCreateActionMode(mode, menu);
                if (contextMenuListener != null) {
                    contextMenuListener.onContextMenuCreate(mode, menu);
                }
                return res;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                boolean res = callback.onPrepareActionMode(mode, menu);
                if (contextMenuListener != null) {
                    contextMenuListener.onContextMenuPrepare(mode, menu);
                }
                return res;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (contextMenuListener != null) {
                    if (contextMenuListener.onContextMenuItemClicked(mode, item)) {
                        return true;
                    }
                }
                return callback.onActionItemClicked(mode, item);
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                if (contextMenuListener != null) {
                    contextMenuListener.onContextMenuDestroy(mode);
                }
                callback.onDestroyActionMode(mode);
            }

            @Override
            public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
                try {
                    ActionMode.Callback2 callback2 = (ActionMode.Callback2) callback;
                    callback2.onGetContentRect(mode, view, outRect);
                } catch (ClassCastException e) {
                    super.onGetContentRect(mode, view, outRect);
                }
            }
        }

        return super.startActionMode(new CustomCallback(), type);
    }
}
