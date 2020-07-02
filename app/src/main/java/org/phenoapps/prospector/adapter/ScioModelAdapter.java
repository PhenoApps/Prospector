//package consumerphysics.com.myscioapplication.adapter;
package org.phenoapps.prospector.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.consumerphysics.android.sdk.model.ScioModel;
import com.consumerphysics.android.sdk.model.attribute.ScioAttribute;
import com.consumerphysics.android.sdk.model.attribute.ScioDatetimeAttribute;
import com.consumerphysics.android.sdk.model.attribute.ScioNumericAttribute;
import com.consumerphysics.android.sdk.model.attribute.ScioStringAttribute;

import org.phenoapps.prospector.R;

import java.util.List;

//import consumerphysics.com.myscioapplication.R;

/**
 * Created by nadavg on 16/02/2016.
 */
public class ScioModelAdapter extends ArrayAdapter<ScioModel> {
    public ScioModelAdapter(final Context context, final List<ScioModel> models) {
        super(context, 0, models);
    }

    @Override @NonNull
    public View getView(final int position, View convertView, @NonNull final ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.results_item, parent, false);
        }

        final TextView remainingScans = convertView.findViewById(R.id.remaining);
        final TextView attributeName = convertView.findViewById(R.id.attribute_name);
        final TextView attributeValue = convertView.findViewById(R.id.attribute_value);
        attributeValue.setText("");


        final ScioModel model = getItem(position);

        attributeName.setText(model.getName());

        // Remaining scans

        if(model.getRequiredScans() == 1){
            remainingScans.setText("");
        }
        else {
            int numberOfScansLeft = model.getRequiredScans() - model.getStoredScans();
            if (numberOfScansLeft > 0) {
                remainingScans.setText(String.format("%s%d%s", getContext().getString(R.string.scan_without_space), numberOfScansLeft, getContext().getString(R.string.more_times)));
            } else {
                remainingScans.setText(R.string.scan_session_complete);
            }
        }

        if (model.getAttributes() != null && !model.getAttributes().isEmpty()) {
            for (ScioAttribute attribute : model.getAttributes()) {
                String value;
                String unit = null;

                /*
                 * Classification model will return a STRING value.
                 * Estimation will return the NUMERIC value.
                 */
                switch (attribute.getAttributeType()) {
                    case STRING:
                        value = ((ScioStringAttribute) (attribute)).getValue();
                        break;
                    case NUMERIC:
                        Double numericValue = ((ScioNumericAttribute) (attribute)).getValue();
                        value = (numericValue == null)?null:String.valueOf(((ScioNumericAttribute) (attribute)).getValue());
                        unit = attribute.getUnits();
                        break;
                    case DATE_TIME:
                        value = ((ScioDatetimeAttribute) (attribute)).getValue().toString();
                        break;
                    default:
                        continue;
                }

                if (attribute.getLabel() != null) {
                    value = attribute.getLabel() + " " + value;
                }

                if (model.getType().equals(ScioModel.Type.ESTIMATION)) {
                    if(value == null){
                        value = getContext().getString(R.string.no_value);
                    }
                    else{
                        Float estimationValue= Float.parseFloat(value);
                        if (unit == null) {
                            value = String.format("%.2f",estimationValue);
                        }
                        else {
                            value = String.format("%.2f",estimationValue) + unit;
                        }
                    }
                }
                else {
                    value = value + " (" + String.format("%.2f", attribute.getConfidence()) + ")";
                }

                attributeValue.setText(attributeValue.getText().toString() + value);
            }
        }
        else {
            attributeValue.setText(R.string.not_applicable);
        }

        if(model.isUnknownMaterial()){
            attributeValue.setText(R.string.unknown_material);
        }

        return convertView;
    }
}
