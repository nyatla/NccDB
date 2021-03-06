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

import jp.nyatla.nccdb.BitCoinTankCoinListScraper;
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


public class BctHtmlCache
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
//		String ua=ap.getString("-ua",null);
//		String cookie=ap.getString("-cookie",null);
		
		//URLリストの構築

		long today=(new Date()).getTime();
		//tableオープン
		HtmlCacheTable hct=new HtmlCacheTable(cache_db,HtmlCacheTable.NAME);
		CoinSourceUrlTable csu=new CoinSourceUrlTable(cache_db,CoinSourceUrlTable.NAME);

		db.beginTransaction();
		try{
			BasicHttpClient httpcl=new BasicHttpClient();
//			httpcl.setSession(ua, cookie);
			
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
					pbl=new CctThreadLinkList(Jsoup.parse(row.html));
				}else{
					continue;
				}

				//コインを選択
				CoinMasterTable.Item coin=coin_master.getItem(row.symbol,row.name);
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
	private static void scrapeFromBctThread(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		Logger.log("Start name,symbol,url scraping from BitcoinTalk Coin thread");
		CoinMasterTable ctt=null;
		//URLリストの構築
		String target_url=ap.getString("-url",UrlData.BC_URL_LIST);
		int nonn_max=(int)ap.getLong("-page_max",10);

		//?prune_day=100&sort_by=Z-A&sort_key=last_post&topicfilter=all
		CoinSourceUrlTable csu=new CoinSourceUrlTable(i_db,CoinSourceUrlTable.NAME);
		//tableオープン
//		HtmlCacheTable hct=new HtmlCacheTable(i_db,HtmlCacheTable.NAME);
		i_db.beginTransaction();
		try{
			int num_of_no_new=0;
			BitCoinTankCoinListScraper scp=new BitCoinTankCoinListScraper("","");
			//CoinListの取得
			for(int i2=0;;i2++){
				int number_of_add=0;
				String url=target_url+String.format(".%d;sort=last_post;desc",i2*40);
				Logger.log("Scan URL="+url);
				ArrayList<BitCoinTankCoinListScraper.Item> items=scp.parse(url);
				if(items==null){
					break;
				}
				for(int i3=0;i3<items.size();i3++){
					if(csu.isExistByUrl(items.get(i3).href)){
						Logger.log("[EXIST]"+items.get(i3).href);
						continue;
					}
					if(csu.add(CoinSourceUrlTable.DOMAIN_BCT,items.get(i3).href)){
						Logger.log("[ADD]"+items.get(i3).href);
						number_of_add++;
					}else{
						Logger.log("[EXIST]"+items.get(i3).href);
					}
				}
				if(number_of_add==0){
					num_of_no_new++;
					if(num_of_no_new>nonn_max){
						break;
					}
				}
				if(scp.getMaxThread()==i2){
					break;
				}
				//wait
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
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

	public static String readme()
	{
		return
			CoinListCsvIo.class.getName()+"\n"+
			"-cmd bct_html_cache [-cct_db CCTDB] [-u UA] [-url COIN_LIST_URL] [-cookie COOKIE]\n"+
			" BCTDBコインシンボルテーブルから有効なURLのみをキャッシュします。"+
			"-cmd bct_import_urls [-cct_db CCTDB] [-cct_db DB] [-csv CSV]\n"+
			"　CachDBのHTMLキャッシュからURLを抽出してコイン名と共にNccDBへ登録します。\n"+
			"-cmd bct_scrape_url_from_thread [-cct_db CCTDB] [-u UA] [-url CCT_THREAD_URL] [-cookie COOKIE]\n"+
			"　BitcoinTankのスレッドを巡回してURLをキャッシュDBへ登録します。\nURLはビットコインTalkのフォーラムスレッドである必要があります。\n"+
			"　https://bitcointalk.org/index.php?board=159 (.XXは除外すること)"+
			
			"	DB - sqlite3 file name. default="+NccDBAppArgHelper.ENV_NCCDB_DB_PATH+"\n"+
			"	CCTDB - sqlite3 file name. default="+NccDBAppArgHelper.ENV_HTML_CACHE_DB_PATH+"\n"+
			"	UA - HTTPリクエストのユーザエージェント\n"+
			"	COOKIE - HTTPリクエストに使うCookie値"+
			"	CCT_COIN_LIST_URL - CryptocoinTalk.comのコインリストスレッドリスト。CSVスタイル\n"+
			"	CCT_THREAD_URL - CryptocoinTalk.comのスレッド";
	}	
	public static boolean run(String i_cmd,NccDBAppArgHelper args) throws SdbException
	{
		if(i_cmd.compareTo("bct_html_cache")==0){
			//スレッドからHTMLをCCT DBへキャッシュ
			getCryptocoinTalkHtmlCache(args);
		}else if(i_cmd.compareTo("bct_import_urls")==0){
			//HTMLキャッシュからコインペアへサービスURLリストをCCT DBへインポート
			importUrls(args);
		}else if(i_cmd.compareTo("bct_scrape_url_from_thread")==0){
			//CryptCoinTankスレッドからURLをスクレイピング
			scrapeFromBctThread(args,args.getHtmlCache());
		}else{
			return false;
		}
		return true;
	}
}
