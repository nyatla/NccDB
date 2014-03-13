

import jp.nyatla.nyansat.db.SqliteDB;
import jp.nyatla.nyansat.utils.ArgHelper;
import jp.nyatla.nyansat.utils.SdbException;


public class Main
{
	public static final String VERSION="1.1.0";
	public static final String ENV_DB_PATH="NccDB.sqlite3";
	
	public static void main(String[] args)
	{
		try{
			ArgHelper a=new ArgHelper(args);			
			String c=a.getString("-cmd",null);
			if(c==null){
				throw new SdbException("-cmd not found");
			}
			String db_path=a.getString("-db",ENV_DB_PATH);
			if(db_path==null){
				throw new SdbException();
			}
			SqliteDB db=new SqliteDB(db_path);
			
			if(CoinListCsvIo.run(c,a,db)){
			}else if(UrlListCsvIo.run(c,a,db)){
			}else{
				throw new Exception("Bad -cmd");
			}
		}catch(Throwable e){
			System.out.println("nccdb - NyatlaCryptCoinDatabase version "+VERSION);
			System.out.println("-cmd ["
				+"init|"
				+"coin_importcsv|"
				+"coin_exportcsv|"
				+"coin_addlist|"
				+"url_importcsv|"
				+"url_exportcsv"
				+"]"
				);
			System.out.println("-db [dbpath] ex d:/table.sqlite");
			System.out.println("cmd==[coin_importcsv|coin_exportcsv]");
			System.out.println("-csv [csv file name] default './coinspec.csv'");
			System.out.println("cmd==[coin_addlist]");
			System.out.println("-url [forum url list] default '[:presetted cryptocointalk.com coin list:]'");
			System.out.println("-ua [optional useragent to http request]");
			System.out.println("-cookie [optional cookie to http request]");
			System.out.println();
			e.printStackTrace();
		}
	}
}
