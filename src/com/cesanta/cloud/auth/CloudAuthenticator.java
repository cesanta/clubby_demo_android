
package com.cesanta.cloud.auth;

import java.util.Set;
import java.util.UUID;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class CloudAuthenticator {

    private final String cloudHost = "cloud3.cesanta.com";
    private final String appHost = "foobar.com";

    private final Context context;
    private Dialog dialog = null;

    //TODO: remove
    private static final String TAG = "clubby";

    public CloudAuthenticator(Context context) {
        this.context = context;
    }

    public void auth(final Listener listener) {
        CookieSyncManager.createInstance(context);
        CookieSyncManager.getInstance().startSync();

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        CookieManager.setAcceptFileSchemeCookies(true);

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        dialog.setTitle("Cesanta Cloud Auth");
        dialog.setCancelable(true);

        //TODO: take drawable id as a param
        /*
        dialog.setFeatureDrawableResource(
                Window.FEATURE_LEFT_ICON,
                R.drawable.ic_launcher
                );
                */

        LinearLayout layout = new LinearLayout(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL
                );
        dialog.addContentView(
                layout,
                params
                );

        layout.setLayoutParams(params);

        final ProgressBar pb = new ProgressBar(
                context, null, android.R.attr.progressBarStyleHorizontal
                );
        pb.setLayoutParams(
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                );
        layout.addView(pb);

        WebView wv = new WebView(context);
        wv.setLayoutParams(
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                );
        layout.addView(wv);

        dialog.setContentView(layout);

        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        wv.getSettings().setSavePassword(false);

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.v(TAG, "Loading url: " + url);

                Uri uri = Uri.parse(url);

                Set<String> names = uri.getQueryParameterNames();

                if (appHost.equals(uri.getHost())
                        && "/clubby_auth".equals(uri.getPath())
                        && names.contains("id")
                        && names.contains("token")
                        )
                {
                    // Append a random component to the id
                    String id = uri.getQueryParameter("id")
                        + "." + UUID.randomUUID().toString();
                    // Notify listener
                    listener.onAuthenticated(new Credentials(
                                id,
                                uri.getQueryParameter("token")
                                ));
                    dialog.dismiss();
                    return true;
                }

                // Actually it would be appropriate to just return `false`
                // here, but it doesn't work on Android 6.0
                // (but works on Android 4.3)
                view.loadUrl(url);
                return true;
            }

        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                //Log.v(TAG, "Progress: " + String.valueOf(newProgress));
                pb.setProgress(newProgress);
                if (newProgress >= 100) {
                    pb.setVisibility(View.GONE);
                } else {
                    pb.setVisibility(View.VISIBLE);
                }
            }
        });

        wv.loadUrl("https://" + cloudHost + "/auth/token?redirect_to=https://" + appHost);

        dialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface di) {
                listener.onCancelled();
            }

        });

        dialog.show();
    }

    public void clearAuthData() {
        //TODO(dfrank): more fine-grained cleanup
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }

    public boolean isDialogVisible() {
        return (dialog != null && dialog.isShowing());
    }

    /* TODO(dfrank): make it work or remove */
    private void clearCookies(String domain) {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookiestring = cookieManager.getCookie("https://" + domain);
        String[] cookies =  cookiestring.split(";");
        for (int i=0; i<cookies.length; i++) {
            String[] cookieparts = cookies[i].split("=");
            cookieManager.setCookie(domain, cookieparts[0].trim()+"=; Expires=Wed, 31 Dec 2025 23:59:59 GMT");
        }
        CookieSyncManager.getInstance().sync();
    }

    public static class Credentials {
        public String id;
        public String key;

        Credentials(String id, String key) {
            this.id = id;
            this.key = key;
        }
    }

    public static interface Listener {
        public void onAuthenticated(Credentials cred);
        public void onCancelled();
    }
}

