package com.fieldbook.tracker.traits;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.fieldbook.tracker.R;
import com.fieldbook.tracker.activities.CollectActivity;
import com.fieldbook.tracker.database.dao.ObservationDao;
import com.fieldbook.tracker.database.models.ObservationModel;
import com.fieldbook.tracker.preferences.GeneralKeys;
import com.fieldbook.tracker.utilities.CategoryJsonUtil;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;

import org.brapi.v2.model.pheno.BrAPIScaleValidValuesCategories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public class MultiCatTraitLayout extends BaseTraitLayout {
    //todo this can eventually be merged with multicattraitlayout when we can support a switch in traits on how many categories to allow user to select

    //on load layout, check preferences and save to variable
    //this will choose whether to display the label or value in subsequent functions
    private boolean showLabel = true;

    private ArrayList<BrAPIScaleValidValuesCategories> categoryList;

    //track when we go to new data
    private boolean isFrozen = false;

    //private StaggeredGridView gridMultiCat;
    private RecyclerView gridMultiCat;

    public MultiCatTraitLayout(Context context) {
        super(context);
    }

    public MultiCatTraitLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiCatTraitLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setNaTraitsText() {
    }

    @Override
    public void refreshLock() {
        isFrozen = ((CollectActivity) getContext()).isDataLocked();
    }

    @Override
    public String type() {
        return "multicat";
    }

    @Override
    public void init() {

        gridMultiCat = findViewById(R.id.catGrid);

        categoryList = new ArrayList<>();
    }

    @Override
    public void loadLayout() {
        super.loadLayout();

        final String trait = getCurrentTrait().getTrait();

        String labelValPref = getPrefs().getString(GeneralKeys.LABELVAL_CUSTOMIZE,"value");
        showLabel = !labelValPref.equals("value");

        categoryList = new ArrayList<>();

        if (!getNewTraits().containsKey(trait)) {

            getCollectInputView().setText("");
            getCollectInputView().setTextColor(Color.BLACK);

        } else {

            loadScale();
        }

        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getContext());
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setAlignItems(AlignItems.STRETCH);
        gridMultiCat.setLayoutManager(layoutManager);

        if (!((CollectActivity) getContext()).isDataLocked()) {

           setAdapter();

        }

        gridMultiCat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                gridMultiCat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                gridMultiCat.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }
        });

        refreshLock();
    }

    private void setAdapter() {

        loadScale();

        //grabs all categories for this given trait
        BrAPIScaleValidValuesCategories[] cat = getCategories();

        //creates buttons for each category and sets state based on if its been selected
        gridMultiCat.setAdapter(new MultiCatTraitAdapter(getContext()) {

            @Override
            public void onBindViewHolder(MultiCatTraitViewHolder holder, int position) {
                holder.bindTo(cat[position]);

                holder.mButton.setOnClickListener(createClickListener(holder.mButton,position));

                if (showLabel) {
                    holder.mButton.setText(cat[position].getLabel());

                } else {
                    holder.mButton.setText(cat[position].getValue());
                }

                //has category checks the loaded categoryList to see if this button has been selected
                if (hasCategory(cat[position])) {

                    pressOnButton(holder.mButton);

                } else {

                    pressOffButton(holder.mButton);
                }
            }

            @Override
            public int getItemCount() {
                return cat.length;
            }
        });
    }

    private void loadScale() {

        //load();

        categoryList.clear();

        String value = getCollectInputView().getText();

        ArrayList<BrAPIScaleValidValuesCategories> scale = new ArrayList<>();

        try {

            scale = CategoryJsonUtil.Companion.decode(value);

        } catch (Exception e) {

            String[] tokens = value.split("\\:");
            for (String token : tokens) {
                BrAPIScaleValidValuesCategories c = new BrAPIScaleValidValuesCategories();
                c.setLabel(token);
                c.setValue(token);
                scale.add(c);
            }
        }

        scale = CategoryJsonUtil.Companion.filterExists(getCategories(), scale);

        categoryList.addAll(scale);
        refreshCategoryText();
        getCollectInputView().setTextColor(Color.parseColor(getDisplayColor()));

    }

    private void load() {

        CollectActivity act = (CollectActivity) getContext();

        List<ObservationModel> models = Arrays.asList(ObservationDao.Companion
                .getAllRepeatedValues(act.getStudyId(), act.getObservationUnit(), act.getTraitName()));

        for (ObservationModel m : models) {

            if (m.getValue() != null && !m.getValue().isEmpty()) {

                StringJoiner joiner = new StringJoiner(":");
                ArrayList<BrAPIScaleValidValuesCategories> scale = CategoryJsonUtil.Companion.decode(m.getValue());

                for (BrAPIScaleValidValuesCategories v : scale) {

                    joiner.add(v.getValue());
                }

                m.setValue(joiner.toString());
            }
        }

        act.getInputView().getRepeatView().initialize(models);

    }

    private OnClickListener createClickListener(final Button button, int position) {
        return v -> {

            if (!isFrozen) {
                BrAPIScaleValidValuesCategories cat = (BrAPIScaleValidValuesCategories) button.getTag();

                if (hasCategory(cat)) {
                    pressOffButton(button);
                    removeCategory(cat);
                } else {
                    pressOnButton(button);
                    addCategory((BrAPIScaleValidValuesCategories) button.getTag());
                }

                StringJoiner joiner = new StringJoiner(":");
                for (BrAPIScaleValidValuesCategories c : categoryList) {
                    if (showLabel) {
                        joiner.add(c.getLabel());
                    } else joiner.add(c.getValue());
                }

                getCollectInputView().setText(joiner.toString());

                String json = CategoryJsonUtil.Companion.encode(categoryList);

                updateObservation(getCurrentTrait().getTrait(),
                        getCurrentTrait().getFormat(),
                        json);

                if (showLabel) {
                    triggerTts(cat.getLabel());
                } else triggerTts(cat.getValue());
            }
        };
    }

    private BrAPIScaleValidValuesCategories[] getCategories() {

        //read the json object stored in additional info of the trait object (only in BrAPI imported traits)
        ArrayList<BrAPIScaleValidValuesCategories> cat = new ArrayList<>();

        String categoryString = getCurrentTrait().getCategories();
        try {

            ArrayList<BrAPIScaleValidValuesCategories> json = CategoryJsonUtil.Companion.decode(categoryString);

            if (!json.isEmpty()) {

                cat.addAll(json);
            }

        } catch (Exception e) {

            String[] rawStrings = categoryString.split("/");

            for (String label : rawStrings) {
                BrAPIScaleValidValuesCategories s = new BrAPIScaleValidValuesCategories();
                s.setValue(label);
                s.setLabel(label);
                cat.add(s);
            }
        }

        return cat.toArray(new BrAPIScaleValidValuesCategories[0]);
    }

    private boolean hasCategory(final BrAPIScaleValidValuesCategories category) {
        for (final BrAPIScaleValidValuesCategories cat : categoryList) {
            if (cat.getLabel().equals(category.getLabel())
                && cat.getValue().equals(category.getValue())) return true;
        }
        return false;
    }

    private void pressOnButton(Button button) {
        button.setTextColor(Color.parseColor(getDisplayColor()));
        button.getBackground().setColorFilter(button.getContext().getResources().getColor(R.color.button_pressed), PorterDuff.Mode.MULTIPLY);
    }

    private void pressOffButton(Button button) {
        button.setTextColor(Color.BLACK);
        button.getBackground().setColorFilter(button.getContext().getResources().getColor(R.color.button_normal), PorterDuff.Mode.MULTIPLY);
    }

    private void addCategory(final BrAPIScaleValidValuesCategories category) {

        categoryList.add(category);

        refreshCategoryText();
    }

    private void refreshCategoryText() {

        StringJoiner joiner = new StringJoiner(":");

        for (BrAPIScaleValidValuesCategories c : categoryList) {
            String value;
            if (showLabel) value = c.getLabel();
            else value = c.getValue();
            joiner.add(value);
        }

        getCollectInputView().setText(joiner.toString());
    }

    private void removeCategory(final BrAPIScaleValidValuesCategories category) {

        categoryList.remove(category);

        refreshCategoryText();

    }

    @Override
    public void deleteTraitListener() {
        ((CollectActivity) getContext()).removeTrait();
        super.deleteTraitListener();
        refreshLayout(false);
    }

    /**
     * Notifies grid adapter to refresh which buttons are selected.
     * This is used when the repeat value view navigates across repeated values.
     */
    @Override
    public void refreshLayout(Boolean onNew) {
        super.refreshLayout(onNew);

        setAdapter();

    }

    @Override
    public String decodeValue(String value) {
        StringJoiner joiner = new StringJoiner(":");
        ArrayList<BrAPIScaleValidValuesCategories> scale = CategoryJsonUtil.Companion.decode(value);
        for (BrAPIScaleValidValuesCategories s : scale) {
            joiner.add(s.getValue());
        }
        return joiner.toString();
    }
}
