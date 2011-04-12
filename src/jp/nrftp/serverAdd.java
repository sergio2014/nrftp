package jp.nrftp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
public class serverAdd extends Activity {
	
	Button 		addButton,
				cancelButton;
	
	EditText 	editTextName,
				editTextAddress,
				editTextUser,
				editTextPassword,
				editTextPort,
				editTextRemotedir,
				editTextLocaldir;

	@Override    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serveradd);
        initialViewSetting();

	}	
	
	public void initialViewSetting(){
        addButton = (Button)findViewById(R.id.ADD);
        addButton.setOnClickListener(new OnClickListener() {  
			@Override
			public void onClick(View v) {
				serverSettingAdd();
			}
        });
        cancelButton = (Button)findViewById(R.id.CANCEL);
        cancelButton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v){
        		serverSettingCancel();
        	}
        });
		
        editTextName 		=(EditText)findViewById(R.id.server_add_edit_name);
		editTextAddress		=(EditText)findViewById(R.id.server_add_edit_address);
		editTextUser		=(EditText)findViewById(R.id.server_add_edit_user);
		editTextPassword	=(EditText)findViewById(R.id.server_add_edit_pass);
		editTextPort		=(EditText)findViewById(R.id.server_add_edit_port);
		editTextRemotedir	=(EditText)findViewById(R.id.server_add_edit_remotedir);
		editTextLocaldir	=(EditText)findViewById(R.id.server_add_edit_localdir);
		
		//Editで飛んできた場合
		Intent intent = getIntent();
		if(intent.getStringExtra("nrftp_name") != null){
			addButton.setText(getString(R.string.SERVER_ADD_BUTTON_EDIT));
			editTextName.setEnabled(false);
			
			editTextName.setText(intent.getStringExtra("nrftp_name"));
			editTextAddress.setText(intent.getStringExtra("nrftp_serverAddress"));
			editTextUser.setText(intent.getStringExtra("nrftp_userName"));
			editTextPassword.setText(intent.getStringExtra("nrftp_password"));
			editTextPort.setText(intent.getStringExtra("nrftp_port"));
			if(intent.getStringExtra("nrftp_remoteDir").compareTo("null") != 0)
				editTextRemotedir.setText(intent.getStringExtra("nrftp_remoteDir"));
			if(intent.getStringExtra("nrftp_localDir").compareTo("null") != 0)
				editTextLocaldir.setText(intent.getStringExtra("nrftp_localDir"));
		}
	}
	
	public boolean serverSettingAdd(){
    	Intent intent = getIntent();   	
    	
    	String temp = editTextName.getText().toString();
    	
    	if(editTextName.getText().toString().length() != 0){
    		intent.putExtra("serverADD_NAME",editTextName.getText().toString());
    	}else{
    		Toast.makeText(serverAdd.this, "name is empty..", Toast.LENGTH_LONG);
    		return false;
    	}

    	if(editTextAddress.getText().toString().length() != 0){
    		intent.putExtra("serverADD_ADDRESS",	editTextAddress.getText().toString());
    	}else{
    		Toast.makeText(serverAdd.this, "name is empty..", Toast.LENGTH_LONG);
    		return false;
    	}

    	if(editTextUser.getText().toString().length() != 0){
    		intent.putExtra("serverADD_USER", editTextUser.getText().toString());
    	}else{
    		Toast.makeText(serverAdd.this, "name is empty..", Toast.LENGTH_LONG);
    		return false;
    	}

    	if(editTextPassword.getText().toString().length() != 0){
    		intent.putExtra("serverADD_PASS", editTextPassword.getText().toString());
    	}else{
    		Toast.makeText(serverAdd.this, "name is empty..", Toast.LENGTH_LONG);
    		return false;
    	}
    	
    	if(editTextPort.getText().toString().length() != 0){
    		intent.putExtra("serverADD_PORT", editTextPort.getText().toString());
    	}else{
    		intent.putExtra("serverADD_PORT","21");
    	}
    	
    	if(editTextRemotedir.getText().toString().length() != 0){
    		intent.putExtra("serverADD_REMOTE_DIR", editTextRemotedir.getText().toString());
    	}else{
    		intent.putExtra("serverADD_REMOTE_DIR", "null");
    	}
    	
    	if(editTextLocaldir.getText().toString().length() != 0){
    		intent.putExtra("serverADD_LOCAL_DIR", 	editTextLocaldir.getText().toString());
    	}else{
    		intent.putExtra("serverADD_LOCAL_DIR", "null");
    	}
    	
		setResult(RESULT_OK,intent);
		finish();
		return true;
	}
	
	public void serverSettingCancel(){
		Intent intent = new Intent();
		setResult(RESULT_CANCELED,intent);
		finish();
	}
	
	
}
