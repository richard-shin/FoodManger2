package org.androidtown.foodmanger2;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.androidtown.foodmanger2.common.TitleBitmapButton;
import org.androidtown.foodmanger2.db.MemoDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG="MainActivity";

    ListView listView;
    FoodAdapter mFoodListadapter;
    TitleBitmapButton insertBtn, closeBtn;
    TextView greenText, yellowText, redText;

    public static MemoDatabase mDatabase=null;

    int mGreenNum = 0;
    int mYellowNum = 0;
    int mRedNum = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 디바이스 내장 스토리지 상태 체크 후 경로 설정
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "내장 스토리지를 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
            return;
        } else {
            String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (!BasicInfo.ExternalChecked && externalPath != null) {
                BasicInfo.ExternalPath = externalPath;
                Log.d(TAG, "ExternalPath : " + BasicInfo.ExternalPath);

                BasicInfo.DATABASE_NAME = BasicInfo.ExternalPath + BasicInfo.DATABASE_NAME;
                BasicInfo.ExternalChecked = true;
            }
        }


        greenText = (TextView) findViewById(R.id.green_text);
        yellowText = (TextView) findViewById(R.id.yellow_text);
        redText = (TextView) findViewById(R.id.red_text);

        // 리스트뷰 설정
        listView = findViewById(R.id.foodList);
        mFoodListadapter = new FoodAdapter(getApplicationContext());
        listView.setAdapter(mFoodListadapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                 viewFood(i);

            }
        });

        // 추가 버튼 설정
        insertBtn = findViewById(R.id.newFoodBtn);
        insertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "new Food Button clicked");

                Intent intent = new Intent(getApplicationContext(), FoodInsertActivity.class);
                intent.putExtra(BasicInfo.KEY_FOOD_MODE, BasicInfo.MODE_INSERT);
                startActivityForResult(intent, BasicInfo.REQ_INSERT_ACTIVITY);
            }
        });


        // 닫기 버튼 설정
        closeBtn = findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


    }

    @Override
    protected void onStart() {
        openDatabase();
        loadFoodListData();
        setStats();

       super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void openDatabase() {
        if(mDatabase !=null) {
            mDatabase.close();
            mDatabase = null;
        }

        mDatabase = MemoDatabase.getInstance(this);
        boolean isOpen = mDatabase.open();
        if(isOpen) {
            Log.d(TAG, "Food database is open");
        }else {
            Log.d(TAG, "Food database is not open");
        }

    }

    public void viewFood(int position) {
        Toast.makeText(getApplicationContext(), "Selected Item: " + position, Toast.LENGTH_LONG).show();

        FoodItem item =(FoodItem)mFoodListadapter.getItem(position);

        Intent intent = new Intent(getApplicationContext(), FoodInsertActivity.class);
        intent.putExtra(BasicInfo.KEY_FOOD_MODE, BasicInfo.MODE_MODIFY);

        intent.putExtra(BasicInfo.KEY_FOOD_ID,    item.getId());
        intent.putExtra(BasicInfo.KEY_FOOD_DATE,  item.getDate());
        intent.putExtra(BasicInfo.KEY_FOOD_NAME,  item.getName());
        intent.putExtra(BasicInfo.KEY_FOOD_DURATION,   item.getDuration());
        intent.putExtra(BasicInfo.KEY_FOOD_RESID, item.getResId());

        startActivityForResult(intent, BasicInfo.REQ_VIEW_ACTIVITY);

    }


    public int loadFoodListData() {
        String SQL = "select  _id, INPUT_DATE, FOOD_NAME, ID_RES, FOOD_DAY from  FOOD  order by INPUT_DATE  desc";
        int recordCount = -1;
        if(mDatabase !=null) {
            Cursor outCursor = mDatabase.rawQuery(SQL);

            recordCount = outCursor.getCount();
            Log.d(TAG, "cursor count: " + recordCount + "in loadFoodListData().\n");

            mFoodListadapter.clear();
            Resources res = getResources();

            // 통계값 초기화
            mRedNum=0;
            mYellowNum = 0;
            mGreenNum = 0;

            for(int i=0; i < recordCount; i++) {
                outCursor.moveToNext();

                String foodId = outCursor.getString(0);
                String dateStr = outCursor.getString(1);
                if (dateStr.length() > 10) {
                    dateStr = dateStr.substring(0, 10);
                }
                String foodName = outCursor.getString(2);
                int resId = outCursor.getInt(3);
                int foodduration = outCursor.getInt(4);
                Log.d(TAG, "duration day: " + foodduration);

                Date currentTime = new Date ();
                String today = BasicInfo.dateDayFormat.format(currentTime);
                int days = calDate(today, dateStr);
                int calDateDays = foodduration - days;
                Log.d(TAG, "remain day: " + calDateDays);

                mFoodListadapter.addItem(new FoodItem(foodId, foodName, dateStr,
                        foodduration, resId, calDateDays));

                calStats(calDateDays);

            }
            outCursor.close();
            mFoodListadapter.notifyDataSetChanged();
        }

        return recordCount;

    }

    // 신선도 통계 계산
    public void calStats(int remainday) {

        if(remainday < 0) {   // 기간 초과
            mRedNum = mRedNum + 1;
        }else if(remainday >=0 && remainday < 2) { // 0~1일 사이
            mYellowNum = mYellowNum + 1;
        }else if(remainday >= 2) {  // 2일 이상
            mGreenNum = mGreenNum + 1;
        }
        Log.d(TAG, " stats: " + mGreenNum + "," + mYellowNum + "," + mRedNum);

    }

    // 신선도 통계 표시
    public void setStats() {
        greenText.setText(String.valueOf(mGreenNum));
        yellowText.setText(String.valueOf(mYellowNum));
        redText.setText(String.valueOf(mRedNum));
    }

    public int calDate(String date1, String date2) {

        long calDateDays = 0;

        try{
           Date firstDate =  BasicInfo.dateDayFormat.parse(date1);
           Date secondDate = BasicInfo.dateDayFormat.parse(date2);

           long calDate = firstDate.getTime() - secondDate.getTime();

           calDateDays = calDate / (24*60*60*1000);

//           Log.d(TAG, "두 날짜의 날짜 차이: " + calDateDays);

        }catch (ParseException e){
            e.printStackTrace();
        }

        return (int) calDateDays;
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case BasicInfo.REQ_INSERT_ACTIVITY:
                if(resultCode == RESULT_OK) {
                    loadFoodListData();
                }
                break;

            case BasicInfo.REQ_VIEW_ACTIVITY:
                loadFoodListData();

                break;
        }

    }
}
