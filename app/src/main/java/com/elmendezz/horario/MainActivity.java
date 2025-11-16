package com.elmendezz.horario;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.splashscreen.SplashScreenViewProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private WebView miWebView;
    private LinearLayout errorPanel;
    private TextView errorDetails;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String IS_FIRST_RUN = "isFirstRun";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));

        super.onCreate(savedInstanceState);

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        EdgeToEdge.enable(this); // Habilita edge-to-edge para la actividad principal
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // --- INICIO DE CAMBIOS: ANIMACIÓN DE SALIDA DEL SPLASH SCREEN ---
        splashScreen.setOnExitAnimationListener(splashScreenView -> {
            final View view = splashScreenView.getView();
            final ObjectAnimator slideUp = ObjectAnimator.ofFloat(
                    view,
                    View.TRANSLATION_Y,
                    0f,
                    -view.getHeight()
            );
            slideUp.setInterpolator(new AccelerateInterpolator());
            slideUp.setDuration(800L); // Duración de la animación

            slideUp.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    splashScreenView.remove();
                }
            });
            slideUp.start();
        });
        // --- FIN DE CAMBIOS ---

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(IS_FIRST_RUN, true)) {
            showDevelopmentDialog();
            prefs.edit().putBoolean(IS_FIRST_RUN, false).apply();
        }

        miWebView = findViewById(R.id.webview);
        errorPanel = findViewById(R.id.error_panel);
        errorDetails = findViewById(R.id.error_details);

        setupWebView();
        miWebView.loadUrl("https://horario-1cv.vercel.app");
    }

    private void setupWebView() {
        WebSettings webSettings = miWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(miWebView, true);

        miWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("WebViewConsole", consoleMessage.message() + " -- From line " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }
        });

        miWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                miWebView.setVisibility(View.VISIBLE);
                errorPanel.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    handleError("Code: " + error.getErrorCode() + "\nDescription: " + error.getDescription() + "\nURL: " + request.getUrl());
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                handleError("Code: " + errorCode + "\nDescription: " + description + "\nURL: " + failingUrl);
            }
        });
    }

    private void handleError(String errorLog) {
        Log.e("WebView_Error", errorLog);
        errorDetails.setText(errorLog);
        miWebView.setVisibility(View.GONE);
        errorPanel.setVisibility(View.VISIBLE);
    }

    private void showDevelopmentDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog_Fullscreen)
                .setTitle("Modo de Desarrollo")
                .setMessage("Esta aplicación se encuentra en una fase activa de desarrollo. Algunas funciones pueden no comportarse como se espera.")
                .setPositiveButton("Entendido", (d, which) -> d.dismiss())
                .setIcon(android.R.drawable.ic_dialog_info)
                .create();

        if (dialog.getWindow() != null) {
            WindowCompat.setDecorFitsSystemWindows(dialog.getWindow(), false);
        }

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if (errorPanel.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
            return;
        }

        if (miWebView.canGoBack()) {
            miWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
