package jp.nrftp;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Date;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import it.sauronsoftware.ftp4j.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class ftpmain2 extends TabActivity implements TabHost.TabContentFactory  {
    /** Called when the activity is first created. */
	//FTP Client
	FTPClient cl = new FTPClient();
	private String serverAddress = "";
	private String userName = "";
	private String password = "";
	private String port = "";
	private String remoteDir = "";
	private String localDir = "";
	private boolean ongoing= false;
	private int currentSize= 0;
	boolean loginResult = false;
	private String ftpResultStr = "";
	private ProgressDialog progressDialog;
	
	//ListView(ファイル一覧関連)
	private FTPFile remoteList[];
	private File localFiles[];
	private ArrayAdapter<String> localAdapter;
	private ArrayAdapter<String> adapter;
	private ListView list1;
	private ListView list2;
	
	//View関連
	private final int FTP_RESULT = 1;
	private TextView path;
	private TextView localPath;
	private Activity me;
	private static final int MENU_ITEM1 = Menu.FIRST;
	private static final int MENU_ITEM2 = Menu.FIRST+1;

	//DB関連
    ftpdb dbHelper = new ftpdb(this);  
    SQLiteDatabase ftpresult;
    
    @Override    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;
        
        //タブ関連
        setContentView(R.layout.main2);
        TabHost tabHost = getTabHost();
        tabHost.setCurrentTabByTag("First");
        
        //リモートのファイルリストタブ
        TabSpec firstTab = tabHost.newTabSpec("First");
        firstTab.setIndicator("FTP server",getResources().getDrawable(R.drawable.laptop)); //タブ上の文字列と画像
        firstTab.setContent(R.id.first_content);	//表示するView
        tabHost.addTab(firstTab);

        //ローカルファイルリストタブ
        TabSpec secondTab = tabHost.newTabSpec("Second");
        secondTab.setIndicator("Local files",getResources().getDrawable(R.drawable.phone));	//タブ上の文字列と画像
        secondTab.setContent(R.id.second_content);	//表示するView
        tabHost.addTab(secondTab);

        //Tabの高さを設定
        tabHost.getTabWidget().getChildAt(0).getLayoutParams().height=65;
        tabHost.getTabWidget().getChildAt(1).getLayoutParams().height=65;
        
        //Local側のView
        list1 = (ListView) findViewById(R.id.ListView01);
        path = (TextView)findViewById(R.id.TextView01);
        path.setHeight(20);
        path.setTextSize(15);
        path.setBackgroundColor(Color.CYAN);
        path.setTextColor(Color.BLACK);

        //Remote側のView
        list2 = (ListView) findViewById(R.id.ListView02);
        localPath = (TextView)findViewById(R.id.TextView02);
        localPath.setHeight(20);
        localPath.setTextSize(15);
        localPath.setBackgroundColor(Color.CYAN);
        localPath.setTextColor(Color.BLACK);


        //初期画面からのインテントデータ取得
    	Intent intent = getIntent();
    	serverAddress = intent.getStringExtra("nrftp_serverAddress");
		userName = intent.getStringExtra("nrftp_userName");
		password = intent.getStringExtra("nrftp_password");
		port =  intent.getStringExtra("nrftp_port");
		remoteDir = intent.getStringExtra("nrftp_remoteDir");
		localDir = intent.getStringExtra("nrftp_localDir");
		if(remoteDir.compareTo("null") == 0)remoteDir = "";
        if(localDir.compareTo("null") == 0)localDir = "";
		
		//ローカルファイルリスト関連
        localFileListUpdate();
        list2.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
        	public boolean onItemLongClick(AdapterView parent,View view,int position,long id){
        		final File removeFile = localFiles[position];
        		Log.d(getString(R.string.app_name),removeFile.getName()+"long click selected");

        		AlertDialog.Builder rmbldr = new AlertDialog.Builder(me);
        		rmbldr.setTitle(getString(R.string.FTPMAIN_DEL_INFO));
        		rmbldr.setMessage(removeFile.getName()+ getString(R.string.FTPMAIN_DEL_CONFIRM));
        		rmbldr.setPositiveButton(getString(R.string.FTPMAIN_DEL_BUTTON_DELETE), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						removeFile.delete();
						localFileListUpdate();
					}
				});
        		rmbldr.setNegativeButton(getString(R.string.FTPMAIN_DEL_BUTTON_CANCEL), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
					}
				});
        		rmbldr.create();
        		rmbldr.show();
        		return true;
        	}
		});
        list2.setOnItemClickListener(new AdapterView.OnItemClickListener() {  
            public void onItemClick(AdapterView parent, View view, int position, long id) {
            	final File putFile = localFiles[position];
            	Log.d(getString(R.string.app_name),putFile.getName() +" selected");
            	if(putFile.isDirectory() || putFile.getName().compareTo(getString(R.string.FTPMAIN_PARENT_FOLDER)) == 0){
            		//フォルダの場合はそこへ移動
            		localPath.setText(getString(R.string.FTPMAIN_UL_CURRENT_DIR)+" "+changeLocalDirectory(putFile.getName()));
            		localFileListUpdate();
            	}else{
	            	AlertDialog.Builder bldr = new AlertDialog.Builder(me);
	            	bldr.setTitle(getString(R.string.FTPMAIN_UL_INFO));
	            	bldr.setMessage(putFile.getName()+" "+putFile.length()+getString(R.string.FTPMAIN_UL_MESSAGE));
	            	bldr.setPositiveButton(getString(R.string.FTPMAIN_UL_BUTTON_START),new DialogInterface.OnClickListener() {
	    				@Override
	    				public void onClick(DialogInterface dialog, int which) {
	    					ongoing = true;
	    					
	    					//転送中のプログレス表示
	    					progressDialog = new ProgressDialog(me); 
	    					progressDialog.setMax((int)putFile.length());
	    					progressDialog.incrementProgressBy(0);  
	    					progressDialog.incrementSecondaryProgressBy(5);  
	    			        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    			        progressDialog.setMessage(getString(R.string.FTPMAIN_UL_PROGRESS)+putFile.getName());
	    			        progressDialog.setCancelable(true);
	    			        
	    				    // ProgressDialog の Cancel ボタン  
	    				    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,"Cancel",new DialogInterface.OnClickListener() {  
	    				        public void onClick(DialogInterface dialog, int which) {  
	    				          // ProgressDialog をキャンセル  
	    				        	dialog.cancel();   
	    				        	try{
	    				        		cl.abortCurrentDataTransfer(false);
	    				        		//currentSize = progressDialog.getMax();	
	    				        		ftpResultStr = getString(R.string.FTPMAIN_TRANSFER_CANCEL);
	    				        		progressDialog.dismiss();
	    				        		ongoing = false;
	    				        		Log.d(getString(R.string.app_name),"Transfer canceled.");
	    				        	}catch(Exception e){
	    				        		Log.d(getString(R.string.app_name),e.toString());
	    				        	}
	    				        } 
	    				    });  
	    				    //キャンセルボタンのリスナー
	    					progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {  
	    						public void onCancel(DialogInterface dialog) {  
	    							try{
	    								cl.abortCurrentDataTransfer(false);
	    								//progressを強制終了するため・・
	    								//currentSize = progressDialog.getMax();
	    								ftpResultStr = getString(R.string.FTPMAIN_TRANSFER_CANCEL);
	    								progressDialog.dismiss();
	    								ongoing = false;
	    								Log.d(getString(R.string.app_name),"Transfer canceled.");
	    							}catch(Exception e){
	    								Log.d(getString(R.string.app_name),e.toString());
	    							}
	    						}  
						    }); 
	    					progressDialog.show();
	
	    					
	    					//FTP DL Thread
	    					new Thread(new Runnable(){
	    						@Override
						    	public void run(){
	    							ftpResultStr = ftpUpload(putFile);
	    							ongoing = false;
	    						} 
	    					}).start();
	    					
	    					//Progress Thread
	    					new Thread(new Runnable(){
	    						 	@Override
	    					    	public void run(){
	    				    		    while (true) {  
		    				    		   progressDialog.setProgress(currentSize);  
		    				    		   try{  
		    				    			   Thread.sleep(200);    
		    				    		   } catch (InterruptedException e) {
		    				    			   
		    				    		   }
		    				    		   //if (currentSize >= progressDialog.getMax()) {  
		    				    		   if(!ongoing){
		    				    			   break;  
		    				    		   }  
	    				    		    }
	    				    		    progressDialog.dismiss(); 
	    				    		    ongoing = false;
	    				    		    currentSize = 0;
	    				    		    //resultDisplay();
	    				    		    startIntent();
	    				    	   }
	    					}).start();
	    				}
	    			});
	            	bldr.setNegativeButton(getString(R.string.FTPMAIN_UL_BUTTON_CANCEL),new DialogInterface.OnClickListener() {
	    				@Override
	    				public void onClick(DialogInterface dialog, int which) {
	    				}
	    			});
	            	bldr.create();
	            	bldr.show();
            	}  
        	}
        });  
  
		//FTPリモートファイルリスト関連
		if(!ftpConnect(serverAddress,userName,password)){
			//FTP接続NG
			setResult(RESULT_CANCELED,intent);
			finish();
		}else{
			String currentDir = getCurrentDirectory();
			
			//よくわからんからあとで直す
			if(remoteDir != ""){
				remoteDir = changeDirectory(remoteDir);
				if(remoteDir != ""){
					path.setText(getString(R.string.FTPMAIN_DL_CURRENT_DIR)+" "+remoteDir);
				}else{
					path.setText(getString(R.string.FTPMAIN_DL_CURRENT_DIR)+" "+currentDir);
					Toast.makeText(this, getString(R.string.FTPMAIN_DL_CHANGE_DIR_NG), Toast.LENGTH_LONG);
				}
			}else{
				remoteDir = currentDir;
				path.setText(getString(R.string.FTPMAIN_DL_CURRENT_DIR)+" "+currentDir);
			}
	        
	        remoteFileListUpdate();
	        list1.setOnItemClickListener(new AdapterView.OnItemClickListener() {  
	            public void onItemClick(AdapterView parent, View view, int position, long id) {
	            	final FTPFile getFile = remoteList[position];
	            	Log.d(getString(R.string.app_name),getFile.getName() +"selected");
	            	if(getFile.getType()== FTPFile.TYPE_DIRECTORY){
	            		//フォルダの場合はそこへ移動
	            		path.setText(getString(R.string.FTPMAIN_DL_CURRENT_DIR)+" "+changeDirectory(getFile.getName()));
	            		remoteFileListUpdate();
	            	}else{
		            	AlertDialog.Builder bldr = new AlertDialog.Builder(me);
		            	bldr.setTitle(getString(R.string.FTPMAIN_DL_INFO));
		            	bldr.setMessage(getFile.getName()+" "+getFile.getSize()+getString(R.string.FTPMAIN_DL_MESSAGE));
		            	bldr.setPositiveButton(getString(R.string.FTPMAIN_DL_BUTTON_START),new DialogInterface.OnClickListener() {	
		    				@Override
		    				public void onClick(DialogInterface dialog, int which) {
		    					//転送状態開始フラグ
		    					ongoing = true;
		    					
		    					//転送中のプログレス表示
		    					progressDialog = new ProgressDialog(me); 
		    					progressDialog.setMax((int)getFile.getSize());
		    					progressDialog.incrementProgressBy(0);  
		    					progressDialog.incrementSecondaryProgressBy(5);  
		    			        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		    			        progressDialog.setMessage(getString(R.string.FTPMAIN_DL_PROGRESS)+getFile.getName());
		    			        progressDialog.setCancelable(true);
		    			        
		    				    // ProgressDialog の Cancel ボタン  
		    				    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.FTPMAIN_DL_BUTTON_CANCEL), new DialogInterface.OnClickListener() {  
		    				    	public void onClick(DialogInterface dialog, int which) {  
		    				    		// ProgressDialog をキャンセル  
		    				    		try{
			    				    		dialog.cancel();  
		    								cl.abortCurrentDataTransfer(false);
		    								//progressを強制終了するため・・
		    								//currentSize = progressDialog.getMax();
		    								progressDialog.dismiss();
		    								currentSize=0;
		    								ongoing = false;
		    								ftpResultStr = getString(R.string.FTPMAIN_TRANSFER_CANCEL);
		    								Log.d(getString(R.string.app_name),"Transfer canceled.");
		    							}catch(Exception e){
		    								Log.d(getString(R.string.app_name),e.toString());
		    							}
		    				    		
		    				    	}  
		    				    });  
		    				    //キャンセルボタンのリスナー
		    					progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {  
		    						public void onCancel(DialogInterface dialog) {  
		    							try{
		    								cl.abortCurrentDataTransfer(false);
		    								//progressを強制終了するため・・
		    								//currentSize = progressDialog.getMax();
		    								progressDialog.dismiss();
		    								ftpResultStr = getString(R.string.FTPMAIN_TRANSFER_CANCEL);
		    								currentSize = 0;
		    								ongoing = false;
		    								Log.d(getString(R.string.app_name),"Transfer canceled.");
		    							}catch(Exception e){
		    								Log.d(getString(R.string.app_name),e.toString());
		    							}
		    						}  
	    					    }); 
		    					progressDialog.show();
	
		    					
		    					//FTP DL Thread
		    					new Thread(new Runnable(){
		    						@Override
	    					    	public void run(){
		    							ftpResultStr = ftpDownload(getFile);
		    							ongoing = false;
		    						} 
		    					}).start();
		    					
		    					//Progress Thread
		    					new Thread(new Runnable(){
		    						 	@Override
		    					    	public void run(){
		    				    		    while (true) {  
			    				    		   progressDialog.setProgress(currentSize);  
			    				    		   try{  
			    				    			   Thread.sleep(200);    
			    				    		   } catch (InterruptedException e) {
			    				    		   }  
			    				    		   //if (currentSize >= progressDialog.getMax() || ftpResultStr.compareTo("") != 0) {  
			    				    		   if(!ongoing){
		    				    				   break;  
			    				    		   }  
		    				    		    }
		    				    		    progressDialog.dismiss(); 
		    				    		    //ongoing = false;
		    				    		    currentSize = 0;
		    				    		    //resultDisplay();
		    				    		    startIntent();
		    				    		    
		    				    	   }
		    					}).start();
		    				}
		    			});
		            	bldr.setNegativeButton(getString(R.string.FTPMAIN_DL_BUTTON_CANCEL),new DialogInterface.OnClickListener() {
		    				@Override
		    				public void onClick(DialogInterface dialog, int which) {
		    					//ftpDownload(tempFileName);
		    					//Log.d(getString(R.string.app_name),"DL Canceled");
		    				}
		    			});
		            	bldr.create();
		            	bldr.show();
	            	}
	            }  
	        });  
	        setResult(RESULT_OK,intent);
		}
        uriCheck();
    }


    
    private String getCurrentDirectory(){
    	ftpConnectionCheck();
    	Log.d(getString(R.string.app_name),"PWD");
    	try{
    		return cl.currentDirectory();
    	}catch(Exception e){
    		Log.d(getString(R.string.app_name),e.toString());
    	}
    	return null;

    }
    
    private FTPFile[] getRemoteFileList(){
    	ftpConnectionCheck();
    	Log.d(getString(R.string.app_name),"LIST");
    	FTPFile[] temp;
    	try{
    		temp = cl.list();
    		FTPFile[] temp2 = new FTPFile[temp.length+1];
    		for(int i =0;i<temp.length;i++){
    			temp2[i] = temp[i];
    		}
    		
    		FTPFile a = new FTPFile();
    		a.setName(getString(R.string.FTPMAIN_PARENT_FOLDER));
    		a.setType(FTPFile.TYPE_DIRECTORY);
    		temp2[temp2.length-1] = a;
    		return temp2;
    	}catch(Exception e){
    		Log.d(getString(R.string.app_name),e.toString());
    	}
    	return null;
    }
    
    private String changeDirectory(String dir){
    	ftpConnectionCheck();
    	String currentDir="";
    	try{
    		if(dir == getString(R.string.FTPMAIN_PARENT_FOLDER)){
    			cl.changeDirectoryUp();
				Log.d(getString(R.string.app_name),"cd ..");
				currentDir = getCurrentDirectory();
	    		Log.d(getString(R.string.app_name),"remote dir="+currentDir);
    		}else if(dir != ""){
				Log.d(getString(R.string.app_name),"cd "+ dir);
	    		cl.changeDirectory(dir);
	    		currentDir = getCurrentDirectory();
	    		Log.d(getString(R.string.app_name),"remote dir="+currentDir);
			}
    	}catch(Exception e){
    		Toast.makeText(this,getString(R.string.FTPMAIN_CHANGE_DIR_FAIL)+e.toString(),Toast.LENGTH_LONG);
    		Log.d(getString(R.string.app_name),"change directory is failed!!");
    	}
    	remoteDir = currentDir;

		return currentDir;

    }

    private String changeLocalDirectory(String Dir){
    	if(Dir.compareTo(getString(R.string.FTPMAIN_PARENT_FOLDER))==0){
    		if(Dir.compareTo("/") == 0){
    			localDir = "/";
    			return localDir;
    		}
    		/*
    		 * 2回実施で一つ上のフォルダを表示
    		 */
    		localDir = localDir.substring(0,localDir.lastIndexOf('/'));
    		localDir = localDir.substring(0,localDir.lastIndexOf('/')+1);
    	}else{
    		localDir += Dir;
    	}
    	uriCheck();
    	return localDir;
    }
    
    
    
    private String ftpDownload(FTPFile filename){

    	ftpConnectionCheck();
    	
    	boolean result = false;
    	String errText = "";
    	
    	Log.d(getString(R.string.app_name),"FTP File Download start :"+filename);
    	ContentValues val= new ContentValues();
    	
    	Date start = new Date();
    	Date finish = new Date();
    	try{
    		cl.download(filename.getName(), new File(localDir +filename.getName()), new MyTransferListener());
    		finish = new Date();
    		Log.d(getString(R.string.app_name),"FTP File Download success");
    		result = true;
    	}catch(Exception e){
    		Log.d(getString(R.string.app_name),"FTP File download failed");
    		Log.d(getString(R.string.app_name),e.toString());
    		errText = e.toString();
    		
    	}
    	
    	double elapsedSec = (finish.getTime() - start.getTime()) ;
    	double kbps = 0;
    	try{
    		kbps= (double)(((double)filename.getSize()*(double)8) / (double)elapsedSec );
    	}catch(Exception e){
    		Log.d(getString(R.string.app_name),"elapsedtime is "+elapsedSec);
    	} 
    	elapsedSec = elapsedSec/1000;//ミリ秒から秒へ
    	String tempResult= doubleToStringDecimalPlace(kbps,2);    	
    	
    	val.put("Transfer", "DL");
    	val.put("Date", start.getMonth()+1+"/"+start.getDate());
    	val.put("StartTime", start.getHours()+":"+start.getMinutes()+":"+start.getSeconds());
    	val.put("FinishTime", finish.getHours()+":"+finish.getMinutes()+":"+finish.getSeconds());
    	val.put("ElapsedTime", elapsedSec);
    	val.put("FileName", filename.getName());
    	val.put("FileSize",filename.getSize());
    	val.put("FileServer", serverAddress); 
    	if(result){
    		val.put("Misc", tempResult);
    	}else{
    		val.put("Misc",errText);
    	}

    	//dbに結果を格納
        ftpresult = dbHelper.getWritableDatabase();
    	ftpresult.insert("result", null, val);
    	ftpresult.close();
    	Log.d(getString(R.string.app_name),val.toString());
    	
    	if(result){
    		return  getString(R.string.FTPMAIN_RESULT_FILESIZE)+filename.getSize() +" "
    		          + getString(R.string.FTPMAIN_RESULT_BYTE_AND_ELAPSED)+elapsedSec+" "
    		          + getString(R.string.FTPMAIN_RESULT_SEC_AND_SPEED)
    		          + tempResult+" kbps";
    	}else{
    		return errText;
    	}
    	
    }
    
    private String ftpUpload(File filename){
    
    	ftpConnectionCheck();
    	
    	boolean result = false;
    	String errText = "";
    	Log.d(getString(R.string.app_name),"FTP File Upload start :"+filename);
    	ContentValues val= new ContentValues();
    	Date start = new Date();
    	Date finish = new Date();
    	
    	try{
    		cl.upload(filename,new MyTransferListener());
    		finish = new Date();
    		result = true;
    		Log.d(getString(R.string.app_name),"FTP File upload success");
    	}catch(Exception e){
    		errText = e.toString();
    		Log.d(getString(R.string.app_name),"FTP File upload Failed");
    		Log.d(getString(R.string.app_name),e.toString());
    	}

    	double elapsedSec = (finish.getTime() - start.getTime()) ;
    	double kbps = 0;
    	try{
    		kbps= (double)(((double)filename.length()*(double)8) / (double)elapsedSec );
    	}catch(Exception e){
    		Log.d(getString(R.string.app_name),"elapsedtime is "+elapsedSec);
    	}
    	elapsedSec = elapsedSec/1000;//ミリ秒から秒へ
    	String tempResult= doubleToStringDecimalPlace(kbps,2);
    	
    	val.put("Transfer", "UL");
    	val.put("Date", start.getMonth()+1+"/"+start.getDate());
    	val.put("StartTime", start.getHours()+":"+start.getMinutes()+":"+start.getSeconds());
    	val.put("FinishTime", finish.getHours()+":"+finish.getMinutes()+":"+finish.getSeconds());
    	val.put("ElapsedTime", elapsedSec);
    	val.put("FileName", filename.getName());
    	val.put("FileSize",filename.length());
    	val.put("FileServer", serverAddress); 
    	if(result){
    		val.put("Misc", tempResult);
    	}else{
    		val.put("Misc",errText);
    	}
    	//dbへ結果を格納
        ftpresult = dbHelper.getWritableDatabase();
    	ftpresult.insert("result", null, val);
    	ftpresult.close();
    	Log.d(getString(R.string.app_name),val.toString());
    	if(result){
    		return  getString(R.string.FTPMAIN_RESULT_FILESIZE)+filename.length()
		          + getString(R.string.FTPMAIN_RESULT_BYTE_AND_ELAPSED)+elapsedSec
		          + getString(R.string.FTPMAIN_RESULT_SEC_AND_SPEED)
		          + tempResult+"kbps";
    	}else{
    		return errText;
    	}
    }
    private void uriCheck(){
    	if(localDir.lastIndexOf('/') != localDir.length()-1){
    		localDir += '/';
    	}
    	if(localDir.indexOf('/') != 0){
    		localDir = '/' + localDir;
    	}
    }
    
    private void ftpConnectionCheck(){
    	boolean isconnected = false;
    	try{
			if(cl.currentDirectory() != ""){
				isconnected = true;
			}
			
    	}catch(Exception e){
    		Log.d(getString(R.string.app_name),e.toString());
    		isconnected = false;
    		

    	}
    	
    	if(!isconnected){
    		try{
    			cl.disconnect(false);
    			if(!ftpConnect(serverAddress,userName,password)){
    				//FTP接続NG
    				finish();
    			}
    			changeDirectory(remoteDir);
    			Log.d(getString(R.string.app_name),"FTP session re-connect.");
    		}catch(Exception e){
    			Log.d(getString(R.string.app_name),"Re connect Failed..");
    		}
    	}
    	
    	
    	
    	
    }

    private boolean ftpConnect(String Url,String UserName,String Password){
    	Log.d(getString(R.string.app_name),"FTP Login..");
    	try{    	
    		if(port.compareTo("") == 0){
    			cl.connect(Url);
    		}else{
    			cl.connect(Url,Integer.parseInt(port));
    		}
    		
    		cl.login(UserName,Password);
    		
    		Log.d(getString(R.string.app_name),"FTP Login success");
    		return true;
    	}catch(Exception e){
    		Log.d(getString(R.string.app_name),e.toString());
    	}
    	return false;
    	
    }
    
    private void ftpLogout(){
    	if(cl != null && cl.isConnected()){
			try{
				cl.disconnect(true);
				Log.d(getString(R.string.app_name),"FTP Connection close.");
			}catch(Exception e){
				Log.d(getString(R.string.app_name),e.toString());
			}
    	}
	}

    @Override
    public void onResume(){
    	super.onResume();
    	
    }
    @Override
    public void onPause(){
    	super.onPause();
    }
    @Override
    public void onStop(){
    	super.onStop();
    	//ftpLogout();
    }
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	ftpLogout();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event){
    	if(event.getAction()==KeyEvent.ACTION_DOWN){
    		switch(event.getKeyCode()){
    		case KeyEvent.KEYCODE_BACK:
    			if(currentSize != 0){
    				return true;
    			}
    		}
    	}
    	return super.dispatchKeyEvent(event);
    }
    
    public View createTabContent(String tag) {
        TextView textView = new TextView(this);
        textView.setText("Content for tab with tag " + tag);
        return textView;
    }

    public class MyTransferListener implements FTPDataTransferListener {

    	public void started() {
    		// Transfer started
    		Log.d(getString(R.string.app_name),"transfer started");
    	}

    	public void transferred(int length) {
    		// Yet other length bytes has been transferred since the last time this
    		// method was called
    		if(ongoing){
    			currentSize += length;
    		}
    	}

    	public void completed() {
    		// Transfer completed
    		Log.d(getString(R.string.app_name),"transfer completed");
    	}

    	public void aborted() {
    		// Transfer aborted
    		//progressdialogを強制終了するため。。
    		//currentSize = progressDialog.getMax();
    		progressDialog.dismiss();
    		currentSize = 0;
    		ongoing = false;
    		ftpLogout();
    		Log.d(getString(R.string.app_name),"transfer aborted");    		
    	}

    	public void failed() {
    		// Transfer failed
    		//progressdialogを強制終了するため。
    		//currentSize = progressDialog.getMax();
    		progressDialog.dismiss();
    		currentSize = 0;
    		ongoing = false;
    		ftpLogout();
    		Log.d(getString(R.string.app_name),"transfer failed");
    	}

    }

    public void resultDisplay(){
		//結果格納待ちループ 危険だけど。
		while(ftpResultStr == ""){
	    	Log.d(getString(R.string.app_name),"result waiting..");
			   try{
			   Thread.sleep(300); 
		   }catch(Exception e){}
	    }
    	
		AlertDialog.Builder bldr = new AlertDialog.Builder(ftpmain2.this);
		bldr.setTitle(getString(R.string.FTPMAIN_RESULT_INFO));
		bldr.setMessage("temp");
        bldr.setPositiveButton(getString(R.string.FTPMAIN_RESULT_ACCEPT),new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
        bldr.create();
        bldr.show();
        localFileListUpdate();
        remoteFileListUpdate();
    	
    }
    
    //TODO
    //resultDisplay()が完成したら消す。
	public void startIntent(){
		//結果格納待ちループ 危険だけど。
		while(ftpResultStr == ""){
	    	Log.d(getString(R.string.app_name),"result waiting..");
			   try{
			   Thread.sleep(300); 
		   }catch(Exception e){}
	    }
		
		/*
		 * TODO
		 * エラー文字列を解析してメッセージを出したい
		 * 現状は例外発生を文字列でそのまま出力しているので。
		 * あと可能ならIntentも止めたい。。
		 */
		Log.d(getString(R.string.app_name),"start intent resultDisp");
		Intent intent = new Intent(this, resultDisp.class);
		String temp = ftpResultStr;
		ftpResultStr = "";
		currentSize = 0;
		intent.putExtra("resultDisp",temp);
		startActivityForResult(intent,FTP_RESULT);
		
    }
    
	public void showHistory(){
		Log.d(getString(R.string.app_name),"start intent showHistory");
		Intent intent = new Intent(this, history.class);
		startActivity(intent);
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == FTP_RESULT){
			if(resultCode == RESULT_OK){
				AlertDialog.Builder bldr = new AlertDialog.Builder(this);
				bldr.setTitle(getString(R.string.FTPMAIN_RESULT_INFO));
				bldr.setMessage(data.getStringExtra("result"));
	            bldr.setPositiveButton(getString(R.string.FTPMAIN_RESULT_ACCEPT),new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
	            bldr.create();
	            bldr.show();
	            localFileListUpdate();
	            remoteFileListUpdate();
			}
		}
	}

	private String doubleToStringDecimalPlace(double num,int point){
		String ret;
    	try {
    		// 10進数Formatオブジェクト
    	        DecimalFormat df=new DecimalFormat();

    		// パターンを設定
    	        df.applyPattern("0");

    		// 小数点以下の桁数を指定
    		// この引数にどちらも同じ値を入れると、
    		// 固定小数値になります。
    	        df.setMaximumFractionDigits(point);
    	        df.setMinimumFractionDigits(point);

    		// double変数をDoubleオブジェクトとして作成
    	        Double objnum=new Double(num);
    		
    		// フォーマット
    	        ret=df.format(objnum);
    		
    	}catch(Exception e){
    		ret=null;
    	}
		return ret;
	}
    
	private String[] fileListToListView(FTPFile[] files){
		String fileList[] = new String[files.length];
		int temp=0;
		for(FTPFile fi:remoteList){
			if(fi.getType() == FTPFile.TYPE_DIRECTORY){
				fileList[temp++] = fi.getName()+getString(R.string.FTPMAIN_FOLDER);
			}else{
				fileList[temp++] = fi.getName()+" \n "+fi.getSize()+" bytes";
			}
		}
		return fileList;
	}
	
	private String[] fileListToListView(File[] files){
		String fileList[] = new String[files.length];

		int temp = 0;
		for(File lf:files){
			if(lf.isDirectory() || lf.getName().compareTo(getString(R.string.FTPMAIN_PARENT_FOLDER)) == 0){
				fileList[temp++] = lf.getName() +getString(R.string.FTPMAIN_FOLDER);
			}else{
				fileList[temp++] = lf.getName() +" \n "+lf.length()+" bytes";
			}
		}		
		return fileList;
	}
	
	private void localFileListUpdate(){
		//LocalFile取得と表示
		Log.d(getString(R.string.app_name),"Local file list update.");	
		uriCheck();
		localPath.setText(getString(R.string.FTPMAIN_LOCAL_FOLDER)+" "+localDir);
		
		File temp[],temp2[];
		temp = new File(localDir).listFiles();
		File a = new File(getString(R.string.FTPMAIN_PARENT_FOLDER));
		/*
		 * Permissionが無い場合、もしくはファイルが１個もない場合の対処
		 */
		if(temp == null){
			temp2 = new File[1];
			temp2[0] = a;
		}else{
			temp2 = new File[temp.length+1];
			for(int i = 0;i<temp.length;i++){
				temp2[i] = temp[i];
			}
			temp2[temp.length]= a;
		}
		
		localFiles = temp2;
		localAdapter = new ArrayAdapter<String>(this, R.layout.list, fileListToListView(localFiles));
        localAdapter.notifyDataSetChanged();
        list2.setAdapter(localAdapter);
        list2.setSelection(0);
	}
	
	private void remoteFileListUpdate(){
		ftpConnectionCheck();
		Log.d(getString(R.string.app_name),"Remote file list update.");
		remoteList = getRemoteFileList();
        adapter = new ArrayAdapter<String>(this, R.layout.list, fileListToListView(remoteList));
        adapter.notifyDataSetChanged();
        list1.setAdapter(adapter);
        list1.setSelection(0);	
		
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ITEM1, Menu.NONE, getString(R.string.FTPMAIN_OP_MENU_UPDATE));
        menu.add(Menu.NONE, MENU_ITEM2, Menu.NONE, getString(R.string.FTPMAIN_OP_MENU_HISTORY));
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM1:
            //更新処理。必要ならProgressDialogでもいれるかも。
        	localFileListUpdate();
        	remoteFileListUpdate();
            return true;
            
       case MENU_ITEM2:
        	showHistory();
            return true;
        }
        return false;
    }
    
	
}