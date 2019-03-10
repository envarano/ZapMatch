package dev.tempest.zapmatch;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import dev.tempest.zapmatch.Player.PlayerInfo;
import dev.tempest.zapmatch.R;

public class LevelMenu extends AppCompatActivity {

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.level_menu);
        String json = null;

        try
        {
            InputStream is = getAssets().open("levels.json");
            int size = is.available();
            byte[] buffer  = new byte[size];
            int numBytes = is.read(buffer);
            System.out.println(numBytes);
            is.close();
            json = new String(buffer, "UTF-8");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray difficulty = null;

        if (jsonObject != null) {
            try {
                difficulty = jsonObject.getJSONArray("challenge");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        int levelsCompleted = 0;

        if(!PlayerInfo.isEndless) {
            levelsCompleted = PlayerInfo.levelHighest;
        }


        //final JSONArray levels = difficulty.getJSONArray(PlayerInfo.difficulty);
        final LinearLayout levelLayout = findViewById(R.id.level_layout);

        assert difficulty != null;
        for(int i = 0; i < difficulty.length(); i++){
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            final Button levelButton = new Button(this);
            levelButton.setText(String.format(Locale.getDefault(), "%s %d", getString(R.string.level_text), (i+1)));
            levelButton.setId(i);
            levelButton.setLayoutParams(params);
            levelButton.setBackground(getDrawable(R.drawable.text_list_layout));
            levelButton.setTextColor(Color.parseColor("#dddddd"));
            levelButton.setTextSize(30);
            levelLayout.addView(levelButton);

            if(i > levelsCompleted){
                levelButton.setClickable(false);
                levelButton.setBackground(getDrawable(R.drawable.unclickable_button_nocolour));
            }
            else{
                final JSONArray finalDifficulty = difficulty;
                levelButton.setOnClickListener(new View.OnClickListener() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View v) {
                        if(!PlayerInfo.isEndless) {
                                if (PlayerInfo.level < finalDifficulty.length()) {
                                    Button tempButton = (Button) levelLayout.getChildAt(PlayerInfo.level);
                                    tempButton.setBackgroundTintList(null);
                                    System.out.println(tempButton.getText());
                                }
                                PlayerInfo.level = v.getId();
                                v.setBackgroundTintList(getColorStateList(R.color.design_default_color_primary));
                        }
                    }
                });
            }

            if(!PlayerInfo.isEndless) {
                    if(i == PlayerInfo.level){
                        levelButton.setBackgroundTintList(getColorStateList(R.color.design_default_color_primary));
                    }
            }
        }


    }

}