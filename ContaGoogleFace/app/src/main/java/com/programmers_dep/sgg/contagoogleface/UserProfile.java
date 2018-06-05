package com.programmers_dep.sgg.contagoogleface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;

import java.io.InputStream;

public class UserProfile extends AppCompatActivity {
    private Button logout;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this);
        setContentView(R.layout.activity_main);


        Bundle inBundle = getIntent().getExtras();
        String name = inBundle.get("name").toString();
        String surname = inBundle.get("surname").toString();
        String imageUrl = inBundle.get("imageUrl").toString();

        TextView nameView = (TextView) findViewById(R.id.username);
        nameView.setText("" + name + " " + surname);
        Button logout = (Button) findViewById(R.id.sign_out_button);
        logout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                LoginManager.getInstance().logOut();
                /*Intent login = new Intent(UserProfile.this, MainActivity.class);
                startActivity(login);*/
                finish();
            }
        });
        new UserProfile.DownloadImage((ImageView)findViewById(R.id.profile_image)).execute(imageUrl);
    }

    public class DownloadImage extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImage(ImageView bmImage){
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls){
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try{
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            }catch (Exception e){
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result){
            bmImage.setImageBitmap(result);
        }

    }
}
