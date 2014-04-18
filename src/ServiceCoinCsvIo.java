
import jp.nyatla.nccdb.table.CoinMasterTable;
import jp.nyatla.nccdb.table.CoinUrlIdPairTable;
import jp.nyatla.nccdb.table.ServiceUrlTable;
import jp.nyatla.nccdb.table.IdPairTable;
import jp.nyatla.nccdb.table.internal.ServiceTypeTable;
import jp.nyatla.nyansat.db.basic.RowIterable;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.utils.ArgHelper;
import jp.nyatla.nyansat.utils.CsvReader;
import jp.nyatla.nyansat.utils.CsvWriter;
import jp.nyatla.nyansat.utils.SdbException;

/**
 * ServiceCoinPairCSVのIOコマンドの実装。
 * ServiceCoinPairCSVは{@link CoinMasterTable},{@link ServiceUrlTable},{@link IdPairTable}と関連付けられます。
 * コインIDとサービスURLの関連の一覧をインポート/エクスポートします。
 * コインシンボルに対して、指定したサービスの対応状況を記述します。
 * 1つのサービスがN個のコインに対してサービスを提供していることを入力するときに使います。
 * <pre>
 * フォーマット
 * [id:optional],[coin_symbol],[coin_name],[serveic_name(1)],[service_name(2)]...
 * 
 * </pre>
 * serveic_nameは、"URLTYPE@URL"形式でURLには@を含むことができます。
 */
public class ServiceCoinCsvIo
{
	private static void importCSV(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		//CSVをインポート(値はCSVを優先)
		Logger.log("Start importCSV");
		//ファイル
		String path=ap.getString("-csv", CSV_PATH);
		ServiceUrlTable coin_url=null;
		CoinUrlIdPairTable coin_url_pair=null;
		CoinMasterTable coin_master=null;
		i_db.beginTransaction();
		try {
			coin_url_pair=new CoinUrlIdPairTable(i_db);
			coin_url=new ServiceUrlTable(i_db);
			coin_master=new CoinMasterTable(i_db);
			//ファイル読み込み
			CsvReader csv=new CsvReader(path);
			//1行目のインデックスを得る。	
			int coin_symbol_idx	=csv.getIndex(CoinMasterTable.DN_symbol);
			int coin_name_idx	=csv.getIndex(CoinMasterTable.DN_name);
			//サービス名インデクス、その個数を得る
			int service_name_idx=coin_name_idx+1;
			int number_of_service=csv.getCols()-service_name_idx;
			//2行目のインデクスを得る。
			//3行目のインデクスを得る。
			//サービスIDを検索
			ServiceUrlTable.Item[] url_item=new ServiceUrlTable.Item[number_of_service];
			{
				String[] url=new String[number_of_service];
				int[] url_type=new int[number_of_service];
				//1行目は読み飛ばし
				for(int i=0;i<number_of_service;i++){
					
				}
				csv.next();
				//2行目(TYPE)
				for(int i=0;i<number_of_service;i++){
					url_type[i]=ServiceTypeTable.getSingleton().getId(csv.getString(service_name_idx+i));
				}
				csv.next();
				//3行目
				for(int i=0;i<number_of_service;i++){
					url[i]=csv.getString(service_name_idx+i);
				}
				
				//URLテーブルからURL/TYPEキーでデータを取得
				for(int i=0;i<number_of_service;i++){
					url_item[i]=coin_url.getItemByUrlType(url[i],url_type[i]);
					if(url_item[i]==null){
						throw new SdbException();
					}
				}
			}
			
			while(csv.next())
			{
				//コインを検索(1,2列目がキー)
				CoinMasterTable.Item coin=coin_master.getItem(csv.getString(coin_symbol_idx),csv.getString(coin_name_idx));
				if(coin==null){
					throw new SdbException();
				}
				String l="";
				//コインが見つかったらurl-id
				for(int i=0;i<number_of_service;i++){
					//サービスIDとコインIDのペアを検索
					CoinUrlIdPairTable.Item pair=coin_url_pair.getItem(coin.id,url_item[i].id);
					//表の値を評価
					if(csv.getString(service_name_idx+i).matches("[yY]")){
						l+="Y";
						//有効値の場合
						if(pair==null){
							coin_url_pair.add(coin.id,url_item[i].id);
						}			
					}else{
						l+="-";
						//無効値の場合
						if(pair!=null){
							coin_url_pair.delete(coin.id,url_item[i].id);
						}
					}
				}
				Logger.log(coin.coin_symbol+":"+coin.coin_name+" "+l);
			}
			i_db.commit();
		} catch (Throwable e){
			throw new SdbException(e);
		}finally{
			i_db.endTransaction();
			if(coin_url!=null){
				coin_url.dispose();
			}
			if(coin_master!=null){
				coin_master.dispose();
			}
			if(coin_url_pair!=null){
				coin_url_pair.dispose();
			}
		}
		Logger.log("done.");
	}
	
	private static void exportCSV(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		//CSVをインポート(値はCSVを優先)
		Logger.log("Start exportCSV");
		//ファイル
		String path=ap.getString("-csv", CSV_PATH);
		String[] service_names=ap.getString("-service",null).split(",");
		
		ServiceUrlTable coin_url=null;
		CoinUrlIdPairTable coin_url_pair=null;
		CoinMasterTable coin_master=null;
		try {
			CsvWriter writer=new CsvWriter(path);
			coin_url_pair=new CoinUrlIdPairTable(i_db);
			coin_url=new ServiceUrlTable(i_db);
			coin_master=new CoinMasterTable(i_db);

			writer.writeCol(CoinMasterTable.DN_symbol);
			writer.writeCol(CoinMasterTable.DN_name);
			ServiceUrlTable.Item[] url_item=new ServiceUrlTable.Item[service_names.length];
			//列行の書き出し
			{
				//情報取得と1行目の書き出し
				for(int i=0;i<service_names.length;i++){
					//SERVICE@URL形式
					int sp=service_names[i].indexOf('@');
					if(sp<0){
						throw new SdbException();
					}
					
					//サービス情報を得る。
					url_item[i]=coin_url.getItemByUrlType(
							service_names[i].substring(sp+1),
							ServiceTypeTable.getSingleton().getId(service_names[i].substring(0,sp)));
					if(url_item[i]==null){
						throw new SdbException();
					}
					writer.writeCol(url_item[i].name);
				}
				writer.next();
				//2行目
				writer.writeCol("");
				writer.writeCol("");
				for(int i=0;i<service_names.length;i++){
					writer.writeCol(ServiceTypeTable.getSingleton().getString(url_item[i].id_coin_url_type));
				}
				writer.next();
				//3行目
				writer.writeCol("");
				writer.writeCol("");
				for(int i=0;i<service_names.length;i++){
					writer.writeCol(url_item[i].url);
				}
			}
			writer.next();
			String[] row=new String[service_names.length+2];
			RowIterable<CoinMasterTable.Item> it=coin_master.getAllAsc();
			for(CoinMasterTable.Item ci:it)
			{
				row[0]=ci.coin_symbol;
				row[1]=ci.coin_name;
				//コインペアを検索
				String l="";
				for(int i=0;i<url_item.length;i++){
					//サービスIDとコインIDのペアを検索
					CoinUrlIdPairTable.Item pair=coin_url_pair.getItem(ci.id,url_item[i].id);
					row[2+i]=pair!=null?"Y":"--";
					l+=pair!=null?"Y":"-";
				}
				Logger.log(ci.coin_symbol+":"+ci.coin_name+" "+l);
				writer.writeCol(row);
				writer.next();
				
			}
			writer.flush();
		} catch (Throwable e){
			throw new SdbException(e);
		}finally{
			if(coin_url!=null){
				coin_url.dispose();
			}
			if(coin_url_pair!=null){
				coin_url_pair.dispose();
			}
			if(coin_master!=null){
				coin_master.dispose();
			}
		}
		Logger.log("done.");
	}
	public static final String CSV_PATH="service_list.csv";	
	public static String readme()
	{
		return ServiceCoinCsvIo.class.getName()+"\n"+
		"-cmd srv_importcsv [-db DB] [-csv CSV]\n"+
		"-cmd srv_exportcsv [-db DB] -service SERVICELIST [-csv CSV]\n"+
		"	DB - sqlite3 file name. default="+NccDBAppArgHelper.ENV_NCCDB_DB_PATH+"\n"+
		"	CSV - CSV as CoinListCsv format filename. default="+CSV_PATH+"\n"+
		"	SERVICELIST - CSV style service name list";
	}
	public static boolean run(String i_cmd,NccDBAppArgHelper args) throws SdbException
	{
		if(i_cmd.compareTo("srv_importcsv")==0){
			importCSV(args,args.getNccDB());
		}else if(i_cmd.compareTo("srv_exportcsv")==0){
			exportCSV(args,args.getNccDB());
		}else{
			return false;
		}
		return true;
	}
}
