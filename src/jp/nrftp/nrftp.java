package jp.nrftp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.ads.*;

public class nrftp extends Activity {
    /** Called when the activity is first created. */

    private static final int FTP_CONNECTION = 0;
    private static final int FTP_SERVER_SETTING = 1;
    private static final int FTP_SERVER_EDIT = 2;

    private String serverName ="";
	
	//DB関連
    ftpdb dbHelper = new ftpdb(this);  
    SQLiteDatabase ftpresult;
	
	//view
	private ProgressDialog pd;
	private Spinner sp;
	private Button startButton;
	private Button historyButton;
	private Button settingButton;
	private Button deleteButton;
	private Button editButton;

	@Override    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        

        // adViewの作成
        AdView adView = new AdView(this, AdSize.BANNER, "a14da494efa431e");
        // 広告を追加したいViewを取得
        LinearLayout layout = (LinearLayout)findViewById(R.id.ad_main);
        // adViewを追加
        layout.addView(adView);
        // 広告をリクエストする
        adView.loadAd(new AdRequest());
        AdRequest request = new AdRequest();
        //request.setTesting(true); //for test ad
        adView.loadAd(request);  

        
        //ログイン用ProgressDialog
        pd = new ProgressDialog(this);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setMessage(getString(R.string.TOP_FTP_LOGIN));
        pd.setCancelable(false);

        //Spinner
        sp = (Spinner)findViewById(R.id.Spinner01);
        sp.setFocusable(true);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                // 選択されたアイテムを取得します
            	serverName = (String) sp.getSelectedItem();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        
        startButton = (Button)findViewById(R.id.Button01);
        startButton.setOnClickListener(new OnClickListener() {  
			@Override
			public void onClick(View v) {
				pd.show();
				new Thread(new Runnable(){
		    	   @Override
			    	public void run(){
		    		   try{
				        	Thread.sleep(700);
				        }catch(Exception e){}
				        pd.dismiss();
						startIntent();
					}
			    }).start();
			}
        });
        
        historyButton = (Button)findViewById(R.id.Button02);
        historyButton.setOnClickListener(new OnClickListener() {  
			@Override
			public void onClick(View v) {
				showHistory();
			}
        });
        
        settingButton = (Button)findViewById(R.id.Button03);
        settingButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v){
        		serverAddIntent();
        	}
        });
        
        deleteButton = (Button)findViewById(R.id.Button04);
        deleteButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v){
        		AlertDialog.Builder bldr = new AlertDialog.Builder(nrftp.this);
				bldr.setTitle(getString(R.string.TOP_SERVER_DELETE_CONFIRM_INFO));
				bldr.setMessage(String.format(getString(R.string.TOP_SERVER_DELETE_CONFIRM_MESSAGE), serverName));
	            bldr.setPositiveButton(getString(R.string.TOP_SERVER_DELETE_CONFIRM_OK),new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteServerData();
					}
				});
	            bldr.setNegativeButton(getString(R.string.TOP_SERVER_DELETE_CONFIRM_CANCEL),new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});
	            bldr.create();
	            bldr.show();
        	}
        });
        
        editButton = (Button)findViewById(R.id.Button05);
        editButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v){
        		AlertDialog.Builder bldr = new AlertDialog.Builder(nrftp.this);
				bldr.setTitle(getString(R.string.TOP_SERVER_EDIT_CONFIRM_INFO));
				bldr.setMessage(String.format(getString(R.string.TOP_SERVER_EDIT_CONFIRM_MESSAGE), serverName));
	            bldr.setPositiveButton(getString(R.string.TOP_SERVER_EDIT_CONFIRM_OK),new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						serverEditIntent();
					}
				});
	            bldr.setNegativeButton(getString(R.string.TOP_SERVER_EDIT_CONFIRM_CANCEL),new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});
	            bldr.create();
	            bldr.show();
        	}
        });
                
        
    }
	
	@Override
	public void onResume(){
		super.onResume();

		
        //DB
        ftpresult = dbHelper.getWritableDatabase();
        spinnerDataUpdate();
	}
	
   	public void startIntent(){
   		Cursor cursor = ftpresult.rawQuery("SELECT Address,UserName,Password,Port,RemoteDir,LocalDir from serverList where name='"+serverName+"';",null);
   		cursor.moveToNext();
		Log.d(getString(R.string.app_name),"start intent ftpmain");
		Intent intent = new Intent(this, ftpmain2.class);
		intent.putExtra("nrftp_serverAddress",cursor.getString(0));		
		intent.putExtra("nrftp_userName",cursor.getString(1));
		intent.putExtra("nrftp_password", cursor.getString(2));
		intent.putExtra("nrftp_port", cursor.getString(3));
		intent.putExtra("nrftp_remoteDir", cursor.getString(4));
		intent.putExtra("nrftp_localDir", cursor.getString(5));
		cursor.close();
		startActivityForResult(intent,FTP_CONNECTION);
    }
   	
   	public void deleteServerData(){
   		try{
   			ftpresult.execSQL("delete from serverList where name='"+serverName+"';");//,null);
   			Log.d(getString(R.string.app_name),"Server info '"+serverName+"' deleted.");
   		}catch(Exception e){
   			Log.d(getString(R.string.app_name),"Server info delete failed.");
   		}
        spinnerDataUpdate();
   	}
    
	public void serverAddIntent(){
		Log.d(getString(R.string.app_name),"start intent server add");
		Intent intent = new Intent(this, serverAdd.class);
		startActivityForResult(intent,FTP_SERVER_SETTING);
	}
	
	public void serverEditIntent(){
		Log.d(getString(R.string.app_name),"start intent server edit");
		Intent intent = new Intent(this, serverAdd.class);
		Cursor cursor = ftpresult.rawQuery("SELECT Address,UserName,Password,Port,RemoteDir,LocalDir from serverList where name='"+serverName+"';",null);
		cursor.moveToNext();
		intent.putExtra("nrftp_name", serverName);
		intent.putExtra("nrftp_serverAddress",cursor.getString(0));		
		intent.putExtra("nrftp_userName",cursor.getString(1));
		intent.putExtra("nrftp_password", cursor.getString(2));
		intent.putExtra("nrftp_port", cursor.getString(3));
		intent.putExtra("nrftp_remoteDir", cursor.getString(4));
		intent.putExtra("nrftp_localDir", cursor.getString(5));
		cursor.close();
		startActivityForResult(intent,FTP_SERVER_EDIT);
	}
	
	public void showHistory(){
		Intent intent = new Intent(this, history.class);
		startActivity(intent);	
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == FTP_CONNECTION){
			if(resultCode != RESULT_OK){
				Toast.makeText(this,getString(R.string.TOP_FTP_CONN_NG),Toast.LENGTH_LONG);
				AlertDialog.Builder bldr = new AlertDialog.Builder(this);
				bldr.setTitle(R.string.TOP_INFO);
				bldr.setMessage(getString(R.string.TOP_FTP_CONN_NG_MESSAGE));
	            bldr.setPositiveButton(getString(R.string.TOP_ACCEPT),new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Log.d(getString(R.string.app_name),"FTP login ng..");
					}
				});
	            bldr.create();
	            bldr.show();
			}
		}
		
		if(requestCode == FTP_SERVER_SETTING){
			if(resultCode == RESULT_OK){
				addServerInfoToDb(data);
			}
		}
		
		if(requestCode == FTP_SERVER_EDIT){
			if(resultCode == RESULT_OK){
				editServerInfo(data);
			}
		}
		
		
	}
    
	public void spinnerDataUpdate(){
		String serverList[];
		Cursor cursor = ftpresult.rawQuery("SELECT name from serverList;",null);//No, Transfer,Date, StartTime, ElapsedTime,Misc, FileName, FileSize FROM result order by No desc;",null);
		if(cursor.getCount() == 0){
			cursor.close();
			startButton.setEnabled(false);
			return;
		}else{
			startButton.setEnabled(true);
		}
		
		//SQLからデータ読み出す
		serverList = new String[cursor.getCount()];
		int i =0;
		while(cursor.moveToNext()){
			serverList[i++] = cursor.getString(0);
		}
		cursor.close();
		
		//Spinnerに追加する
		ArrayAdapter<String> serverListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, serverList);
		serverListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		serverListAdapter.notifyDataSetChanged();
		sp.setAdapter(serverListAdapter);
		
	}
	
    public void addServerInfoToDb(Intent data){
    	String name = data.getStringExtra("serverADD_NAME");
    	if(isServerSettingDuplicate(name)){
    		AlertDialog.Builder bldr = new AlertDialog.Builder(nrftp.this);
			bldr.setTitle(R.string.TOP_SERVER_ADD_DUPLICATE_INFO);
			bldr.setMessage(getString(R.string.TOP_SERVER_ADD_DUPLICATE_MESSAGE));
            bldr.setPositiveButton(getString(R.string.TOP_SERVER_ADD_DUPLICATE_ACCEPT),new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.d(getString(R.string.app_name),"Reject add server setting , Duplicate name.");
				}
			});
            bldr.create();
            bldr.show();
    		
    		return;
    	}
    	
    	String address = data.getStringExtra("serverADD_ADDRESS");
    	String user = data.getStringExtra("serverADD_USER");
    	String password = data.getStringExtra("serverADD_PASS");
    	int port = Integer.valueOf(data.getStringExtra("serverADD_PORT"));
    	String remoteDir = data.getStringExtra("serverADD_REMOTE_DIR");
    	String localDir = data.getStringExtra("serverADD_LOCAL_DIR");
    	
    	String sql = "insert into serverList (Name,Address,UserName,Password,Port,RemoteDir,LocalDir)    values ('";
    	sql += name +"','";
    	sql += address+"','";
    	sql += user+"','";
    	sql += password+"',";
    	sql += port +",'";
    	sql += remoteDir +"','";
    	sql += localDir + "');";
    	
    	try{
    		ftpresult.execSQL(sql);
    	}catch(Exception e){
    		Log.d(getString(R.string.app_name),"SQL Server add failed.");
    	}
    	spinnerDataUpdate();
    }
    public boolean isServerSettingDuplicate(String name){
    	String sql = "select name from serverList where name='"+name+"';";
    	Log.d(getString(R.string.app_name),"Server setting duplicate check.");
    	try{
    		Cursor c = ftpresult.rawQuery(sql,null);
    		c.moveToNext();
    		if(c.getString(0).compareTo(name)==0){
    			return true;
    		}
    		c.close();
    	}catch(Exception e){
    		
    	}
    	return false;
    }
    
    public void editServerInfo(Intent data){
    	String sql = "update serverList SET ";
    	sql += "Address='" + data.getStringExtra("serverADD_ADDRESS");
    	sql += "',UserName='" +data.getStringExtra("serverADD_USER");
    	sql += "',Password='" +data.getStringExtra("serverADD_PASS");
    	sql += "',Port=" +Integer.valueOf(data.getStringExtra("serverADD_PORT"));
    	sql += ",RemoteDir='" +data.getStringExtra("serverADD_REMOTE_DIR");
    	sql += "',LocalDir='" + data.getStringExtra("serverADD_LOCAL_DIR");
    	sql += "' where name='"+data.getStringExtra("serverADD_NAME")+"';";
    	
    	try{
    		ftpresult.execSQL(sql);
    		Log.d(getString(R.string.app_name),"Edit server info query successed.");
    	}catch(Exception e){
    		Log.d(getString(R.string.app_name),"Edit server info query failed.");
    	}
    	
    }
    
}