package jp.nrftp;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


public class history extends Activity {
	
	static final int MENU_ITEM1 = 0;
	static final int MENU_ITEM2 = 1;
	static final int MENU_ITEM3 = 2;
	static final int MENU_ITEM4 = 3;
	
	
	TextView tv ;
	Button sendButton;
	ftpdb dbHelper = new ftpdb(this);  
    SQLiteDatabase ftpresult;
	
	@Override    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history);
        
        // adViewの作成
        AdView adView = new AdView(this, AdSize.BANNER, "a14da494efa431e");
        // 広告を追加したいViewを取得
        LinearLayout layout = (LinearLayout)findViewById(R.id.ad_history);
        // adViewを追加
        layout.addView(adView);
        // 広告をリクエストする
        adView.loadAd(new AdRequest());
        AdRequest request = new AdRequest();
        adView.loadAd(request);  
        
        
    	tv = (TextView)findViewById(R.id.history_text);
    	sendButton = (Button)findViewById(R.id.history_send);
    	sendButton.setOnClickListener(new OnClickListener() {  
			@Override
			public void onClick(View v) {
				ftpresult = null;
			
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_SENDTO);
				intent.setData(Uri.parse("mailto:"));
				intent.putExtra(Intent.EXTRA_SUBJECT,getString(R.string.HISTORY_MAIL_SUBJECT));
				intent.putExtra(Intent.EXTRA_TEXT,tv.getText());
				startActivity(intent);
			}
        });
    	/*
    	Intent intent = getIntent();
    	String tempResult = intent.getStringExtra("history");
    	tv.setText(tempResult);
    					*/

	}
	@Override
	public void onResume(){
		super.onResume();
		if(ftpresult==null){
			dbHelper = new  ftpdb(this);
			ftpresult = dbHelper.getWritableDatabase();
		}
    	getResult();
	}
	
	
	public void getResult(){
		Log.d(getString(R.string.app_name),"history: getResult all");

		StringBuilder sb = new StringBuilder();
		sb.append(getString(R.string.HISTORY_COLUMN));
		try{
			Cursor cursor = ftpresult.rawQuery("SELECT No, Transfer,Date, StartTime, ElapsedTime,Misc, FileName, FileSize FROM result order by No desc;",null);
			while(cursor.moveToNext()){
				sb.append(cursor.getInt(0));
				sb.append(","+cursor.getString(1));
				sb.append(","+cursor.getString(2));
				sb.append(","+cursor.getString(3));
				sb.append(","+cursor.getString(4));
				sb.append(","+cursor.getString(5));
				sb.append(","+cursor.getString(6));
				sb.append(","+cursor.getString(7));
				sb.append("\n");
			}
			cursor.close();
		}catch(Exception e){
			Log.d(getString(R.string.app_name),e.toString());
		}
		
		tv.setText("\n"+sb);
		
		
	}
	
	public void historyAllDelete(){
	
       	try{
    		//Delete all history
    		ftpresult.execSQL("delete from result where No > 0;");//,null);
    		
    		//Reset No (auto increment by sqlite3)
    		ftpresult.execSQL("update sqlite_sequence set seq=0 where name='result';");
    	}catch(Exception e){
    		Log.d(getString(R.string.app_name),e.toString());
    	}finally{
    		Log.d(getString(R.string.app_name),"History deleted!");
    		getResult();
    	}
	}

	public void historyDlOnly(){
		Log.d(getString(R.string.app_name),"history: getResult DL");
		StringBuilder sb = new StringBuilder();
		sb.append(getString(R.string.HISTORY_COLUMN));
		try{
			Cursor cursor = ftpresult.rawQuery("SELECT No, Transfer,Date, StartTime, ElapsedTime,Misc, FileName, FileSize FROM result  WHERE Transfer='DL'  order by No desc;",null);
			while(cursor.moveToNext()){
				sb.append(cursor.getInt(0));
				sb.append(","+cursor.getString(1));
				sb.append(","+cursor.getString(2));
				sb.append(","+cursor.getString(3));
				sb.append(","+cursor.getString(4));
				sb.append(","+cursor.getString(5));
				sb.append(","+cursor.getString(6));
				sb.append(","+cursor.getString(7));
				sb.append("\n");
			}
			cursor.close();
		}finally{
			//ftpresult.close();
		}
		
		tv.setText("\n"+sb);
		
		
	}
	
	public void historyUlOnly(){
		Log.d(getString(R.string.app_name),"history: getResult UL");
		StringBuilder sb = new StringBuilder();
		sb.append(getString(R.string.HISTORY_COLUMN));
		try{
			Cursor cursor = ftpresult.rawQuery("SELECT No, Transfer,Date, StartTime, ElapsedTime,Misc, FileName, FileSize FROM result WHERE Transfer='UL'  order by No desc;",null);
			while(cursor.moveToNext()){
				sb.append(cursor.getInt(0));
				sb.append(","+cursor.getString(1));
				sb.append(","+cursor.getString(2));
				sb.append(","+cursor.getString(3));
				sb.append(","+cursor.getString(4));
				sb.append(","+cursor.getString(5));
				sb.append(","+cursor.getString(6));
				sb.append(","+cursor.getString(7));
				sb.append("\n");
			}
			cursor.close();
		}finally{
			//ftpresult.close();
		}
		
		tv.setText("\n"+sb);
	}
		
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        menu.add(Menu.NONE, MENU_ITEM1, Menu.NONE, getString(R.string.HISTORY_DL_ONLY));
        menu.add(Menu.NONE, MENU_ITEM2, Menu.NONE, getString(R.string.HISTORY_UL_ONLY));
        menu.add(Menu.NONE, MENU_ITEM3, Menu.NONE, getString(R.string.HISTORY_SHOW_ALL));
        menu.add(Menu.NONE, MENU_ITEM4, Menu.NONE, getString(R.string.HISTORY_DELETE));
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM1:
        	historyDlOnly();
        	break;
        case MENU_ITEM2:
        	historyUlOnly();
        	break;
        case MENU_ITEM3:
        	getResult();
        	break;
        case MENU_ITEM4:
        	historyAllDelete();
        	break;
    	default:
    		break;
		}
        return true;
	}


	@Override
	public void onStop(){
		super.onStop();
		try{
			ftpresult.close();
		}catch(Exception e){
			
		}
	}

}