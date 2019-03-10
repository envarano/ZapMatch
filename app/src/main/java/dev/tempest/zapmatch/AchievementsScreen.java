package dev.tempest.zapmatch;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Objects;

import dev.tempest.zapmatch.Player.PlayerInfo;
import dev.tempest.zapmatch.R;

public class AchievementsScreen extends AppCompatActivity {

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.achievements_menu);

        FileInputStream is;

        try {
            is = openFileInput("player_info");
            StringBuffer fileContent = new StringBuffer();
            InputStreamReader isr = new InputStreamReader(is) ;
            BufferedReader buffReader = new BufferedReader(isr) ;
            String readString = buffReader.readLine() ;

            while (readString != null)
            {
                fileContent.append(readString);
                readString = buffReader.readLine();
            }

            String unfilteredPlayerData = String.valueOf(fileContent);
            String[] playerData = unfilteredPlayerData.split(",");

            PlayerInfo.level = Integer.parseInt(playerData[0]);
            PlayerInfo.levelHighest = Integer.parseInt(playerData[1]);


            for(int i = 0; i < PlayerInfo.achievements.size(); i++){
                StringBuilder objectConverter = new StringBuilder();
                objectConverter.append(Objects.requireNonNull(PlayerInfo.achievements.keySet().toArray())[i]);
                String stringBoolean = String.valueOf(objectConverter);
                PlayerInfo.achievements.put(stringBoolean, Boolean.parseBoolean(playerData[i+2]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String json = null;

        try {
            InputStream inputStream = getAssets().open("achievements.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            int bytesNum = inputStream.read(buffer);
            System.out.println(bytesNum);
            inputStream.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray achievements = null;

        if (jsonObject != null) {
            try {
                achievements = jsonObject.getJSONArray("achievements");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        LinearLayout layout = findViewById(R.id.achievements_layout);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int totalPoints = 0;
        Typeface face = Typeface.createFromAsset(getAssets(),
                "font/abel.ttf");

        assert achievements != null;
        for (int j = 0; j < achievements.length(); j++){

            try {
                JSONObject cit = achievements.getJSONObject(j);
                String title = cit.getString("title");
                String name = cit.getString("name");
                String description = cit.getString("description");
                String points = cit.getString("points");
                LinearLayout achievementLayout = new LinearLayout(this);
                params.bottomMargin = 50;
                achievementLayout.setPadding(0,100,0,0);
                achievementLayout.setLayoutParams(containerParams);
                achievementLayout.setOrientation(LinearLayout.VERTICAL);
                achievementLayout.setBackground(getDrawable(R.drawable.text_list_layout));
                TextView achievementTitle = new TextView(this);
                achievementTitle.setText(title);
                achievementTitle.setTextSize(35);
                achievementTitle.setGravity(Gravity.CENTER);
                achievementTitle.setLayoutParams(params);
                achievementTitle.setTextColor(Color.parseColor("#bbbbbb"));
                achievementTitle.setTypeface(face);
                TextView achievementDescription = new TextView(this);
                achievementDescription.setText(description);
                achievementDescription.setTextSize(18);
                achievementDescription.setPadding(20, 0,0,0);
                achievementDescription.setTypeface(face);
                achievementDescription.setLayoutParams(params);
                achievementDescription.setTextColor(Color.parseColor("#bbbbbb"));
                achievementLayout.getBackground().setColorFilter(Color.rgb(50, 50, 70), PorterDuff.Mode.LIGHTEN);

                if (Objects.equals(PlayerInfo.achievements.get(name), true)) {
                    achievementLayout.getBackground().setColorFilter(null);
                    totalPoints += Integer.parseInt(points);
                    achievementTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.achievement_complete, 0);
                    float tickPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
                    achievementTitle.setPadding((int) tickPadding + 30,0,30,0);

                    if(totalPoints >= 200){
                        PlayerInfo.achievements.put("highAchiever", true);
                    }
                }
                achievementLayout.addView(achievementTitle);
                achievementLayout.addView(achievementDescription);
                layout.addView(achievementLayout);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        TextView achievementView = findViewById(R.id.achievements_text);
        achievementView.setText(String.format(Locale.getDefault(), "%s %d", getString(R.string.achievement_points), totalPoints));
        achievementView.setTypeface(face);
    }
}
