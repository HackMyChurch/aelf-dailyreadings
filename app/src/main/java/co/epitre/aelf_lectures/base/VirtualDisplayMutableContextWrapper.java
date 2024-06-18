package co.epitre.aelf_lectures.base;

import android.content.Context;
import android.content.MutableContextWrapper;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.util.DisplayMetrics;
import android.view.Display;

import androidx.annotation.Nullable;

/**
 * After a webview has been attached to a view, the system somehow remembers it, even after
 * removing all identified references. When changing the display properties, typically when
 * changing the phone orientation, Android will call `onConfigurationChanged` and ultimately
 * trigger a call to `getDisplay` on the webview.
 * <p>
 * When the webview is released, we detach the context and swap it back to the application
 * context to avoid leaking a context. However, this context does not implement the `getDisplay`
 * call.
 * <p>
 * Returning `null` is not enough. We need a real display.
 * <p>
 * This class is a Hack to handle this situation. It allocates a private virtual display as a
 * stub so that all function succeed.
 */
public class VirtualDisplayMutableContextWrapper extends MutableContextWrapper {
    private static Display mStubDisplay;

    public VirtualDisplayMutableContextWrapper(Context base) {
        super(base);
        maybeInitializeStubDisplay(base);
    }

    @Nullable
    @Override
    public Display getDisplay() {
        try {
            return super.getDisplay();
        } catch (UnsupportedOperationException ignored) {
            return mStubDisplay;
        }
    }

    //
    // Internals
    //

    private static synchronized void maybeInitializeStubDisplay(Context ctx) {
        // Skip if already initialized
        if (mStubDisplay != null) {
            return;
        }

        // Initialize stub display
        DisplayManager displayManager = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
        DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();

        VirtualDisplay virtualDisplay = displayManager.createVirtualDisplay(
                "StubDisplay", // Name of the virtual display
                1, 1, // Width and height
                displayMetrics.densityDpi, // Screen density. Must be the same as the real screen.
                null, // Surface for the virtual display
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY // Flags
        );

        mStubDisplay = virtualDisplay.getDisplay();
    }
}
