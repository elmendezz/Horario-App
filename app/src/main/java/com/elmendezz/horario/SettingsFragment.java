package com.elmendezz.horario;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.File;

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

        Preference checkForUpdates = findPreference("check_for_updates");
        if (checkForUpdates != null) {
            checkForUpdates.setOnPreferenceClickListener(preference -> {
                String url = "https://github.com/elmendezz/Horario-App/releases/latest/download/app-release.apk";
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setDestinationInExternalFilesDir(getContext(), Environment.DIRECTORY_DOWNLOADS, "app-release.apk");
                request.setTitle("Descargando actualización");
                request.setDescription("Descargando la última versión de Horario App");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                DownloadManager downloadManager = (DownloadManager) requireActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                downloadID = downloadManager.enqueue(request);
                return true;
            });
        }

        Preference viewChangelog = findPreference("view_changelog");
        if (viewChangelog != null) {
            viewChangelog.setOnPreferenceClickListener(preference -> {
                String url = "https://github.com/elmendezz/Horario-App/releases";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().unregisterReceiver(onDownloadComplete);
    }

    private void installUpdate() {
        File apkFile = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "app-release.apk");
        Uri apkUri = FileProvider.getUriForFile(requireContext(), requireContext().getApplicationContext().getPackageName() + ".provider", apkFile);

        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(apkUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        requireContext().startActivity(intent);
    }
}