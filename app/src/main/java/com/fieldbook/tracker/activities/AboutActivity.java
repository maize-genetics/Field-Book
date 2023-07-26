package com.fieldbook.tracker.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.danielstone.materialaboutlibrary.ConvenienceBuilder;
import com.danielstone.materialaboutlibrary.MaterialAboutActivity;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.fieldbook.tracker.BuildConfig;
import com.fieldbook.tracker.R;
import com.fieldbook.tracker.preferences.GeneralKeys;
import com.fieldbook.tracker.utilities.VersionChecker;
import com.michaelflisar.changelog.ChangelogBuilder;
import com.michaelflisar.changelog.classes.ImportanceChangelogSorter;
import com.michaelflisar.changelog.internal.ChangelogDialogFragment;
import com.mikepenz.aboutlibraries.LibsBuilder;

public class AboutActivity extends MaterialAboutActivity {
    //todo move to fragments so aboutactivity can extend base activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemedActivity.Companion.applyTheme(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    @NonNull
    public MaterialAboutList getMaterialAboutList(@NonNull Context c) {

        MaterialAboutCard.Builder appCardBuilder = new MaterialAboutCard.Builder();
        appCardBuilder.addItem(new MaterialAboutTitleItem.Builder()
                .text(getString(R.string.field_book))
                .icon(R.mipmap.ic_launcher)
                .build());

        appCardBuilder.addItem(ConvenienceBuilder.createVersionActionItem(c,
                getResources().getDrawable(R.drawable.ic_about_info),
                getString(R.string.about_version_title),
                false));

        appCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                getResources().getDrawable(R.drawable.book_open_variant),
                getString(R.string.about_manual_title),
                false,
                Uri.parse("https://docs.fieldbook.phenoapps.org/en/latest/field-book.html")));

        appCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getString(R.string.updates_title))
                .icon(R.drawable.ic_about_get_update)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        checkForUpdate();
                    }
                })
                .build());

        MaterialAboutCard.Builder authorCardBuilder = new MaterialAboutCard.Builder();
        authorCardBuilder.title(getString(R.string.about_project_lead_title));

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getString(R.string.about_developer_trife))
                .subText(getString(R.string.about_developer_trife_location))
                .icon(R.drawable.ic_pref_profile_person)
                .build());

        authorCardBuilder.addItem(ConvenienceBuilder.createEmailItem(c,
                getResources().getDrawable(R.drawable.ic_about_email),
                getString(R.string.about_email_title),
                true,
                getString(R.string.about_developer_trife_email),
                "Field Book Question"));

        authorCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                getResources().getDrawable(R.drawable.ic_about_website),
                "PhenoApps.org",
                false,
                Uri.parse("http://phenoapps.org/")));

        MaterialAboutCard.Builder contributorsCardBuilder = new MaterialAboutCard.Builder();
        contributorsCardBuilder.title(getString(R.string.about_support_title));

        contributorsCardBuilder.addItem(ConvenienceBuilder.createRateActionItem(c,
                getResources().getDrawable(R.drawable.ic_about_rate),
                getString(R.string.about_rate),
                null
        ));

        contributorsCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                getResources().getDrawable(R.drawable.ic_about_contributors),
                getString(R.string.about_contributors_title),
                false,
                Uri.parse("https://github.com/PhenoApps/Field-Book#-contributors")));

        contributorsCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                getResources().getDrawable(R.drawable.ic_about_funding),
                getString(R.string.about_contributors_funding_title),
                false,
                Uri.parse("https://github.com/PhenoApps/Field-Book#-funding")));


        MaterialAboutCard.Builder technicalCardBuilder = new MaterialAboutCard.Builder();
        technicalCardBuilder.title(getString(R.string.about_technical_title));

        technicalCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.about_github_title)
                .icon(R.drawable.ic_about_github)
                .setOnClickAction(ConvenienceBuilder.createWebsiteOnClickAction(c, Uri.parse("https://github.com/PhenoApps/Field-Book")))
                .build());

        String theme = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(GeneralKeys.THEME, "0");

        int styleId = R.style.AboutLibrariesCustom;
        switch (theme) {
            case "2":
                styleId = R.style.AboutLibrariesCustom_Blue;
                break;
            case "1":
                styleId = R.style.AboutLibrariesCustom_HighContrast;
                break;
        }
        final int libStyleId = styleId;

        technicalCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.libraries_title)
                .icon(R.drawable.ic_about_libraries)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        new LibsBuilder()
                                .withActivityTheme(libStyleId)
                                .withAutoDetect(true)
                                .withActivityTitle(getString(R.string.libraries_title))
                                .withLicenseShown(true)
                                .withVersionShown(true)
                                .start(getApplicationContext());
                    }
                })
                .build());

        MaterialAboutCard.Builder otherAppsCardBuilder = new MaterialAboutCard.Builder();
        otherAppsCardBuilder.title(getString(R.string.about_title_other_apps));

        otherAppsCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text("Coordinate")
                .icon(R.drawable.other_ic_coordinate)
                .setOnClickAction(openAppOrStore("org.wheatgenetics.coordinate", c))
                .build());

        otherAppsCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text("Intercross")
                .icon(R.drawable.other_ic_intercross)
                .setOnClickAction(openAppOrStore("org.phenoapps.intercross", c))
                .build());

        return new MaterialAboutList(appCardBuilder.build(), authorCardBuilder.build(), contributorsCardBuilder.build(), otherAppsCardBuilder.build(), technicalCardBuilder.build());
    }

    private String getCurrentAppVersion() {
        return BuildConfig.VERSION_NAME;
    }

    private void checkForUpdate() {
        String currentVersion = getCurrentAppVersion();
        VersionChecker versionChecker = new VersionChecker(AboutActivity.this, currentVersion, "PhenoApps", "Field-Book");
        versionChecker.execute();
    }

    @Override
    protected CharSequence getActivityTitle() {
        return getString(R.string.mal_title_about);
    }


    private MaterialAboutItemOnClickAction openAppOrStore(String packageName, Context c) {
        PackageManager packageManager = getBaseContext().getPackageManager();
        try {
            packageManager.getPackageInfo(packageName, 0);

            return () -> {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                startActivity(launchIntent);
            };
        } catch (PackageManager.NameNotFoundException e) {
            return ConvenienceBuilder.createWebsiteOnClickAction(c, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
        }
    }
}
