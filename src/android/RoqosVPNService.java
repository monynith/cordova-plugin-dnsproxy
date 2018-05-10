package com.roqos.cordova.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.system.OsConstants;
import android.util.Log;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import android.widget.Toast;

public class RoqosVPNService extends VpnService implements Runnable {

    public static final String ACTION_ACTIVATE = "com.roqos.RoqosVpnService.ACTION_ACTIVATE";
    public static final String ACTION_DEACTIVATE = "com.roqos.RoqosVpnService.ACTION_DEACTIVATE";

    private Thread mThread = null;
    public HashMap<String, String> dnsServers;

    private static final int NOTIFICATION_ACTIVATED = 0;

    private static boolean activated = false;

    private NotificationCompat.Builder notification = null;

    public static String primaryServer;
    public static String secondaryServer;

    private ParcelFileDescriptor descriptor;

    private Provider provider;

    @Override
    public void onCreate() {
        Log.d("RoqosVPNService", " onCreate");
        super.onCreate();
    }

    public static boolean isActivated() {
        return activated;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)

    @Override
    public void run() {
        Log.d("RoqosVPNService", " run");
        Builder builder = new Builder()
                .setSession(Roqos.VPNSession);

        String format = null;
        for (String prefix : new String[]{"10.0.0", "192.0.2", "198.51.100", "203.0.113", "192.168.50"}) {
            try {
                builder.addAddress(prefix + ".1", 24);
            } catch (IllegalArgumentException e) {
                continue;
            }

            format = prefix + ".%d";
            break;
        }

        byte[] ipv6Template = new byte[]{32, 1, 13, (byte) (184 & 0xFF), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        if (primaryServer.contains(":") || secondaryServer.contains(":")) {//IPv6
            try {
                InetAddress addr = Inet6Address.getByAddress(ipv6Template);
                Log.d("DNSPlugin", "configure: Adding IPv6 address" + addr);
                builder.addAddress(addr, 120);
            } catch (Exception e) {
                ipv6Template = null;
            }
        } else {
            ipv6Template = null;
        }

        InetAddress aliasPrimary;
        InetAddress aliasSecondary;
        dnsServers = new HashMap<>();
        aliasPrimary = addDnsServer(builder, format, ipv6Template, InetAddress.getByName(primaryServer));
        aliasSecondary = addDnsServer(builder, format, ipv6Template, InetAddress.getByName(primaryServer));

        InetAddress primaryDNSServer = null;
        try {
            InetAddress primaryDNSServer = aliasPrimary;
            InetAddress secondaryDNSServer = aliasSecondary;
            builder.addDnsServer(primaryDNSServer).addDnsServer(primaryDNSServer);

            builder.setBlocking(true);
            builder.allowFamily(OsConstants.AF_INET);
            builder.allowFamily(OsConstants.AF_INET6);

            Log.d("DNSPlugin", "Roqos VPN service is started at " + primaryDNSServer.getHostAddress());

            descriptor = builder.establish();

            provider = new UdpProvider(descriptor, this);
            provider.start();
            provider.process();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


    }

    private InetAddress addDnsServer(Builder builder, String format, byte[] ipv6Template, InetAddress address) throws UnknownHostException {
        int size = dnsServers.size();
        size++;
        if (address instanceof Inet6Address && ipv6Template == null) {
            Log.i("RoqosVPNService", "addDnsServer: Ignoring DNS server " + address);
        } else if (address instanceof Inet4Address) {
            String alias = String.format(format, size + 1);
            dnsServers.put(alias, address.getHostAddress());
            builder.addRoute(alias, 32);
            return InetAddress.getByName(alias);
        } else if (address instanceof Inet6Address) {
            ipv6Template[ipv6Template.length - 1] = (byte) (size + 1);
            InetAddress i6addr = Inet6Address.getByAddress(ipv6Template);
            dnsServers.put(i6addr.getHostAddress(), address.getHostAddress());
            return i6addr;
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if(intent.getAction() == ACTION_ACTIVATE){
                activated = true;
                NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

                Intent nIntent = new Intent(this, DnsPlugin.class);
                PendingIntent pIntent = PendingIntent.getActivity(this, 0, nIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setWhen(0)
//                        .setContentTitle(getResources().getString(R.string.notice_activated))
                        .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
//                        .setSmallIcon(R.drawable.ic_security)
                        // .setColor(getResources().getColor(R.color.colorPrimary)) //backward compatibility
                        .setAutoCancel(false)
                        .setOngoing(true)
//                        .setTicker(getResources().getString(R.string.notice_activated))
                        .setContentIntent(pIntent);

                Notification notification = builder.build();

                if (this.mThread == null) {
                    this.mThread = new Thread(this, "RoqosVpn");
//                    this.running = true;
                    this.mThread.start();
                }
//                manager.notify(NOTIFICATION_ACTIVATED, notification);
                this.notification = builder;

//                this.notification = builder;
//                if (MainActivity.getInstance() != null) {
//                    MainActivity.getInstance().startActivity(new Intent(getApplicationContext(), MainActivity.class)
//                            .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_SERVICE_DONE));
//                }
                return START_STICKY;
            }
            else {
                stopThread();
                return START_NOT_STICKY;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopThread();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void stopThread() {
//        Log.d(TAG, "stopThread");
        activated = false;
        boolean shouldRefresh = false;
        try {
            if (this.descriptor != null) {
                this.descriptor.close();
                this.descriptor = null;
            }
            if (mThread != null) {
                shouldRefresh = true;
                if (provider != null) {
                    provider.shutdown();
                    mThread.interrupt();
                    provider.stop();
                } else {
                    mThread.interrupt();
                }
                mThread = null;
            }
            if (notification != null) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(NOTIFICATION_ACTIVATED);
                notification = null;
            }
            dnsServers = null;
        } catch (Exception e) {

        }
        stopSelf();

        if (shouldRefresh) {
            RuleResolver.clear();
            DNSServerHelper.clearPortCache();
        }
    }

    @Override
    public void onRevoke() {
        stopThread();
    }

    public static class VpnNetworkException extends Exception {
        public VpnNetworkException(String s) {
            super(s);
        }

        public VpnNetworkException(String s, Throwable t) {
            super(s, t);
        }

    }

}
