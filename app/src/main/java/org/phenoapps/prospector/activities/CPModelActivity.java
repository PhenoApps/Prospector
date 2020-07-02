//package consumerphysics.com.myscioapplication.activities;
package org.phenoapps.prospector.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.consumerphysics.android.sdk.callback.cloud.ScioCloudCPModelsCallback;
import com.consumerphysics.android.sdk.model.ScioCPModel;
import com.consumerphysics.android.sdk.model.ScioModel;

import org.phenoapps.prospector.R;
import org.phenoapps.prospector.config.Constants;

import java.util.ArrayList;
import java.util.List;

//import consumerphysics.com.myscioapplication.R;


public class CPModelActivity extends BaseScioActivity {

    private ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model);

        setTitle(getString(R.string.select_model_title));

        List<ScioCPModel> modelArrayList = new ArrayList<>();
        final ModelAdapter adp = new ModelAdapter(this, modelArrayList);

        lv = (ListView) findViewById(R.id.listView);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        lv.setMultiChoiceModeListener(new ModeCallback());

        lv.setAdapter(adp);

        if (getScioCloud() == null || !getScioCloud().hasAccessToken()) {
            Toast.makeText(getApplicationContext(), R.string.user_not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ScioModel model = adp.getItem(position);
                storeSelectedModel(model);
                Toast.makeText(getApplicationContext(), model.getName() + getString(R.string.was_selected_with_space), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        getScioCloud().getCPModels(new ScioCloudCPModelsCallback() {
            @Override
            public void onSuccess(List<ScioCPModel> models) {
                adp.addAll(models);
            }

            @Override
            public void onError(int code, String error) {
                Toast.makeText(getApplicationContext(), getString(R.string.error_retrieving_cp_models) + error + "(" + code + ")", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void storeSelectedModel(final ScioModel model) {
        SharedPreferences pref = getSharedPreferences(Constants.PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();

        edit.putString(Constants.MODEL_ID, model.getId());
        edit.putString(Constants.MODEL_NAME, model.getName());

        edit.commit();
    }

    private void storeSelectedModels(final List<ScioCPModel> models) {
        SharedPreferences pref = getSharedPreferences(Constants.PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();

        String modelIds = "";
        String modelNames = "";
        for (ScioCPModel scioModel : models) {
            modelNames += scioModel.getName();
            modelNames += ",";
            modelIds += scioModel.getId();
            modelIds += ",";
        }

        modelIds = modelIds.substring(0, modelIds.length() - 1);
        modelNames = modelNames.substring(0, modelNames.length() - 1);

        edit.putString(Constants.MODEL_ID, modelIds);
        edit.putString(Constants.MODEL_NAME, modelNames);

        edit.commit();
    }

    private class ModeCallback implements ListView.MultiChoiceModeListener {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.model_selector_menu, menu);
            mode.setTitle(R.string.select_models);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.done:
                    if (lv.getCheckedItemCount() == 0) {
                        return true;
                    }

                    List<ScioCPModel> scioModels = new ArrayList<>();
                    for (int i = 0; i < lv.getChildCount(); i++) {
                        if (lv.getCheckedItemPositions().get(i) == true) {
                            scioModels.add((ScioCPModel) lv.getAdapter().getItem(i));
                        }
                    }

                    storeSelectedModels(scioModels);
                    Toast.makeText(getApplicationContext(), scioModels.size() + getString(R.string.selected_with_space), Toast.LENGTH_SHORT).show();

                    finish();
                    break;
            }

            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
            for (int i = 0; i < lv.getChildCount(); i++) {
                lv.getChildAt(i).setBackgroundColor(getResources().getColor(R.color.transparent));
            }
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            View view = lv.getChildAt(position);
            if (checked) {
                view.setBackgroundColor(getResources().getColor(R.color.grey));
            }
            else {
                view.setBackgroundColor(getResources().getColor(R.color.transparent));
            }

            final int checkedCount = lv.getCheckedItemCount();
            switch (checkedCount) {
                case 0:
                    mode.setSubtitle(null);
                    break;
                case 1:
                    mode.setSubtitle(R.string.one_model_selected);
                    break;
                default:
                    mode.setSubtitle(checkedCount + getString(R.string.models_selected));
                    break;
            }
        }
    }

    public class ModelAdapter extends ArrayAdapter<ScioCPModel> {
        public ModelAdapter(Context context, List<ScioCPModel> devices) {
            super(context, 0, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            ScioCPModel model = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.details_item, parent, false);
            }

            // Build supported versions string
            String supportedScioVersion = "";
            for (String supportVersion : model.getScioVersions()) {
                supportedScioVersion += supportVersion + ", ";
            }

            if (!supportedScioVersion.equals("")) {
                supportedScioVersion = supportedScioVersion.substring(0, supportedScioVersion.length() - 2);
            }

            // Lookup view for data population
            TextView modelNameView = (TextView) convertView.findViewById(R.id.itemTitle);
            TextView scioVersionsView = (TextView) convertView.findViewById(R.id.itemSubtitle);
            TextView requiredScansView = (TextView) convertView.findViewById(R.id.itemDetails);

            // Populate the data into the template view using the data object
            modelNameView.setText(model.getName());
            scioVersionsView.setText(supportedScioVersion);
            requiredScansView.setText(Integer.toString(model.getRequiredScans()));

            // Return the completed view to render on screen
            return convertView;
        }
    }
}
