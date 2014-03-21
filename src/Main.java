

import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.utils.ArgHelper;
import jp.nyatla.nyansat.utils.SdbException;


public class Main
{
	public static final String VERSION="1.2.2";
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
			}else if(ServiceCoinCsvIo.run(c,a,db)){
			}else{
				throw new Exception("Bad -cmd");
			}
		}catch(Throwable e){
			System.out.println("nccdb - NyatlaCryptCoinDatabase version "+VERSION);
			System.out.println(CoinListCsvIo.readme());
			System.out.println(UrlListCsvIo.readme());
			System.out.println(ServiceCoinCsvIo.readme());
			System.out.println();
			e.printStackTrace();
		}
	}
}
