package co.epitre.aelf_lectures;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

/**
 * Created by jean-tiare on 21/06/17.
 */

// Monitor network state change. In particular wait for the wifi to be connected
public class NetworkStatusMonitor extends BroadcastReceiver {
    private static final String TAG = "NetworkStatusMonitor";

    //
    // Event interface
    //

    private boolean isWifiAvailable = false;
    private boolean isNetworkAvailable = false;

    public enum NetworkStatusEvent {
        WIFI_ON,
        WIFI_OFF,
        NETWORK_ON,
        NETWORK_OFF
    }

    public interface NetworkStatusChangedListener {
        void onNetworkStatusChanged(NetworkStatusEvent networkStatusEvent);
    }

    List<NetworkStatusChangedListener> networkStatusChangeListeners = new LinkedList();

    // Internal state
    private long listeners = 0;
    private Context ctx = null;
    private ConnectivityManager connectivityManager;

    // Event hook
    public final Object whenNetworkOk = new Object();

    //
    // Singleton
    //

    private static NetworkStatusMonitor networkStatusMonitorInstance;
    private NetworkStatusMonitor() {}
    public static synchronized NetworkStatusMonitor getInstance() {
        if (networkStatusMonitorInstance == null) {
            networkStatusMonitorInstance = new NetworkStatusMonitor();
        }
        return networkStatusMonitorInstance;
    }

    //
    // Public registration API
    //

    public void registerNetworkStatusChangeListener(NetworkStatusChangedListener listener) {
        synchronized (networkStatusChangeListeners) {
            if (networkStatusChangeListeners.contains(listener)) {
                return;
            }
            networkStatusChangeListeners.add(listener);

            // Trigger initial events
            listener.onNetworkStatusChanged(this.isWifiAvailable    ? NetworkStatusEvent.WIFI_ON    : NetworkStatusEvent.WIFI_OFF);
            listener.onNetworkStatusChanged(this.isNetworkAvailable ? NetworkStatusEvent.NETWORK_ON : NetworkStatusEvent.NETWORK_OFF);
        }
    }

    public void unregisterNetworkStatusChangeListener(NetworkStatusChangedListener listener) {
        synchronized (networkStatusChangeListeners) {
            networkStatusChangeListeners.remove(listener);
        }
    }

    public synchronized void register(Context ctx) {
        this.listeners++;
        if (this.listeners > 1) {
            return;
        }

        // Init initial state
        this.ctx = ctx;
        this.connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.isWifiAvailable = this.isWifiAvailable();
        this.isNetworkAvailable = this.isNetworkAvailable();

        // Register listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CONNECTIVITY_ACTION);
        ctx.registerReceiver(this, intentFilter);
    }

    // This will throw a NullPointerException on underflow. This is intentional
    public synchronized void unregister() {
        this.listeners--;
        if (this.listeners > 0) {
            return;
        }

        this.ctx.unregisterReceiver(this);
        this.connectivityManager = null;
        this.ctx = null;
    }

    //
    // Public high level API
    //

    // This will throw a NullPointerException if no listener is connected. This is intentional
    public void waitForWifi() throws InterruptedException {
        synchronized (this.whenNetworkOk) {
            while (!this.isWifiAvailable()) {
                Log.d(TAG, "WiFi is not connected, waiting for it");
                this.whenNetworkOk.wait();
            }
        }
    }

    // This will throw a NullPointerException if no listener is connected. This is intentional
    public void waitForNetwork() throws InterruptedException {
        synchronized (this.whenNetworkOk) {
            while (!this.isNetworkAvailable()) {
                Log.d(TAG, "Network is not connected, waiting for it");
                this.whenNetworkOk.wait();
            }
        }
    }

    public boolean isNetworkAvailable() {
        NetworkInfo activeNetwork = this.connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public boolean isWifiAvailable() {
        NetworkInfo activeNetwork = this.connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    //
    // Internal receiver
    //

    @Override
    public void onReceive(Context context, Intent intent) {
        // Notify synchronous event waiters that something happened
        synchronized (whenNetworkOk) {
            Log.d(TAG, "System tells us that network state just changed");
            whenNetworkOk.notifyAll();
        }

        // Grab new state
        boolean isNetworkAvailableNew = this.isNetworkAvailable();
        boolean isWifiAvailableNew = this.isWifiAvailable();

        // Call async listeners
        for(NetworkStatusChangedListener listener: networkStatusChangeListeners){
            if (isWifiAvailableNew == true && this.isWifiAvailable == false) {
                listener.onNetworkStatusChanged(NetworkStatusEvent.WIFI_ON);
            }
            if (isWifiAvailableNew == false && this.isWifiAvailable == true) {
                listener.onNetworkStatusChanged(NetworkStatusEvent.WIFI_OFF);
            }
            if (isNetworkAvailableNew == true && this.isNetworkAvailable == false) {
                listener.onNetworkStatusChanged(NetworkStatusEvent.NETWORK_ON);
            }
            if (isNetworkAvailableNew == false && this.isNetworkAvailable == true) {
                listener.onNetworkStatusChanged(NetworkStatusEvent.NETWORK_OFF);
            }
        }

        // Commit new state
        this.isWifiAvailable = isWifiAvailableNew;
        this.isNetworkAvailable = isNetworkAvailableNew;
    }
}
