package org.opensourcebridge.navigator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class Main extends Activity {
        private final int SPLASH_DISPLAY_LENGTH = 1000;
        /** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle bundle) {
                super.onCreate(bundle);
                setContentView(R.layout.loading);
                /* New Handler to start the Menu-Activity
                 * and close this Splash-Screen after some seconds.*/
                new Handler().postDelayed(new Runnable(){

                        @Override
                        public void run() {
                                /* Create an Intent that will start the Menu-Activity. */
                                Intent mainIntent = new Intent(Main.this, Schedule.class);
                                Main.this.startActivity(mainIntent);
                                Main.this.finish();
                        }
                }, SPLASH_DISPLAY_LENGTH);
        }
}
