package info.truthindata.weatherandtraffic;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    public static final String DARK_SKY_API = "info.truthindata.weatherandtraffic.DARK_SKI_API";
    public static final String HOME_ADDRESS = "info.truthindata.weatherandtraffic.HOME_ADDRESS";
    public static final String WORK_ADDRESS = "info.truthindata.weatherandtraffic.WORK_ADDRESS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /** Called when the user taps the Send button */
    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);

        EditText editWork = (EditText) findViewById(R.id.editWorkAddress);
        EditText editHome = (EditText) findViewById(R.id.editHomeAddress);
        EditText editDarkSky = (EditText) findViewById(R.id.editDarkSkyApi);

        String darkSkyApi = editDarkSky.getText().toString();
        String homeAddress = editHome.getText().toString();
        String workAddress = editWork.getText().toString();

        intent.putExtra(DARK_SKY_API, darkSkyApi);
        intent.putExtra(HOME_ADDRESS, homeAddress);
        intent.putExtra(WORK_ADDRESS, workAddress);

        startActivity(intent);
    }
}