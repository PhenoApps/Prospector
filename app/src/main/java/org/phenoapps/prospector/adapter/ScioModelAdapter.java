//package consumerphysics.com.myscioapplication.adapter;
package org.phenoapps.prospector.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.consumerphysics.android.sdk.model.ScioModel;
import com.consumerphysics.android.sdk.model.attribute.ScioAttribute;
import com.consumerphysics.android.sdk.model.attribute.ScioDatetimeAttribute;
import com.consumerphysics.android.sdk.model.attribute.ScioNumericAttribute;
import com.consumerphysics.android.sdk.model.attribute.ScioStringAttribute;

import java.util.List;

//import consumerphysics.com.myscioapplication.R;
import org.phenoapps.prospector.R;

/**
 * Created by nadavg on 16/02/2016.
 */
public class ScioModelAdapter extends ArrayAdapter<ScioModel> {
    public ScioModelAdapter(final Context context, final List<ScioModel> models) {
        super(context, 0, models);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.results_item, parent, false);
        }

        final TextView remainingScans = (TextView) convertView.findViewById(R.id.remaining);
        final TextView attributeName = (TextView) convertView.findViewById(R.id.attribute_name);
        final TextView attributeValue = (TextView) convertView.findViewById(R.id.attribute_value);
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
                remainingScans.setText("Scan " + String.valueOf(numberOfScansLeft) + " more times");
            } else {
                remainingScans.setText("Scan session complete");
            }
        }

        if (model.getAttributes() != null && !model.getAttributes().isEmpty()) {
            for (ScioAttribute attribute : model.getAttributes()) {
                String value;
                String unit = null;

                /**
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
                        value = "No Value";
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
            attributeValue.setText("N/A");
        }

        if(model.isUnknownMaterial()){
            attributeValue.setText("Unknown Material");
        }

        return convertView;
    }
}
