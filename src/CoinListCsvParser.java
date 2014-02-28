import java.io.IOException;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;

import jp.nyatla.nccdb.CoinAlgorismTable;
import jp.nyatla.nccdb.CryptCoinTankListScraper;
import jp.nyatla.nccdb.table.*;
import jp.nyatla.nyansat.db.basic.table.SqliteDB;
import jp.nyatla.nyansat.utils.ArgHelper;
import jp.nyatla.nyansat.utils.CsvReader;
import jp.nyatla.nyansat.utils.CsvWriter;
import jp.nyatla.nyansat.utils.SdbException;



/**
 * CoinListCSVをパースして、CoinTitleTableへ値を投入する。
 * CoinListCSVのフォーマットは以下の通り
 * symbol,name,start_date,coin_algorism,total_coin,premine,id_block_reword_type
 */
public class CoinListCsvParser
{
	private static void log(String s)
	{
		System.out.println(s);
	}


	private static void importCSV(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		//CSVをインポート(値はCSVを優先)
		log("Start importCSV");
		//ファイル
		String path=ap.getString("-csv", CSV_PATH);
		CoinTitleTable coin_title=null;
		CoinSpecTable coin_spec=null;
		try {
			coin_title=new CoinTitleTable(i_db);
			coin_spec=new CoinSpecTable(i_db);
			//ファイル読み込み
			CsvReader csv=new CsvReader(path);
			//列インデックスを得る。
			int coin_symbol_idx=csv.getIndex(CoinTitleTable.DN_symbol);
			int coin_name_idx=csv.getIndex(CoinTitleTable.DN_name);
			int coin_start_date_idx=csv.getIndex(CoinSpecTable.DN_start_date);
			int spec_total=csv.getIndex(CoinSpecTable.DN_total_coin);
			int spec_premine=csv.getIndex(CoinSpecTable.DN_premine);
			int spec_reword_type=csv.getIndex(CoinSpecTable.DN_id_block_reword_type);
			int coin_algorism_idx=csv.getIndex(CoinSpecTable.DN_id_algorism);
			while(csv.next()){
				String symbol=csv.getString(coin_symbol_idx);
				String name=csv.getString(coin_name_idx);
				//Titleを追加
				if(!coin_title.update(
					symbol,
					name))
				{
					throw new SdbException();
				}
				//Specを追加
				CoinTitleTable.Item it=coin_title.getItem(symbol,name);
				if(it!=null){
					if(!coin_spec.update(
						it.id,
						csv.getDate(coin_start_date_idx,null),
						csv.getDouble(spec_total,null),
						csv.getDouble(spec_premine,null),
						0,
						CoinAlgorismTable.getSingleton().getId(csv.getString(coin_algorism_idx),0)))
					{
						throw new SdbException();
					}
				}
			}
		} catch (Throwable e){
			throw new SdbException(e);
		}finally{
			if(coin_title!=null){
				coin_title.dispose();
			}
			if(coin_spec!=null){
				coin_spec.dispose();
			}
		}
		log("done.");
	}
	private static void exportCSV(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		log("Start exportCSV");
		//ファイル
		String path=ap.getString("-csv", CSV_PATH);
		//CSVをエクスポート
		CoinInfoView civ=null;
		try{
			CsvWriter writer=new CsvWriter(path);
			civ=new CoinInfoView(i_db);
			writer.writeCol(civ.getColHeader());
			writer.next();
			CoinInfoView.RowIterable it=civ.getAll();
			int p=0;
			for(CoinInfoView.Item i :it){
				log(i.symbol+" "+i.name);
				writer.writeCol(i.toCsvArrray());
				writer.next();
				p++;
				System.out.println(p+":"+i.symbol);
			}
			it.dispose();
			writer.flush();
		} catch (IOException e) {
			throw new SdbException(e);
		}finally{
		}
		log("done.");
	}
	/**
	 * CryptcoinTankからコインリストを取り込む。
	 * @param ap
	 * @param i_db
	 * @throws SdbException
	 */
	private static void syncryptCoinList(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		log("Start syncryptCoinList");
		CoinTitleTable ctt=null;
		CoinSpecTable cst=null;
		//URLリストの構築
		String[] url_list=cc_url_list;
		{
			String l=ap.getString("url", null);
			if(l!=null){
				url_list=l.split(",");
			}
		}
		//tableオープン
		try{
			ctt=new CoinTitleTable(i_db);
			cst=new CoinSpecTable(i_db);
			CryptCoinTankListScraper scp=new CryptCoinTankListScraper(ap.getString("-ua",null),ap.getString("-cookie",null));
			//CoinListの取得
			for(int i2=0;i2<url_list.length;i2++){
				log("Scan URL="+url_list[i2]);
				ArrayList<CryptCoinTankListScraper.Item> items=scp.parse(url_list[i2]);
				//テーブルを追記
				for(CryptCoinTankListScraper.Item i:items){
					boolean is_cct=ctt.add(i.symbol,i.name);
					log("["+(is_cct?"ADD":"DROP")+"]"+i.symbol+":"+i.name);
					CoinTitleTable.Item ccti=ctt.getItem(i.symbol,i.name);
					cst.add(ccti.id,null,null,null,0,0);
				}
			}
		}finally{
			if(ctt!=null){
				ctt.dispose();
			}
			if(cst!=null){
				cst.dispose();
			}
		}
		log("done.");
		
	}
	public static void initDB(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		log("Setup table and view of database.");
		CoinTitleTable ctt=null;
		CoinSpecTable cst=null;
		CoinInfoView civ=null;
		//tableオープン
		try{
			ctt=new CoinTitleTable(i_db);
			cst=new CoinSpecTable(i_db);
			civ=new CoinInfoView(i_db);
		}finally{
			if(ctt!=null){
				ctt.dispose();
			}
			if(cst!=null){
				cst.dispose();
			}
			if(civ!=null){
				civ.dispose();
			}
		}
		log("done.");
	}
	
	public static String[] cc_url_list={
		"https://cryptocointalk.com/forum/557-unlaunched-cryptocoins/",
		"https://cryptocointalk.com/forum/40-new-cryptocoins/",
		"https://cryptocointalk.com/forum/178-scrypt-cryptocoins/",
		"https://cryptocointalk.com/forum/886-dying-scrypt-cryptocoins/",
		"https://cryptocointalk.com/forum/179-sha-256-cryptocoins/",
		"https://cryptocointalk.com/forum/887-dying-sha256-cryptocoins/",
		"https://cryptocointalk.com/forum/302-other-algo-cryptocoins/",
		"https://cryptocointalk.com/forum/888-dying-other-algo-cryptocoins/"		
	};
	public static final String ENV_DB_PATH="NccDB.sqlite3";
	public static final String CSV_PATH="coinspec.csv";
	
	public static void main(String[] args)
	{/*
		String[] test_init={"-cmd","init"};
		String[] test_sync={
			"-cmd","addlist",
			"-ua","Mozilla/5.0 (Windows NT 6.3; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0",
			"-cookie","__cfduid=d254672ab0e9cde4465cf8db1fb8070961393564689506; cf_clearance=e0d5f0415fe59e32dbb613f96c1b557a95b4d51e-1393564695-7200; session_id=219e1c967692daf46e2d8bcccb186c68; _ga=GA1.2.256697107.1393564695"};
		String[] test_export={"-cmd","exportcsv"};
		String[] test_import={"-cmd","importcsv"};*/
		main2(args);
	}
	public static final String VERSION="1.0.0";
	public static void main2(String[] args)
	{
		try{
			ArgHelper a=new ArgHelper(args);
			String db_path=a.getString("-db",ENV_DB_PATH);
			if(db_path==null){
				throw new SdbException();
			}
			String c=a.getString("-cmd",null);
			if(c==null){
				throw new Exception("-cmd not found");
			}
			SqliteDB db=new SqliteDB(ENV_DB_PATH);

			if(c.compareTo("init")==0){
				initDB(a,db);
			}else if(c.compareTo("addlist")==0){
				syncryptCoinList(a,db);
			}else if(c.compareTo("exportcsv")==0){
				exportCSV(a,db);
			}else if(c.compareTo("importcsv")==0){
				importCSV(a,db);
			}else{
				throw new Exception("Bad -cmd");
			}
		}catch(Throwable e){
			System.out.println("nccdb - NyatlaCryptCoinDatabase version "+VERSION);
			System.out.println("-cmd [importcsv|exportcsv|addlist|init]");
			System.out.println("-db [dbpath] ex d:/table.sqlite");
			System.out.println("cmd==[importcsv|exportcsv]");
			System.out.println("-csv [csv file name] default './coinspec.csv'");
			System.out.println("cmd==[addlist]");
			System.out.println("-url [forum url list] default '[:presetted cryptocointalk.com coin list:]'");
			System.out.println("-ua [optional useragent to http request]");
			System.out.println("-cookie [optional cookie to http request]");
			System.out.println();
			e.printStackTrace();
		}
	}
}
