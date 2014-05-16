package jp.nyatla.nccdbtoolkit;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import jp.nyatla.nccdb.CryptCoinTankCoinListScraper;
import jp.nyatla.nccdb.table.*;
import jp.nyatla.nccdb.table.CoinMasterTable.Item;
import jp.nyatla.nccdb.table.internal.CoinAlgorismTable;
import jp.nyatla.nyansat.db.basic.RowIterable;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.utils.ArgHelper;
import jp.nyatla.nyansat.utils.CsvReader;
import jp.nyatla.nyansat.utils.CsvWriter;
import jp.nyatla.nyansat.utils.SdbException;



/**
 * CoinListCSVのIOコマンドの実装。
 * CoinListCSVは{@link CoinInfoView}、{@link CoinMasterTable}、{@link CoinSpecTable}と関連付けられます。
 * インポート時は{@link CoinMasterTable}と{@link CoinSpecTable}に値を書き込みます。
 * エクスポート時は{@link CoinInfoView}から値をダンプします。
 * <pre>
 * フォーマット
 * [id:optional],[symbol],[name],[alias_id],[start_date],[total_coin],[premine],[coin_algorism]
 * </pre>
 */
public class CoinListCsvIo
{
	/**
	 * CSVファイルの内容を重複無くテーブルに追加する。
	 * CoinSpecTableには参照されなくなったデータが溜まるので別途クリーンアッププロセスが必要だけど当分大丈夫そうだから作ってない。
	 * @param ap
	 * @param i_db
	 * @throws SdbException
	 */
	private static void importCSV(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		//CSVをインポート(値はCSVを優先)
		Logger.log("Start importCSV");
		//ファイル
		String path=ap.getString("-csv", CSV_PATH);
		CoinMasterTable coin_title=null;
		CoinSpecTable coin_spec=null;
		i_db.beginTransaction();
		try {
			coin_title=new CoinMasterTable(i_db);
			coin_spec=new CoinSpecTable(i_db);
			//ファイル読み込み
			CsvReader csv=new CsvReader(path);
			//列インデックスを得る。
			int coin_symbol_idx		=csv.getIndex(CoinMasterTable.DN_symbol);
			int coin_name_idx		=csv.getIndex(CoinMasterTable.DN_name);
			int coin_alias_idx		=csv.getIndex(CoinMasterTable.DN_alias_id);
			int coin_start_date_idx	=csv.getIndex(CoinMasterTable.DN_start_date);
			int spec_total_idx		=csv.getIndex(CoinSpecTable.DN_total_coin);
			int spec_premine_idx	=csv.getIndex(CoinSpecTable.DN_premine);
			int coin_algorism_idx	=csv.getIndex(CoinSpecTable.DN_id_algorism);
			while(csv.next())
			{
				String symbol			=csv.getString(coin_symbol_idx);
				String name				=csv.getString(coin_name_idx);
				Integer alias			=csv.getInteger(coin_alias_idx,null);
				Long start_date			=csv.getDate(coin_start_date_idx,null);

				Double spec_total		=csv.getDouble(spec_total_idx,null);
				Double spec_premine		=csv.getDouble(spec_premine_idx,null);
				int spec_algorism		=CoinAlgorismTable.getSingleton().getId(csv.getString((coin_algorism_idx)),CoinAlgorismTable.UNKNOWN);
				
				Logger.log_start_line();
				Logger.log("["+symbol+":"+name+"]");
				//コインマスタを検索
				CoinMasterTable.Item it=coin_title.getItem(symbol,name);
				//コインスペックを検索
				CoinSpecTable.Item st;
				if(it==null){
					st=coin_spec.getItem(spec_total, spec_premine, spec_algorism);
				}else{
					st=coin_spec.getItem(it.spec_id);
				}
				//実名かチェック
				if(alias!=null){
					Logger.log("ALIAS=yes,");					
				}else{
					Logger.log("ALIAS=no,");
					////実名の場合
					if(st==null){
						////同一スペックが存在しない場合
						if(!coin_spec.add(spec_total, spec_premine, spec_algorism)){
							throw new SdbException();
						}
						//st再取得
						st=coin_spec.getItem(spec_total, spec_premine, spec_algorism);
						if(st==null){
							throw new SdbException();
						}
						Logger.log("SPEC add="+st.id+",");
					}else if(!st.match(spec_total,spec_premine,spec_algorism)){
						////IDに記述されているスペックと異なる場合
						st=coin_spec.getItem(spec_total, spec_premine, spec_algorism);
						if(st==null){
							////stを再検索して発見できなければ追加
							if(!coin_spec.add(spec_total, spec_premine, spec_algorism)){
								throw new SdbException();
							}
							//st再取得
							st=coin_spec.getItem(spec_total, spec_premine, spec_algorism);
							if(st==null){
								throw new SdbException();
							}
							Logger.log("SPEC add="+st.id+",");
						}else{
							Logger.log("SPEC change="+st.id+",");
						}
					}else{
						Logger.log("SPEC found="+st.id+",");
					}
				}
				//マスタの上書き
				if(!coin_title.update(symbol,name,alias,start_date, st.id)){
					throw new SdbException();
				}
				Logger.log("MASTER update");
				Logger.log_end_line();
			}
			i_db.commit();
		} catch (IOException e) {
			throw new SdbException(e);			
		}finally{
			i_db.endTransaction();
			if(coin_title!=null){
				coin_title.dispose();
			}
			if(coin_spec!=null){
				coin_spec.dispose();
			}
		}
		////省略：未参照Specのチェック
		Logger.log("done.");
	}
	private static void exportCSV(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		Logger.log("Start exportCSV");
		//ファイル
		String path=ap.getString("-csv", CSV_PATH);
		//CSVをエクスポート
		CoinInfoView civ=null;
		try{
			CsvWriter writer=new CsvWriter(path);
			civ=new CoinInfoView(i_db);
			writer.writeCol(civ.getColHeader());
			writer.next();
			RowIterable<CoinInfoView.Item> it=civ.getAllAsc();
			int p=0;
			for(CoinInfoView.Item i :it){
				Logger.log(p+":"+i.symbol+" "+i.name);
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
	/**
	 * CryptcoinTankからコインリストを取り込む。
	 * @param ap
	 * @param i_db
	 * @throws SdbException
	 */
	private static void syncryptCoinList(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		Logger.log("Start syncryptCoinList");
		CoinMasterTable ctt=null;
		CoinSpecTable cst=null;
		//URLリストの構築
		String[] url_list=UrlData.CC_URL_LIST;
		{
			String l=ap.getString("-url", null);
			if(l!=null){
				url_list=l.split(",");
			}
		}
		//tableオープン
		i_db.beginTransaction();
		try{
			ctt=new CoinMasterTable(i_db);
			cst=new CoinSpecTable(i_db);
			//nullスペックのidを取得
			CoinSpecTable.Item csti=cst.getItem(null,null,CoinAlgorismTable.UNKNOWN);
			if(csti==null){
				if(!cst.add(null,null,CoinAlgorismTable.UNKNOWN)){
					throw new SdbException();
				}
				csti=cst.getItem(null,null,CoinAlgorismTable.UNKNOWN);
				if(csti==null){
					throw new SdbException();
				}
			}
			
			CryptCoinTankCoinListScraper scp=new CryptCoinTankCoinListScraper(ap.getString("-ua",null),ap.getString("-cookie",null));
			//CoinListの取得
			for(int i2=0;i2<url_list.length;i2++){
				Logger.log("Scan URL="+url_list[i2]);
				ArrayList<CryptCoinTankCoinListScraper.Item> items=scp.parse(url_list[i2]);
				if(items==null){
					throw new SdbException();
				}
				//テーブルを追記
				for(CryptCoinTankCoinListScraper.Item i:items){
					boolean is_cct=ctt.add(i.symbol,i.name,null,null,csti.id);
					Logger.log("["+(is_cct?"ADD":"DROP")+"]"+i.symbol+":"+i.name);
				}
			}
			i_db.commit();			
		}finally{
			i_db.endTransaction();			
			if(ctt!=null){
				ctt.dispose();
			}
			if(cst!=null){
				cst.dispose();
			}
		}
		Logger.log("done.");
		
	}
	public static void initDB(NccDBAppArgHelper ap) throws SdbException
	{
		Logger.log("Setup table and view of database.");
		SqliteDB db=ap.getNccDB();
		CoinMasterTable ctt=null;
		CoinSpecTable cst=null;
		CoinInfoView civ=null;
		ServiceUrlTable cut=null;
		//tableオープン
		try{
			ctt=new CoinMasterTable(db);
			cst=new CoinSpecTable(db);
			civ=new CoinInfoView(db);
			cut=new ServiceUrlTable(db);
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
			if(cut!=null){
				cut.dispose();
			}
		}
		Logger.log("done.");
	}
	
	public static void refleshDB(NccDBAppArgHelper ap) throws SdbException
	{
		SqliteDB db=ap.getNccDB();
		CoinMasterTable cmt=new CoinMasterTable(db);
		CoinSpecTable cst=new CoinSpecTable(db);
		CoinUrlIdPairTable cpair=new CoinUrlIdPairTable(db);
		db.beginTransaction();
		try{
			//コインIDの多重エイリアスを解決
			{
				Logger.log("Reflesh alias_id");
				int nor=0;
				//alias_idのあるものを選択
				RowIterable<CoinMasterTable.Item> m=cmt.getAliasItems();
				for(CoinMasterTable.Item i :m){
					CoinMasterTable.Item bi=cmt.getRootItemByAlias(i.alias_id);
					if(bi==null){
						//アカン
						Logger.log(String.format("Error: id=%d has not root ID.",i.alias_id));
						throw new SdbException();
					}
					//IDの更新
					if((int)i.alias_id!=(int)bi.id){
						Logger.log(String.format("Update %s:%s alias_id %d->%d [%s]",i.coin_symbol,i.coin_name,i.alias_id,bi.id,cmt.updateAliasId(i.coin_symbol,i.coin_name,bi.id)?"OK":"ERROR"));
					}else{
						Logger.log(String.format("Valid %s:%s",i.coin_symbol,i.coin_name));
					}
					nor++;
				}
				m.dispose();
				Logger.log(String.format("%d data processed. done.",nor));
			}
			//コインスペックの未参照問題を解決
			{
				Logger.log("Reflesh spec_table.");
				RowIterable<CoinSpecTable.Item> s=cst.getAll();
				int nor=0;
				for(CoinSpecTable.Item i :s){
					
					if(!cmt.isExistSpec(i.id)){
						Logger.log(String.format("ID=%d has not reference ID. delete=%s",i.id,cst.deleteItem(i.id)?"OK":"NG"));
					}
					nor++;
				}
				s.dispose();
				Logger.log(String.format("%d data processed. done.",nor));
			}
			//URLのALIASID問題を解消
			{
				Logger.log("Reflesh url_pair alias_id problem.");
				int nor=0;
				//alias_idのあるものを選択
				RowIterable<CoinMasterTable.Item> m=cmt.getAliasItems();
				for(CoinMasterTable.Item i :m)
				{
					CoinMasterTable.Item bi=cmt.getRootItemByAlias(i.alias_id);
					if(bi==null){
						//処理できぬ
						continue;
					}
					int root_id=bi.id;
					//Alias_idを持ってるURL-Pair行を検索
					RowIterable<CoinUrlIdPairTable.Item> pm=cpair.getItemsByCoinId(i.id);
					int del=0;
					for(CoinUrlIdPairTable.Item p:pm){
						del++;
						nor++;
						//Alias idにリンクした項目を削除
						if(!cpair.delete(p.id1,p.id2)){
							Logger.log(String.format("Error! Delete id=%d:%d",p.id1,p.id2));
							throw new SdbException();
						}
						//aliasのroot-idとurl_idで検索して同盟のがある？
						if(!cpair.isExistItem(root_id,p.id2)){
							//ルートidとURLIDペアが無い場合は追加
							if(!cpair.add(root_id,p.id2)){
								Logger.log(String.format("Error! Add id=%d:%d",root_id,p.id2));
								throw new SdbException();
							}
						}
					}
					if(del>0){
						Logger.log(String.format("Alias nesting is solved. %s:%s",i.coin_symbol,i.coin_name));
					}
				}
				m.dispose();
				Logger.log(String.format("%d data processed. done.",nor));			
			}
			//URLペアの未参照問題を解決(1:未参照コインID)
			{
				Logger.log("delete unreferenced coin_id in url_pair.");
				int ret=cpair.deleteNoReferencedRowByCoinId();
				Logger.log(String.format("%d data deleted. done.",ret));			
				//省略
			}
			//URLペアの未参照問題を解決(2:未参照URL)
			{
				Logger.log("delete unreferenced url_id in uri_pair.");
				int ret=cpair.deleteNoReferencedRowByUriId();
				Logger.log(String.format("%d data deleted. done.",ret));			
			}
			//未参照のURLを削除
			{
				//省略
			}
			db.commit();
		}finally{
			db.endTransaction();
		}
	}
	
	
	public static final String CSV_PATH="coinspec.csv";
	
	public static String readme()
	{
		return
			CoinListCsvIo.class.getName()+"\n"+
			"-cmd coin_importcsv [-db DB] [-csv CSV]\n"+
			"-cmd coin_exportcsv [-db DB] [-csv CSV]\n"+
			"-cmd coin_addlist [-db DB] [-u UA] [-url URL] [-cookie COOKIE]\n"+
			"	DB - sqlite3 file name. default="+NccDBAppArgHelper.ENV_NCCDB_DB_PATH+"\n"+
			"	CSV - CSV as CoinListCsv format filename. default="+CSV_PATH+"\n"+
			"	UA - User agent parametor for http get.\n"+
			"	URL - CSV style URL list of cryptocointalk.com thread. default=set of cryptocointalk.com thread.\n"+
			"	COOKIE - Cookie parametor for http get."+
			"-cmd reflesh [-db DB]\n";
	}
	public static boolean run(String i_cmd,NccDBAppArgHelper args) throws SdbException
	{
		if(i_cmd.compareTo("init")==0){
			initDB(args);
		}else if(i_cmd.compareTo("coin_addlist")==0){
			syncryptCoinList(args,args.getNccDB());
		}else if(i_cmd.compareTo("coin_exportcsv")==0){
			exportCSV(args,args.getNccDB());
		}else if(i_cmd.compareTo("coin_importcsv")==0){
			importCSV(args,args.getNccDB());
		}else if(i_cmd.compareTo("reflesh")==0){
			refleshDB(args);
		}else{
			return false;
		}
		return true;
	}
}
