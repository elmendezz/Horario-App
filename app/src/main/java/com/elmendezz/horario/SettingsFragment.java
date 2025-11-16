package com.elmendezz.horario;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsFragment extends PreferenceFragmentCompat {

    private long downloadID;

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadID == id) {
                Toast.makeText(getContext(), "Descarga completada", Toast.LENGTH_SHORT).show();
                installUpdate();
            }
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        requireActivity().registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        // Actualizaciones
        Preference checkForUpdates = findPreference("check_for_updates");
        if (checkForUpdates != null) {
            checkForUpdates.setOnPreferenceClickListener(preference -> {
                beginDownload();
                return true;
            });
        }

        Preference viewChangelog = findPreference("view_changelog");
        if (viewChangelog != null) {
            viewChangelog.setOnPreferenceClickListener(preference -> {
                fetchChangelog();
                return true;
            });
        }

        // Datos y caché
        Preference clearCache = findPreference("clear_cache");
        if (clearCache != null) {
            clearCache.setOnPreferenceClickListener(preference -> {
                if (getContext() != null) {
                    new WebView(getContext()).clearCache(true);
                    Toast.makeText(getContext(), "Caché limpiada", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        // Acerca de
        Preference appVersion = findPreference("app_version");
        if (appVersion != null) {
            try {
                PackageInfo pInfo = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0);
                appVersion.setSummary(pInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                appVersion.setSummary("Desconocida");
            }
        }
    }

    private void beginDownload() {
        String url = "https://github.com/elmendezz/Horario-App/releases/latest/download/app-release.apk";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDestinationInExternalFilesDir(getContext(), Environment.DIRECTORY_DOWNLOADS, "app-release.apk");
        request.setTitle("Descargando actualización");
        request.setDescription("Descargando la última versión de Horario App");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager downloadManager = (DownloadManager) requireActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        downloadID = downloadManager.enqueue(request);
    }

    private void fetchChangelog() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        AlertDialog loadingDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cargando...")
                .setMessage("Obteniendo el registro de cambios.")
                .setCancelable(false)
                .show();

        executor.execute(() -> {
            String result = "";
            try {
                URL url = new URL("https://api.github.com/repos/elmendezz/Horario-App/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                conn.disconnect();

                JSONObject json = new JSONObject(content.toString());
                result = json.getString("body");

            } catch (Exception e) {
                e.printStackTrace();
                result = "No se pudo cargar el registro de cambios. Verifica tu conexión a internet.";
            }

            String finalResult = result;
            handler.post(() -> {
                loadingDialog.dismiss();
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Registro de Cambios")
                        .setMessage(finalResult)
                        .setPositiveButton("Cerrar", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        });
    }

    private void installUpdate() {
        if (getContext() == null) return;
        File apkFile = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "app-release.apk");
        Uri apkUri = FileProvider.getUriForFile(requireContext(), requireContext().getApplicationContext().getPackageName() + ".provider", apkFile);

        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(apkUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        requireContext().startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requireActivity() != null) {
            requireActivity().unregisterReceiver(onDownloadComplete);
        }
    }
}
