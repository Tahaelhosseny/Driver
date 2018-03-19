package uber.example.uber.driver;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Splash extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                startActivity(new Intent(getApplicationContext(),Login.class));
                finish();
            }
        };
        Handler handler = new Handler();
        handler.postDelayed(runnable,3000);
    }
}
