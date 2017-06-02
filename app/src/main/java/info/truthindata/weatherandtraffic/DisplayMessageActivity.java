package info.truthindata.weatherandtraffic;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DisplayMessageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String darkSkyApi = intent.getStringExtra(MainActivity.DARK_SKY_API);
        String homeAddress = intent.getStringExtra(MainActivity.HOME_ADDRESS);
        String workAddress = intent.getStringExtra(MainActivity.WORK_ADDRESS);

        // Capture the layout's TextView and set the string as its text
        TextView textDarkSkyApi = (TextView) findViewById(R.id.textDarkSkyApi);
        TextView textWorkAddress = (TextView) findViewById(R.id.textWorkAddress);
        TextView textHomeAddress = (TextView) findViewById(R.id.textHomeAddress);

        textDarkSkyApi.setText(darkSkyApi);
        textWorkAddress.setText(workAddress);
        textHomeAddress.setText(homeAddress);
    }
}
