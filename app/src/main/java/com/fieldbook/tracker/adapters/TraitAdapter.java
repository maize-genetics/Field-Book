package com.fieldbook.tracker.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.fieldbook.tracker.R;
import com.fieldbook.tracker.activities.CollectActivity;
import com.fieldbook.tracker.activities.TraitEditorActivity;
import com.fieldbook.tracker.objects.TraitObject;
import com.fieldbook.tracker.utilities.DialogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Loads data on trait editor screen
 */
public class TraitAdapter extends BaseAdapter {

    public Boolean infoDialogShown = false;
    public ArrayList<TraitObject> list;
    private final Context context;
    private final OnItemClickListener listener;
    private final HashMap<String, Boolean> visibility;

    public TraitAdapter(Context context,
                        int resource,
                        ArrayList<TraitObject> list,
                        OnItemClickListener listener,
                        HashMap<String, Boolean> visibility,
                        Boolean dialogShown) {
        this.context = context;
        this.list = list;
        this.listener = listener;
        this.visibility = visibility;
        // dialog shown indicates whether dialog has been shown on activity or not
        this.infoDialogShown = dialogShown;
    }

    public int getCount() {
        return list.size();
    }

    public TraitObject getItem(int position) {
        return list.get(position);
    }

    public long getItemId(int position) {

        if (position < 0 || position >= visibility.size()) {
            return -1;
        }

        return position;
    }

    public View getView(final int position, View convertView, final ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.list_item_trait, parent, false);

            holder = new ViewHolder();
            holder.name = convertView.findViewById(R.id.list_item_trait_trait_name);
            holder.format = convertView.findViewById(R.id.traitType);
            holder.visible = convertView.findViewById(R.id.visible);
            holder.dragSort = convertView.findViewById(R.id.dragSort);
            holder.menuPopup = convertView.findViewById(R.id.popupMenu);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.id = getItem(position).getId();
        holder.realPosition = String.valueOf(getItem(position).getRealPosition());
        holder.name.setText(getItem(position).getTrait());

        switch (getItem(position).getFormat()) {
            case "numeric":
                holder.format.setBackgroundResource(R.drawable.ic_trait_numeric);
                break;
            case "categorical":
                holder.format.setBackgroundResource(R.drawable.ic_trait_categorical);
                break;
            case "date":
                holder.format.setBackgroundResource(R.drawable.ic_trait_date);
                break;
            case "percent":
                holder.format.setBackgroundResource(R.drawable.ic_trait_percent);
                break;
            case "boolean":
                holder.format.setBackgroundResource(R.drawable.ic_trait_boolean);
                break;
            case "text":
                holder.format.setBackgroundResource(R.drawable.ic_trait_text);
                break;
            case "photo":
                holder.format.setBackgroundResource(R.drawable.ic_trait_camera);
                break;
            case "audio":
                holder.format.setBackgroundResource(R.drawable.ic_trait_audio);
                break;
            case "counter":
                holder.format.setBackgroundResource(R.drawable.ic_trait_counter);
                break;
            case "disease rating":
                holder.format.setBackgroundResource(R.drawable.ic_trait_disease_rating);
                break;
            case "rust rating":
                holder.format.setBackgroundResource(R.drawable.ic_trait_disease_rating);
                break;
            case "multicat":
                holder.format.setBackgroundResource(R.drawable.ic_trait_multicat);
                break;
            case "location":
                holder.format.setBackgroundResource(R.drawable.ic_trait_location);
                break;
            case "barcode":
                holder.format.setBackgroundResource(R.drawable.ic_trait_barcode);
                break;
            case "zebra label print":
                holder.format.setBackgroundResource(R.drawable.ic_trait_labelprint);
                break;
            case "gnss":
                holder.format.setBackgroundResource(R.drawable.ic_trait_gnss);
                break;
            case "usb camera":
                holder.format.setBackgroundResource(R.drawable.ic_trait_usb);
                break;
            default:
                holder.format.setBackgroundResource(R.drawable.ic_reorder);
                break;
        }

        // Check or uncheck the list items
        if (visibility != null) {
            if (visibility.get(holder.name.getText().toString()) != null) {
                boolean vis = Boolean.TRUE.equals(visibility.get(holder.name.getText().toString()));
                holder.visible.setChecked(vis);
            }
        }

        holder.visible.setOnCheckedChangeListener((arg0, isChecked) -> {
            String traitName = holder.name.getText().toString();
            if (holder.visible.isChecked()) {
                ((TraitAdapterController) context).getDatabase().updateTraitVisibility(traitName, true);
                if (visibility != null) {
                    visibility.put(traitName, true);
                }
            } else {
                ((TraitAdapterController) context).getDatabase().updateTraitVisibility(traitName, false);
                if (visibility != null) {
                    visibility.put(traitName, false);
                }
            }
        });

        // We make this separate form the on check changed listener so that we can
        // separate the difference between user interaction and programmatic checking.
        holder.visible.setOnClickListener(v -> {

            // Only show dialog if it hasn't been show yet
            if (!infoDialogShown) {

                // Check if the button is checked or not.
                CheckBox visibleCheckBox = (CheckBox) v;
                if (visibleCheckBox.isChecked()) {

                    // Show our BrAPI info box if this is a non-BrAPI trait
                    String traitName = holder.name.getText().toString();
                    infoDialogShown = ((TraitAdapterController) context).displayBrapiInfo(context, traitName, false);

                }

            }
        });

        holder.menuPopup.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(TraitEditorActivity.thisActivity, v);
            //Inflating the Popup using xml file
            popup.getMenuInflater().inflate(R.menu.menu_trait_list_item, popup.getMenu());

            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(createTraitListListener(parent, holder, v, position));

            popup.show();//showing popup menu

        });

        return convertView;
    }

    private PopupMenu.OnMenuItemClickListener createTraitListListener(
            final ViewGroup parent, final ViewHolder holder,
            final View v, final int position) {
        return item -> {
            if (item.getTitle().equals(TraitEditorActivity.thisActivity.getString(R.string.traits_options_copy))) {
                copyTrait(position);
            } else if (item.getTitle().equals(TraitEditorActivity.thisActivity.getString(R.string.traits_options_delete))) {
                deleteTrait(holder);
            } else if (item.getTitle().equals(TraitEditorActivity.thisActivity.getString(R.string.traits_options_edit))) {
                listener.onItemClick((AdapterView) parent, v, position, v.getId());
            }

            return false;
        };
    }

    private void copyTrait(final int position) {
        int pos = ((TraitAdapterController) context).getDatabase().getMaxPositionFromTraits() + 1;

        String traitName = getItem(position).getTrait();
        final String newTraitName = copyTraitName(traitName);

        TraitObject trait = getItem(position);
        trait.setTrait(newTraitName);
        trait.setVisible(true);
        trait.setRealPosition(pos);

        ((TraitAdapterController) context).getDatabase().insertTraits(trait);
        ((TraitAdapterController) context).queryAndLoadTraits();

        CollectActivity.reloadData = true;
    }

    private String copyTraitName(String traitName) {
        if (traitName.contains("-Copy")) {
            traitName = traitName.substring(0, traitName.indexOf("-Copy"));
        }

        String newTraitName = "";

        String[] allTraits = ((TraitAdapterController) context).getDatabase().getAllTraitNames();

        for (int i = 0; i < allTraits.length; i++) {
            newTraitName = traitName + "-Copy-(" + i + ")";
            if (!Arrays.asList(allTraits).contains(newTraitName)) {
                return newTraitName;
            }
        }
        return "";    // not come here
    }

    private void deleteTrait(final ViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppAlertDialog);

        builder.setTitle(context.getString(R.string.traits_options_delete_title));
        builder.setMessage(context.getString(R.string.traits_warning_delete));

        builder.setPositiveButton(context.getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ((TraitAdapterController) context).getDatabase().deleteTrait(holder.id);

                if (context instanceof TraitAdapterController) {
                    ((TraitAdapterController) context).queryAndLoadTraits();
                }

                CollectActivity.reloadData = true;
            }
        });

        builder.setNegativeButton(context.getString(R.string.dialog_no), (dialog, which) -> dialog.dismiss());

        AlertDialog alert = builder.create();
        alert.show();
        DialogUtils.styleDialogs(alert);
    }

    private static class ViewHolder {
        TextView name;
        ImageView format;
        CheckBox visible;
        ImageView dragSort;
        ImageView menuPopup;
        String id;
        String realPosition;
    }
}