package dev.sololearn.test.util;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;

/**
 * listens for network state change by BroadCastReceiver(e.g connected,disconnected and)
 */
public class NetworkStateReceiver extends BroadcastReceiver {

    private List<NetworkStateListener> listeners;

    public NetworkStateReceiver() {
        listeners = new ArrayList<>();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            List<NetworkStateListener> copyList;
            synchronized (this) {
                copyList = new ArrayList<>(listeners);
            }
            for (NetworkStateListener listener : copyList) {
                listener.onNetworkAvailable(this);
            }
        } else if (activeNetInfo == null) {
            List<NetworkStateListener> copyList;
            synchronized (this) {
                copyList = new ArrayList<>(listeners);
            }
            for (NetworkStateListener listener : copyList) {
                listener.onNetworkDisconnected(this);
            }
        }
    }

    public synchronized void addListener(NetworkStateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(NetworkStateListener listener) {
        listeners.remove(listener);
    }

    public interface NetworkStateListener {
        void onNetworkAvailable(NetworkStateReceiver receiver);

        void onNetworkDisconnected(NetworkStateReceiver receiver);
    }

    public static class NetworkState {
        public static final int NETWORK_CONNECTED = 1;
        public static final int NETWORK_DIS_CONNECTED = 2;
        private int networkState;

        public NetworkState(int networkState) {
            this.networkState = networkState;
        }

        public int getNetworkState() {
            return networkState;
        }

        public void setNetworkState(int networkState) {
            this.networkState = networkState;
        }
    }
}
