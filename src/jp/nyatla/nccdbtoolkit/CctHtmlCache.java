package jp.nyatla.nccdbtoolkit;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import jp.nyatla.nccdb.CryptCoinTankCoinListScraper;
import jp.nyatla.nccdb.CryptCoinTankCoinListScraper.Item;
import jp.nyatla.nccdb.table.CoinBaseHtmlCacheView;
import jp.nyatla.nccdb.table.CoinMasterTable;
import jp.nyatla.nccdb.table.CoinSourceUrlTable;
import jp.nyatla.nccdb.table.CoinSpecTable;
import jp.nyatla.nccdb.table.CoinUrlIdPairTable;
import jp.nyatla.nccdb.table.HtmlCacheTable;
import jp.nyatla.nccdb.table.IdPairTable;
import jp.nyatla.nccdb.table.ServiceUrlTable;
import jp.nyatla.nccdb.table.internal.CoinAlgorismTable;
import jp.nyatla.nccdb.table.internal.ServiceTypeTable;
import jp.nyatla.nyansat.db.basic.RowIterable;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.utils.ArgHelper;
import jp.nyatla.nyansat.utils.BasicHttpClient;
import jp.nyatla.nyansat.utils.SdbException;


public class CctHtmlCache
{
	
	/**
	 * CryptcoinTankからコイン情報スレッドをキャッシュする。
	 * キャッシュキーは"Symbol:Name:1"
	 * @param ap
	 * @param i_db
	 * @throws SdbException
	 */
	private static void getCryptocoinTalkHtmlCache(NccDBAppArgHelper ap) throws SdbException
	{
		Logger.log("Start getInformationCoinThread");
		SqliteDB cache_db=ap.getHtmlCache();
		SqliteDB db=ap.getNccDB();		
		CoinMasterTable ctt=null;
		String ua=ap.getString("-ua",null);
		String cookie=ap.getString("-cookie",null);
		
		//URLリストの構築

		long today=(new Date()).getTime();
		//tableオープン
		HtmlCacheTable hct=new HtmlCacheTable(cache_db,HtmlCacheTable.NAME);
		CoinSourceUrlTable csu=new CoinSourceUrlTable(cache_db,CoinSourceUrlTable.NAME);

		db.beginTransaction();
		try{
			BasicHttpClient httpcl=new BasicHttpClient();
			httpcl.setSession(ua, cookie);
			
			RowIterable<CoinSourceUrlTable.Item> b=csu.getAll();
			//CoinListの取得
			for(CoinSourceUrlTable.Item i:b){
				if(i.symbol==null || i.name==null || i.symbol.charAt(0)=='*'){
					Logger.log("[IGNORE]");
					continue;
				}
//				String key_text=i.symbol+":"+i.name+":"+i.domain;
				if(hct.isExist(i.url))
				{
					Logger.log("[EXIST]"+i.url);
					continue;
				}
				Document d=httpcl.httpGet(i.url);
				if(d==null){
					Logger.log("[ERROR]"+i.url);
					continue;
				}
				//保存
				boolean is_cct=hct.add(today,i.url,d.toString());
				Logger.log("["+(is_cct?"ADD":"DROP")+"]"+i.url);
				//ちょっと待つ
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e){
					throw new SdbException(e);
				}
			}
			db.commit();			
		}finally{
			db.endTransaction();			
			if(ctt!=null){
				ctt.dispose();
			}
			if(hct!=null){
				hct.dispose();
			}
		}
		Logger.log("done.");
	}

	/**
	 * コマンド関数。HtmlCacheTableに格納される全てのHTMLを解析し、キーのSymbol:Nameに従ってコインのサービスURLを登録する。
	 * 
	 * @param ap
	 * @throws SdbException
	 */
	private static void importUrls(NccDBAppArgHelper ap) throws SdbException
	{
		Logger.log("Start extractCoinUrl");
		CoinMasterTable ctt=null;
		//tableオープン
		SqliteDB cache_db=ap.getHtmlCache();
		SqliteDB db=ap.getNccDB();
		CoinBaseHtmlCacheView hct=new CoinBaseHtmlCacheView(cache_db,CoinBaseHtmlCacheView.NAME);
		RowIterable<CoinBaseHtmlCacheView.Item> rows=hct.getAll();
		CoinMasterTable coin_master=new CoinMasterTable(db);
		ServiceUrlTable service_url=new ServiceUrlTable(db);
		CoinUrlIdPairTable id_pair=new CoinUrlIdPairTable(db);
		try{
			db.beginTransaction();
			//全行取得
			for(CoinBaseHtmlCacheView.Item row : rows){
				//コインシンボル、コイン名を取得
				
				//HTMLを解析してURLリストを生成
				ArrayList<String[]> pbl;
				if(row.domain==CoinSourceUrlTable.DOMAIN_CCT){
					//CCTドメインの場合
					pbl=new CctLinkListParser(Jsoup.parse(row.html));
				}else{
					continue;
				}

				//コインを選択(ルートノードのみ)
				CoinMasterTable.Item coin=coin_master.getRootItem(row.symbol,row.name);
				if(coin==null){
					//コイン見つからない。
					Logger.log(String.format("[ERROR]%s:%s:coin not found",row.symbol,row.name));
					continue;
				}
				//登録
				for(String[] s:pbl){
					String url=s[2];
					int url_type=ServiceTypeTable.getSingleton().getId(s[0]);
					//URLをキーに選択してnameを取得
					ServiceUrlTable.Item url_item=service_url.getItemByUrlType(url,url_type);
					//URLをignore existで追加
					boolean is_add_url=service_url.update(
						url_item==null?s[1]:url_item.name,	//name
						url_type,	//id_type
						0,		//status
						s[2],	//url
						null);	//description
					//URLを選択しなおす。
					ServiceUrlTable.Item selected_url=service_url.getItemByUrlType(s[2],url_type);
					if(selected_url==null){
						throw new SdbException();
					}
					//コインID/URLを追加
					boolean is_add_id_pair=id_pair.add(coin.id,selected_url.id);
					//ログ
					Logger.log(String.format("[OK]%s:%s:url=%s,id_pair=%s",row.symbol,row.name,is_add_url?"update":"error",is_add_id_pair?"add":"exist"));
				}
			}
			db.commit();			
		}finally{
			db.endTransaction();			
			if(ctt!=null){
				ctt.dispose();
			}
			if(hct!=null){
				hct.dispose();
			}
		}
		Logger.log("done.");
	}
	private static void scrapeFromCoinList(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		Logger.log("Start name,symbol,url scraping from CryptocoinTalk Coin thread");
		CoinMasterTable ctt=null;
		//URLリストの構築
		String[] url_list=UrlData.CC_URL_LIST;
		{
			String l=ap.getString("-url", null);
			if(l!=null){
				url_list=l.split(",");
			}
		}
		CoinSourceUrlTable csu=new CoinSourceUrlTable(i_db,CoinSourceUrlTable.NAME);
		//tableオープン
//		HtmlCacheTable hct=new HtmlCacheTable(i_db,HtmlCacheTable.NAME);
		i_db.beginTransaction();
		try{
			String ua=ap.getString("-ua",null);
			String cookie=ap.getString("-cookie",null);
			CryptCoinTankCoinListScraper scp=new CryptCoinTankCoinListScraper(ua,cookie);
			//CoinListの取得
			for(int i2=0;i2<url_list.length;i2++){
				Logger.log("Scan URL="+url_list[i2]);
				ArrayList<CryptCoinTankCoinListScraper.Item> items=scp.parse(url_list[i2]);
				if(items==null){
					throw new SdbException();
				}
				//テーブルを追記
				BasicHttpClient httpcl=new BasicHttpClient();
				httpcl.setSession(ua,cookie);
				L1:for(CryptCoinTankCoinListScraper.Item i:items){
					//既にデータベースにあるコインコードなら無視
					if(csu.isExist(i.symbol,i.name,CoinSourceUrlTable.DOMAIN_CCT))
					{
						Logger.log("[EXIST]"+i.symbol+":"+i.name);
						continue;
					}
					//
					Document d=httpcl.httpGet(i.href);
					if(d==null){
						Logger.log("ERROR:"+i.href);
					}					
					try {
						Thread.sleep(500);
					} catch (InterruptedException e){
					}
					//Information Threadを探す
					Element el_content=d.getElementById("forum_table");
					Elements el_tds=el_content.select("h4 a");
					for(int i3=0;i3<el_tds.size();i3++){
						String s=el_tds.get(i3).text();
						String h=el_tds.get(i3).attr("href");
						//入れ子リストは無視
						if(!s.trim().matches(".+Information( .+)?$")){
							continue;
						}
						if(csu.add(i.symbol,i.name,CoinSourceUrlTable.DOMAIN_CCT,h)){
							Logger.log("[ADD]"+i.symbol+":"+i.name);
						}else{
							Logger.log("[ERROR]"+i.symbol+":"+i.name);
						}
						continue L1;
					}
					if(csu.add(i.symbol,i.name,CoinSourceUrlTable.DOMAIN_CCT,null)){
						Logger.log("[PARTIAL]"+i.symbol+":"+i.name);
					}else{
						Logger.log("[ERROR]"+i.symbol+":"+i.name);
					}
				}
			}
			i_db.commit();			
		}finally{
			i_db.endTransaction();			
			if(ctt!=null){
				ctt.dispose();
			}
			if(csu!=null){
				csu.dispose();
			}
		}
		Logger.log("done.");
	}
	public static String encodePath(String i_cct_path) throws SdbException
	{
		int p=i_cct_path.indexOf("/topic/");
		String ret;
		try {
			ret = i_cct_path.substring(0,p)+"/topic/"+URLEncoder.encode(i_cct_path.substring(p+7,i_cct_path.length()-1),"UTF-8")+"/";
		} catch (UnsupportedEncodingException e) {
			throw new SdbException(e);
		}
		return ret;
	}
	private static void scrapeFromCctThread(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		Logger.log("Start name,symbol,url scraping from CryptocoinTalk Coin thread");
		CoinMasterTable ctt=null;
		//URLリストの構築
		String target_url=ap.getString("-url",UrlData.CC_ANNOUNCEMENT_URL);

		//?prune_day=100&sort_by=Z-A&sort_key=last_post&topicfilter=all
		CoinSourceUrlTable csu=new CoinSourceUrlTable(i_db,CoinSourceUrlTable.NAME);
		//tableオープン
//		HtmlCacheTable hct=new HtmlCacheTable(i_db,HtmlCacheTable.NAME);
		i_db.beginTransaction();
		try{
			String ua=ap.getString("-ua",null);
			String cookie=ap.getString("-cookie",null);
			CryptCoinTankCoinListScraper scp=new CryptCoinTankCoinListScraper(ua,cookie);
			//CoinListの取得
			for(int i2=0;;i2++){
				int number_of_add=0;
				String url=target_url+String.format("%s?prune_day=100&sort_by=Z-A&sort_key=last_post&topicfilter=all",(i2==0)?"":"page-"+(i2+1));
				Logger.log("Scan URL="+url);
				ArrayList<CryptCoinTankCoinListScraper.Item> items=scp.parse(url);
				if(items==null){
					break;
				}
				for(int i3=0;i3<items.size();i3++){
					if(csu.isExistByUrl(items.get(i3).href)){
						Logger.log("[EXIST]"+items.get(i3).href);
						continue;
					}
//					String encoded_href=encodePath(items.get(i3).href);
//					if(csu.add(CoinSourceUrlTable.DOMAIN_CCT,encoded_href)){
					if(csu.add(CoinSourceUrlTable.DOMAIN_CCT,items.get(i3).href)){
						Logger.log("[ADD]"+items.get(i3).href);
						number_of_add++;
					}else{
						Logger.log("[EXIST]"+items.get(i3).href);
					}
				}
				if(number_of_add==0){
					break;
				}
			}
			i_db.commit();
		}finally{
			i_db.endTransaction();			
			if(ctt!=null){
				ctt.dispose();
			}
			if(csu!=null){
				csu.dispose();
			}
		}
		Logger.log("done.");
	}

	/**
	 * SourceURLからCoinMasterへ存在しないコインをインポートする。
	 * @param ap
	 * @param i_db
	 * @throws SdbException
	 */
	private static void importCoinList(NccDBAppArgHelper ap) throws SdbException
	{
		Logger.log("Start syncryptCoinList");
		CoinMasterTable ctt=null;
		CoinSourceUrlTable csu=null;
		SqliteDB cache_db=ap.getHtmlCache();
		SqliteDB ncc_db=ap.getNccDB();
		//tableオープン
		ncc_db.beginTransaction();
		int num_of_add=0;
		try{
			ctt=new CoinMasterTable(ncc_db);
			csu=new CoinSourceUrlTable(cache_db,CoinSourceUrlTable.NAME);
			//元データを全行取得
			RowIterable<CoinSourceUrlTable.Item> csti=csu.getAll();
			for (CoinSourceUrlTable.Item i:csti)
			{
				if(i.symbol==null || i.name==null || i.symbol.charAt(0)=='*'){
					Logger.log("[IGNORE]");
					continue;
				}
				CoinMasterTable.Item it=ctt.getItem(i.symbol,i.name);
				if(it!=null){
					Logger.log("[EXISTS]"+i.symbol+":"+i.name);
					continue;
				}
				//add
				if(ctt.add(i.symbol, i.name,null,null,null)){
					Logger.log("[ADD]"+i.symbol+":"+i.name+" "+i.url);
				}else{
					Logger.log("[ERROR]"+i.symbol+":"+i.name);
				}
				num_of_add++;
			}
			ncc_db.commit();			
		}finally{
			ncc_db.endTransaction();			
			if(ctt!=null){
				ctt.dispose();
			}
			if(csu!=null){
				csu.dispose();
			}
		}
		Logger.log(num_of_add+" data add.\ndone.");
		
	}	
	public static String readme()
	{
		return
			CoinListCsvIo.class.getName()+"\n"+
			"-cmd cct_html_cache [-cct_db CCTDB] [-u UA] [-url COIN_LIST_URL] [-cookie COOKIE]\n"+
			" CCTDBコインシンボルテーブルから有効なURLのみをキャッシュします。"+
			"-cmd cct_import_urls [-cct_db CCTDB] [-cct_db DB] [-csv CSV]\n"+
			"　CCTDBのHTMLキャッシュからURLを抽出してコイン名と共にNccDBへ登録します。\n"+
			"-cmd cct_import_coinlist [-cct_db DB]  [-db DB]"+
			" CCTDBのコインシンボルテーブルからコイン名とシンボルをNccDBへ登録します。\n"+
			"-cmd cct_scrape_url_from_list_thread [-cct_db CCTDB] [-u UA] [-url CCT_COIN_LIST_URL] [-cookie COOKIE]\n"+
			"　CryptoCoinTankのCoinListスレッド(s)を巡回してURLとコイン名をCCTDBのコインシンボルテーブルへ登録します。\n"+
			"-cmd cct_scrape_url_from_thread [-cct_db CCTDB] [-u UA] [-url CCT_THREAD_URL] [-cookie COOKIE]\n"+
			"　CryptoCoinTankのスレッドを日付順で巡回してURLをCCTDBのコインシンボルテーブルへ登録します。\n"+
			"	DB - sqlite3 file name. default="+NccDBAppArgHelper.ENV_NCCDB_DB_PATH+"\n"+
			"	CCTDB - sqlite3 file name. default="+NccDBAppArgHelper.ENV_HTML_CACHE_DB_PATH+"\n"+
			"	UA - HTTPリクエストのユーザエージェント\n"+
			"	COOKIE - HTTPリクエストに使うCookie値"+
			"	CCT_COIN_LIST_URL - CryptocoinTalk.comのコインリストスレッドリスト。CSVスタイル\n"+
			"	CCT_THREAD_URL - CryptocoinTalk.comのスレッド";
	}	
	public static boolean run(String i_cmd,NccDBAppArgHelper args) throws SdbException
	{
		if(i_cmd.compareTo("cct_html_cache")==0){
			//スレッドからHTMLをCCT DBへキャッシュ
			getCryptocoinTalkHtmlCache(args);
		}else if(i_cmd.compareTo("cct_import_urls")==0){
			//HTMLキャッシュからコインペアへサービスURLリストをCCT DBへインポート
			importUrls(args);
		}else if(i_cmd.compareTo("cct_import_coinlist")==0){
			importCoinList(args);
		}else if(i_cmd.compareTo("cct_scrape_url_from_list_thread")==0){
			//コインリストからコインソースURLテーブルへデータをスクレイプ
			scrapeFromCoinList(args,args.getHtmlCache());
		}else if(i_cmd.compareTo("cct_scrape_url_from_thread")==0){
			//CryptCoinTankスレッドからURLをスクレイピング
			scrapeFromCctThread(args,args.getHtmlCache());
		}else{
			return false;
		}
		return true;
	}
}
