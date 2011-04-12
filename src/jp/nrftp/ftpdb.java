package jp.nrftp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class ftpdb extends SQLiteOpenHelper {
 
    /* データベース名 */
    private final static String DB_NAME = "FTP";
    /* データベースのバージョン */
    private final static int DB_VER = 1;
    
 //test//test//
    
    /*
     * コンストラクタ
      */
    public ftpdb(Context context) {
        super(context, DB_NAME, null, DB_VER);
    }

    /*
     * onCreateメソッド
     * データベースが作成された時に呼ばれます。
     * テーブルの作成などを行います。
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
    	//FTP 接続結果
        String sql = "";
        sql += "create table result (";
        sql += " No integer primary key autoincrement";
        sql += ",Transfer text not null";
        sql += ",Date text not null";
        sql += ",StartTime text not null";
        sql += ",FinishTime text";
        sql += ",ElapsedTime integer";
        sql += ",FileName text not null";
        sql += ",FileSize integer not null";
        sql += ",FileServer text not null";
        sql += ",Misc text";
        sql += ");";
        db.execSQL(sql);
        
        //FTP server
        sql = "";
        sql += "create table serverList (";
        sql += "No integer primary key autoincrement";
        sql += ", Name text not null";
        sql += ", Address text not null";
        sql += ", UserName text not null";
        sql += ", Password text not null";
        sql += ", Port integer";
        sql += ", RemoteDir";
        sql += ", LocalDir";
        sql += ");";
        db.execSQL(sql);
        
    }

 
    
    /*
     * onUpgradeメソッド
     * onUpgrade()メソッドはデータベースをバージョンアップした時に呼ばれます。
     * 現在のレコードを退避し、テーブルを再作成した後、退避したレコードを戻すなどの処理を行います。
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
  
    }

}
