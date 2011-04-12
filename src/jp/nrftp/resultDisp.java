package jp.nrftp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
public class resultDisp extends Activity {

	@Override    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
    	Intent intent = getIntent();
    	String tempResult = intent.getStringExtra("resultDisp");
    	intent.putExtra("result", tempResult);
		setResult(RESULT_OK,intent);
		finish();
	}	
}