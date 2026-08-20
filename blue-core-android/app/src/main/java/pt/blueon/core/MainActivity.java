package pt.blueon.core;

import android.app.Activity;
import android.app.Dialog;
import android.print.PrintManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private WebView mainWebView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER = 1001;
    private final List<Dialog> popupDialogs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainWebView = new WebView(this);
        setContentView(mainWebView);
        configureWebView(mainWebView);
        mainWebView.loadUrl("file:///android_asset/index.html");
    }

    private void configureWebView(WebView webView) {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new BlueBridge(webView), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;
                if (url.startsWith("file:///android_asset/") || url.startsWith("about:blank") || url.startsWith("blob:")) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectAndroidSupport(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(intent, FILE_CHOOSER);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                final Dialog dialog = new Dialog(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
                final WebView child = new WebView(MainActivity.this);
                configureWebView(child);
                dialog.setContentView(child, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                dialog.setOnDismissListener(d -> {
                    try { child.destroy(); } catch (Exception ignored) {}
                    popupDialogs.remove(dialog);
                });
                popupDialogs.add(dialog);
                dialog.show();
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(child);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                for (Dialog d : new ArrayList<>(popupDialogs)) {
                    if (d.isShowing()) {
                        d.dismiss();
                        break;
                    }
                }
            }
        });
    }

    private void injectAndroidSupport(WebView view) {
        String js = "(function(){" +
                "if(!window.AndroidBridge||window.__blueAndroidReady)return;" +
                "window.__blueAndroidReady=true;" +
                "var oldRevoke=URL.revokeObjectURL.bind(URL);" +
                "URL.revokeObjectURL=function(u){setTimeout(function(){try{oldRevoke(u)}catch(e){}},6000)};" +
                "var oldClick=HTMLAnchorElement.prototype.click;" +
                "HTMLAnchorElement.prototype.click=function(){" +
                "var a=this,h=a.href||'',n=a.download||'BLUE_CORE';" +
                "if(a.download&&h.indexOf('blob:')===0){" +
                "fetch(h).then(function(r){return r.blob()}).then(function(b){var f=new FileReader();f.onload=function(){AndroidBridge.saveDataUrl(n,String(f.result))};f.readAsDataURL(b)}).catch(function(){AndroidBridge.toast('Não foi possível exportar o ficheiro.')});return;}" +
                "return oldClick.call(a);};" +
                "window.print=function(){AndroidBridge.printPage()};" +
                "})();";
        view.evaluateJavascript(js, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER || fileCallback == null) return;
        Uri[] result = null;
        if (resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                result = new Uri[count];
                for (int i = 0; i < count; i++) result[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
        }
        fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    @Override
    public void onBackPressed() {
        for (Dialog d : new ArrayList<>(popupDialogs)) {
            if (d.isShowing()) {
                d.dismiss();
                return;
            }
        }
        if (mainWebView != null && mainWebView.canGoBack()) mainWebView.goBack();
        else super.onBackPressed();
    }

    private class BlueBridge {
        private final WebView webView;
        BlueBridge(WebView webView) { this.webView = webView; }

        @JavascriptInterface
        public void toast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void printPage() {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                if (pm != null) pm.print("BLUE CORE", webView.createPrintDocumentAdapter("BLUE CORE"), null);
            });
        }

        @JavascriptInterface
        public void saveDataUrl(String filename, String dataUrl) {
            try {
                int comma = dataUrl.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException("data URL inválido");
                String header = dataUrl.substring(0, comma);
                String mime = "application/octet-stream";
                if (header.startsWith("data:") && header.contains(";")) mime = header.substring(5, header.indexOf(';'));
                byte[] bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
                String safeName = (filename == null || filename.trim().isEmpty()) ? "BLUE_CORE_export" : filename.replaceAll("[\\\\/:*?\"<>|]", "_");

                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BLUE_CORE");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IllegalStateException("sem destino");
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new IllegalStateException("sem acesso");
                    out.write(bytes);
                }
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Guardado em Downloads/BLUE_CORE", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Não foi possível guardar o ficheiro.", Toast.LENGTH_LONG).show());
            }
        }
    }
}
