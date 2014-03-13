import java.io.IOException;

import jp.nyatla.nccdb.table.*;
import jp.nyatla.nccdb.table.internal.ServiceTypeTable;
import jp.nyatla.nyansat.db.SqliteDB;
import jp.nyatla.nyansat.utils.ArgHelper;
import jp.nyatla.nyansat.utils.CsvReader;
import jp.nyatla.nyansat.utils.CsvWriter;
import jp.nyatla.nyansat.utils.SdbException;



/**
 * URLリストCSVのIOコマンドの実装。
 * UrlListCsvは{@link CoinUrlTable}のダンプファイルです。
 * URLとそのサービスカテゴリの一覧を記述します。
 * <pre>
 * 形式
 * [id:optional],[date],[service_type],[name],[status],[url],[description]
 * 
 * </pre>
 */
public class UrlListCsvIo
{
	private static void importCSV(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		//CSVをインポート(値はCSVを優先)
		Logger.log("Start importCSV");
		//ファイル
		String path=ap.getString("-csv", CSV_PATH);
		CoinUrlTable coin_url=null;
		try {
			coin_url=new CoinUrlTable(i_db);
			//ファイル読み込み
			CsvReader csv=new CsvReader(path);
			//列インデックスを得る。	
			int name_idx		=csv.getIndex(CoinUrlTable.DN_name);
			int url_type_idx	=csv.getIndex(CoinUrlTable.DN_id_coin_url_type);
			int url_status_idx	=csv.getIndex(CoinUrlTable.DN_id_coin_url_status);
			int url_idx			=csv.getIndex(CoinUrlTable.DN_url);
			int description_idx	=csv.getIndex(CoinUrlTable.DN_description);
			while(csv.next()){
				String name=csv.getString(name_idx);
				int type_idx=ServiceTypeTable.getSingleton().getId(csv.getString(url_type_idx));
				int url_status=csv.getInteger(url_status_idx);
				String url=csv.getString(url_idx);
				String description=csv.getString(description_idx);
				//Titleを追加
				if(!coin_url.update(name,type_idx,url_status,url,description))
				{
					throw new SdbException();
				}
			}
		} catch (Throwable e){
			throw new SdbException(e);
		}finally{
			if(coin_url!=null){
				coin_url.dispose();
			}
		}
		Logger.log("done.");
	}
	private static void exportCSV(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		Logger.log("Start exportCSV");
		//ファイル
		String path=ap.getString("-csv", CSV_PATH);
		//CSVをエクスポート
		CoinUrlTable civ=null;
		try{
			CsvWriter writer=new CsvWriter(path);
			civ=new CoinUrlTable(i_db);
			writer.writeCol(civ.getColHeader());
			writer.next();
			CoinUrlTable.RowIterable it=civ.getAll();
			int p=1;
			for(CoinUrlTable.Item i :it){
				Logger.log(p+":"+i.name);
				writer.writeCol(i.toCsvArrray());
				writer.next();
				p++;
			}
			it.dispose();
			writer.flush();
		} catch (IOException e) {
			throw new SdbException(e);
		}finally{
		}
		Logger.log("done.");
	}
	public static final String CSV_PATH="coinurl.csv";	

	public static boolean run(String i_cmd,ArgHelper args,SqliteDB db) throws SdbException
	{
		if(i_cmd.compareTo("url_importcsv")==0){
			importCSV(args,db);
		}else if(i_cmd.compareTo("url_exportcsv")==0){
			exportCSV(args,db);
		}else{
			return false;
		}
		return true;
	}
}
