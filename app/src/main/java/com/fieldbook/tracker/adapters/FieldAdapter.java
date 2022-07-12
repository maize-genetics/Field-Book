package com.fieldbook.tracker.adapters;

import androidx.appcompat.app.AlertDialog;
import androidx.arch.core.util.Function;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fieldbook.tracker.activities.ConfigActivity;
import com.fieldbook.tracker.activities.CollectActivity;
import com.fieldbook.tracker.R;
import com.fieldbook.tracker.brapi.BrapiInfoDialog;
import com.fieldbook.tracker.activities.FieldEditorActivity;
import com.fieldbook.tracker.brapi.model.BrapiStudyDetails;
import com.fieldbook.tracker.brapi.model.Observation;
import com.fieldbook.tracker.brapi.service.BrAPIService;
import com.fieldbook.tracker.brapi.service.BrAPIServiceFactory;
import com.fieldbook.tracker.brapi.service.BrapiPaginationManager;
import com.fieldbook.tracker.database.DataHelper;
import com.fieldbook.tracker.database.dao.ObservationUnitAttributeDao;
import com.fieldbook.tracker.database.dao.StudyDao;
import com.fieldbook.tracker.objects.FieldObject;
import com.fieldbook.tracker.objects.TraitObject;
import com.fieldbook.tracker.preferences.GeneralKeys;
import com.fieldbook.tracker.utilities.DialogUtils;
import com.fieldbook.tracker.utilities.PrefsConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loads data on field manager screen
 */

public class FieldAdapter extends BaseAdapter {

    private static final int PRIMARY = 0;
    private static final int SECONDARY = 1;
    private static final int TERTIARY = 2;
    private final String PLACEHOLDER_OPTION;

    private LayoutInflater mLayoutInflater;
    private ArrayList<FieldObject> list;
    private final Context context;
    private SharedPreferences ep;
    private String selectedPrimary;
    private String selectedSecondary;
    private String selectedTertiary;

    AlertDialog.Builder builder;
    AlertDialog progressDialog;

    public FieldAdapter(Context context, ArrayList<FieldObject> list) {
        this.context = context;
        mLayoutInflater = LayoutInflater.from(context);
        this.list = list;

        PLACEHOLDER_OPTION = context.getString(R.string.sort_placeholder);
    }

    public int getCount() {
        return list.size();
    }

    public FieldObject getItem(int position) {
        return list.get(position);
    }

    public long getItemId(int position) {

        if (position < 0) {
            return -1;
        }

        return position;
    }

    private void setEditorItem(SharedPreferences ep, FieldObject item) {
        SharedPreferences.Editor ed = ep.edit();
        boolean has_contents = item != null;
        if (has_contents) {
            ed.putString("FieldFile", item.getExp_name());
            ed.putInt(PrefsConstants.SELECTED_FIELD_ID, item.getExp_id());
            ed.putString("ImportUniqueName", item.getUnique_id());
            ed.putString("ImportFirstName", item.getPrimary_id());
            ed.putString("ImportSecondName", item.getSecondary_id());
        } else {
            ed.putString("FieldFile", null);
            ed.putInt(PrefsConstants.SELECTED_FIELD_ID, -1);
            ed.putString("ImportUniqueName", null);
            ed.putString("ImportFirstName", null);
            ed.putString("ImportSecondName", null);
        }
        ed.putBoolean("ImportFieldFinished", has_contents);
        ed.putString("lastplot", null);
        ed.apply();
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        ep = context.getSharedPreferences("Settings", 0);

        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.listitem_field, null);

            holder.fieldName = convertView.findViewById(R.id.field_name);
            holder.count = convertView.findViewById(R.id.field_count);
            holder.importDate = convertView.findViewById(R.id.field_import_date);
            holder.editDate = convertView.findViewById(R.id.field_edit_date);
            holder.exportDate = convertView.findViewById(R.id.field_export_date);
            holder.active = convertView.findViewById(R.id.fieldRadio);
            holder.menuPopup = convertView.findViewById(R.id.popupMenu);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        convertView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                fieldClick(getItem(position));
            }
        });

        String importDate = getItem(position).getDate_import();
        String editDate = getItem(position).getDate_edit();
        String exportDate = getItem(position).getDate_export();

        if (importDate != null) {
            importDate = importDate.split(" ")[0];
        }

        if (editDate != null) {
            editDate = editDate.split(" ")[0];
        }

        if (exportDate != null) {
            exportDate = exportDate.split(" ")[0];
        }

        holder.fieldName.setText(getItem(position).getExp_name());
        holder.count.setText(getItem(position).getCount());
        holder.importDate.setText(importDate);
        holder.editDate.setText(editDate);
        holder.exportDate.setText(exportDate);

        holder.active.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fieldClick(getItem(position));
            }
        });

        if (ep.getString("FieldFile", "").contentEquals(holder.fieldName.getText())) {
            holder.active.setChecked(true);
        } else {
            holder.active.setChecked(false);
        }

        holder.menuPopup.setOnClickListener(makeMenuPopListener(position));

        return convertView;
    }

    private View.OnClickListener makeMenuPopListener(final int position) {
        return new View.OnClickListener() {
            // Do it when clicking ":"
            @Override
            public void onClick(final View view) {
                PopupMenu popup = new PopupMenu(context, view);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.menu_field_listitem, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(makeSelectMenuListener(position));
                popup.show();//showing popup menu
            }
        };
    }

    private PopupMenu.OnMenuItemClickListener makeSelectMenuListener(final int position) {
        return new PopupMenu.OnMenuItemClickListener() {
            // Do it when selecting Delete or Statistics
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.delete) {
                    AlertDialog alert = createDeleteItemAlertDialog(position);
                    alert.show();
                    DialogUtils.styleDialogs(alert);
                } else if (item.getItemId() == R.id.sort) {
                    AlertDialog alert = showSortDialog(position);
                    DialogUtils.styleDialogs(alert);
                }
                else if (item.getItemId() == R.id.syncObs) {
                    //TODO call the observation download code
                    AlertDialog alert = createSyncItemAlertDialog(position);
                    alert.show();
                    DialogUtils.styleDialogs(alert);
                }

                return false;
            }
        };
    }

    private DialogInterface.OnClickListener makeConfirmDeleteListener(final int position) {
        return new DialogInterface.OnClickListener() {
            // Do it when clicking Yes or No
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                ConfigActivity.dt.deleteField(getItem(position).getExp_id());

                if (getItem(position).getExp_id() == ep.getInt(PrefsConstants.SELECTED_FIELD_ID, -1)) {
                    setEditorItem(ep, null);
                }

                FieldEditorActivity.loadData();
                CollectActivity.reloadData = true;
            }
        };
    }

    private DialogInterface.OnClickListener makeConfirmSyncListener(final int position,ProgressBar progressBar) {
        return new DialogInterface.OnClickListener() {
            // Do it when clicking Yes or No
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressBar.setVisibility(View.VISIBLE);

                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        syncObservations(getItem(position));
                    }
                });


                dialog.dismiss();

//                System.out.println("Clicked sync");
//                System.out.println("ExpId: "+getItem(position).getExp_id());
//                System.out.println("Name: "+getItem(position).getExp_name());
//                System.out.println("Alias: "+getItem(position).getExp_alias());
//                System.out.println("source: "+getItem(position).getExp_source());
//                System.out.println("primary ID: "+getItem(position).getPrimary_id());
//                System.out.println("secondary ID: "+getItem(position).getSecondary_id());
//                System.out.println("unique ID: "+getItem(position).getUnique_id());
//
//
//                //TODO uncomment to allow for it to start syncing observations.
//                syncObservations(getItem(position));


                FieldEditorActivity.loadData();
                CollectActivity.reloadData = true;
            }
        };
    }

//    private void syncObservations(String studyDbId) {
    private void syncObservations(FieldObject study) {
        String brapiStudyDbId = study.getExp_alias();
        int fieldBookStudyDbId = study.getExp_id();

        System.out.println("Clicked Sync 2");
        Toast toast = Toast.makeText(context, "Sync Clicked", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();



        BrAPIService brAPIService = BrAPIServiceFactory.getBrAPIService(this.context);

        List<String> observationIds = new ArrayList<String>();

//        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
//        progressDialog = getDialogProgressBar().create();

        //Trying to get the traits as well:
        brAPIService.getTraits(brapiStudyDbId, new Function<BrapiStudyDetails, Void>() {
            @Override
            public Void apply(BrapiStudyDetails input) {
                for(TraitObject obj : input.getTraits()) {
                    System.out.println("Trait:"+obj.getTrait());
                    System.out.println("ObsIds: "+obj.getExternalDbId());
                    observationIds.add(obj.getExternalDbId());
                }
//                progressDialog.show();
                getObservations(brAPIService, brapiStudyDbId ,fieldBookStudyDbId,observationIds);
//                progressDialog.dismiss();
//                findViewById(R.id.loadingPanel).setVisibility(View.GONE);

                return null;
            }
        }, new Function<Integer, Void>() {
            @Override
            public Void apply(Integer input) {
                return null;
            }
        });

        System.out.println("obsIds Size:"+observationIds.size());

    }

    private void getObservations(BrAPIService brAPIService,String brapiStudyDbId,int fieldbookStudyDbId, List<String> observationIds) {
        int pageSize = Integer.parseInt(context.getSharedPreferences("Settings", 0)
                .getString(GeneralKeys.BRAPI_PAGE_SIZE, "1000"));

        BrapiPaginationManager paginationManager =  new BrapiPaginationManager(0,pageSize);

        DataHelper dataHelper = new DataHelper(context);

        brAPIService.getObservations(brapiStudyDbId, observationIds, paginationManager, new Function<List<Observation>, Void>() {
            @Override
            public Void apply(List<Observation> input) {

                System.out.println("EnteredIn Observation code: "+ input.size());
                for(Observation obs : input) {
                    System.out.println("****************************");
                    System.out.println("Saving: varName: "+obs.getVariableName());
                    System.out.println("Saving: value: "+obs.getValue());
                    System.out.println("Saving: studyId: "+obs.getStudyId());
                    System.out.println("Saving: unitDBId: "+obs.getUnitDbId());
                    System.out.println("Saving: varDbId: "+obs.getVariableDbId());

                    dataHelper.setTraitObservations(fieldbookStudyDbId, obs);
                }

                // If we haven't thrown an error by now, we are good.
//                DataHelper.db.setTransactionSuccessful();
                return null;
            }
        }, new Function<Integer, Void>() {
            @Override
            public Void apply(Integer input) {
                System.out.println("Stopped:");
                return null;
            }
        });
    }

    private AlertDialog showSortDialog(final int position) {

        FieldObject field = getItem(position);
        List<String> ouAttributes = new ArrayList<>();
        ouAttributes.add(PLACEHOLDER_OPTION);
        ouAttributes.addAll(Arrays.asList(ObservationUnitAttributeDao.Companion.getAllNames(field.getExp_id())));
        ArrayAdapter<String> sortOptions = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, ouAttributes);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppAlertDialog);

        builder.setTitle(context.getString(R.string.fields_update_sort_study));
        builder.setView(R.layout.dialog_sort);

        builder.setPositiveButton(context.getString(R.string.dialog_save), null);
        builder.setNegativeButton(context.getString(R.string.dialog_cancel), (dialog, which) -> Log.d("FieldActivity", "Cancel Clicked"));

        AlertDialog alert = builder.create();

        // workaround to prevent positiveButton built in onClick listener from auto dismissing the dialog
        alert.setOnShowListener(dialogInterface -> {
            ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {

                Integer errorMessage = null;

                if(!selectedPrimary.equals(PLACEHOLDER_OPTION) || !selectedSecondary.equals(PLACEHOLDER_OPTION) || !selectedTertiary.equals(PLACEHOLDER_OPTION)) {
                    if (selectedPrimary.equals(PLACEHOLDER_OPTION)) {
                        errorMessage = R.string.sort_dialog_error_missing_primary;
                    } else if (selectedSecondary.equals(PLACEHOLDER_OPTION) && !selectedTertiary.equals(PLACEHOLDER_OPTION)) {
                        errorMessage = R.string.sort_dialog_error_missing_secondary;
                    } else if (selectedPrimary.equals(selectedSecondary)
                            || selectedPrimary.equals(selectedTertiary)
                            || (selectedSecondary.equals(selectedTertiary) && !selectedTertiary.equals(PLACEHOLDER_OPTION))) {
                        errorMessage = R.string.sort_dialog_error_duplicates;
                    }
                }

                if(errorMessage != null) {
                    TextView errorMessageView = alert.findViewById(R.id.sortError);
                    errorMessageView.setText(errorMessage);
                    errorMessageView.setVisibility(View.VISIBLE);
                } else {
                    String sort = null;

                    if(!selectedPrimary.equals(PLACEHOLDER_OPTION)) {
                        sort = selectedPrimary;
                    }

                    if(!selectedSecondary.equals(PLACEHOLDER_OPTION)) {
                        sort += "," + selectedSecondary;
                    }

                    if(!selectedTertiary.equals(PLACEHOLDER_OPTION)) {
                        sort += "," + selectedTertiary;
                    }

                    FieldObject fieldObject = getItem(position);
                    fieldObject.setExp_sort(sort);
                    try {
                        StudyDao.Companion.updateStudySort(sort, fieldObject.getExp_id());
                        if (ep.getInt(PrefsConstants.SELECTED_FIELD_ID, 0) == field.getExp_id()) {
                            ConfigActivity.dt.switchField(fieldObject.getExp_id());
                            CollectActivity.reloadData = true;
                        }
                        Toast toast = Toast.makeText(context, R.string.sort_dialog_saved, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
                        toast.show();
                    } catch (Exception e) {
                        Log.e("FieldAdapter", "Error updating sorting", e);

                        new AlertDialog.Builder(context).setTitle(R.string.dialog_save_error_title)
                                .setPositiveButton(R.string.okButtonText, (dInterface, i) -> Log.d("FieldAdapter", "Sort save error dialog dismissed"))
                                .setMessage(R.string.sort_dialog_error_saving)
                                .create()
                                .show();
                    }
                    FieldEditorActivity.loadData();
                    alert.dismiss();
                }
            });

        });

        alert.show();

        String[] selectedVals = new String[]{PLACEHOLDER_OPTION, PLACEHOLDER_OPTION, PLACEHOLDER_OPTION}; //preventing NPEs

        if(field.getExp_sort() != null) {
            String[] split = field.getExp_sort().split(",");
            System.arraycopy(split, 0, selectedVals, 0, split.length);
        }

        createSortOptionSelectedListener(R.id.primarySpin, alert, sortOptions, PRIMARY, selectedVals);
        createSortOptionSelectedListener(R.id.secondarySpin, alert, sortOptions, SECONDARY, selectedVals);
        createSortOptionSelectedListener(R.id.tertiarySpin, alert, sortOptions, TERTIARY, selectedVals);

        return alert;
    }

    public AlertDialog.Builder getDialogProgressBar() {

        if (builder == null) {
            builder = new AlertDialog.Builder(context);

            builder.setTitle("Loading...");

            final ProgressBar progressBar = new ProgressBar(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            progressBar.setLayoutParams(lp);
            builder.setView(progressBar);
        }
        return builder;
    }

    private void createSortOptionSelectedListener(int spinnerId, AlertDialog alert, ArrayAdapter<String> sortOptions, int spinnerType, String[] selectedVals) {
        Spinner spinner = alert.findViewById(spinnerId);
        spinner.setAdapter(sortOptions);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long id) {
                switch (spinnerType) {
                    case PRIMARY:
                        selectedPrimary = sortOptions.getItem(index);
                        break;
                    case SECONDARY:
                        selectedSecondary = sortOptions.getItem(index);
                        break;
                    case TERTIARY:
                        selectedTertiary = sortOptions.getItem(index);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinner.setSelection(sortOptions.getPosition(selectedVals[spinnerType]));
    }

    private AlertDialog createDeleteItemAlertDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppAlertDialog);

        builder.setTitle(context.getString(R.string.fields_delete_study));
        builder.setMessage(context.getString(R.string.fields_delete_study_confirmation));
        builder.setPositiveButton(context.getString(R.string.dialog_yes), makeConfirmDeleteListener(position));
        builder.setNegativeButton(context.getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });

        AlertDialog alert = builder.create();
        return alert;
    }

    //TODO fix text to use the R.string Variables.
    private AlertDialog createSyncItemAlertDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppAlertDialog);

        final ProgressBar progressBar = new ProgressBar(context);
        progressBar.setVisibility(View.GONE);
        builder.setView(progressBar);


        builder.setTitle("Sync Observations");
        builder.setMessage("Are you sure you want to update observations for this Field?");
        builder.setPositiveButton(context.getString(R.string.dialog_yes), makeConfirmSyncListener(position,progressBar));
        builder.setNegativeButton(context.getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });



        AlertDialog alert = builder.create();
        return alert;
    }

    private void fieldClick(FieldObject selectedField) {

        setEditorItem(ep, selectedField);

        SharedPreferences.Editor ed = ep.edit();
        ed.putInt(PrefsConstants.SELECTED_FIELD_ID, selectedField.getExp_id());
        ed.apply();

        ConfigActivity.dt.switchField(selectedField.getExp_id());
        CollectActivity.reloadData = true;
        notifyDataSetChanged();

        // Check if this is a BrAPI field and show BrAPI info dialog if so
        if (selectedField.getExp_source() != null &&
                selectedField.getExp_source() != "" &&
                selectedField.getExp_source() != "local") {

            BrapiInfoDialog brapiInfo = new BrapiInfoDialog(context,
                    context.getResources().getString(R.string.brapi_info_message));
            brapiInfo.show();
        }
    }

    private class ViewHolder {
        ImageView menuPopup;
        TextView fieldName;
        TextView count;
        TextView importDate;
        TextView editDate;
        TextView exportDate;
        RadioButton active;
    }
}