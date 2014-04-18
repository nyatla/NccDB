import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import jp.nyatla.nccdb.CryptCoinTankCoinListScraper;
import jp.nyatla.nccdb.CryptCoinTankCoinListScraper.Item;
import jp.nyatla.nccdb.table.CoinMasterTable;
import jp.nyatla.nccdb.table.CoinSourceUrlTable;
import jp.nyatla.nccdb.table.CoinSpecTable;
import jp.nyatla.nccdb.table.CoinUrlIdPairTable;
import jp.nyatla.nccdb.table.HtmlCacheTable;
import jp.nyatla.nccdb.table.IdPairTable;
import jp.nyatla.nccdb.table.ServiceUrlTable;
import jp.nyatla.nccdb.table.internal.CoinAlgorismTable;
import jp.nyatla.nccdb.table.internal.ServiceTypeTable;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.utils.ArgHelper;
import jp.nyatla.nyansat.utils.BasicHttpClient;
import jp.nyatla.nyansat.utils.SdbException;


public class CctHtmlCache
{
	
	/**
	 * CryptcoinTankからコイン情報スレッドを取り込む。
	 * @param ap
	 * @param i_db
	 * @throws SdbException
	 */
	private static void getInformationCoinThread(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		Logger.log("Start getInformationCoinThread");
		CoinMasterTable ctt=null;
		//URLリストの構築
		String[] url_list=UrlData.CC_URL_LIST;
		{
			String l=ap.getString("url", null);
			if(l!=null){
				url_list=l.split(",");
			}
		}
		long today=(new Date()).getTime();
		//tableオープン
		HtmlCacheTable hct=new HtmlCacheTable(i_db,HtmlCacheTable.NAME);
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
				for(CryptCoinTankCoinListScraper.Item i:items){
					//既にデータベースにあるコインコードなら無視
					if(hct.isExist(i.symbol+":"+i.name))
					{
						Logger.log("[EXIST]"+i.symbol+":"+i.name);
						continue;
					}
					//
					Document d=httpcl.httpGet(i.href);
					//Information Threadを探す
					Element el_content=d.getElementById("forum_table");
					Elements el_tds=el_content.select("h4 a");
					boolean found=false;
					for(int i3=0;i3<el_tds.size();i3++){
						String s=el_tds.get(i3).text();
						String h=el_tds.get(i3).attr("href");
						//入れ子リストは無視
						if(!s.trim().matches(".+Information( .+)?$")){
							continue;
						}
						//コインページの→HTMLを得る
						d=httpcl.httpGet(h);
						//保存
						boolean is_cct=hct.add(today,i.symbol+":"+i.name,d.toString());
						Logger.log("["+(is_cct?"ADD":"DROP")+"]"+i.symbol+":"+i.name);
						found=true;
						//ちょっと待つ
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e){
							throw new SdbException(e);
						}
						break;
					}
					if(!found){
						Logger.log("[ERROR]"+i.symbol+":"+i.name+"is not found!");
					}
				}
			}
			i_db.commit();			
		}finally{
			i_db.endTransaction();			
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
	 * タグツリーからStrong値とURL解析結果を格納するArrayList。
	 * 各行の内容は以下の通り
	 * s[0]:正規化カテゴリ
	 * s[1]:生カテゴリ
	 * s[2]:正規化URL
	 * 
	 */
	@SuppressWarnings("serial")
	public static class CctThreadLinkList extends ArrayList<String[]>
	{
		/**
		 * 目標のタグエレメントを探す
		 * @param i_node
		 * @return
		 */
		private Element searchContentTag(Element i_node)
		{
			if(
				i_node.tagName().compareToIgnoreCase("div")==0 &&
				i_node.attr("class").trim().compareTo("post entry-content")==0 &&
				i_node.attr("itemprop").trim().compareTo("commentText")==0
			){
				return i_node;
			}else{
				//階層探査
				Elements els=i_node.children();
				for(Element el:els){
					Element ret=this.searchContentTag(el);
					if(ret!=null){
						return ret;
					}
				}
			}
			return null;
		}		
		private static final String DICTIONARY_FILE="./url_normalize.dat";
		private String _current_strong;
		private RegExpKeyTable _regexp_table;

		private CctThreadLinkList(Document i_doc) throws SdbException
		{
			super();
			try {
				this._regexp_table=new RegExpKeyTable(DICTIONARY_FILE);
			} catch (FileNotFoundException e){
				throw new SdbException(e);
			} catch (IOException e) {
				throw new SdbException(e);
			}
			this._current_strong="";
			this.parseNode(this.searchContentTag(i_doc.getElementById("ips_Posts")));
		};		
		private void parseNode(Element i_node)
		{
			if(i_node.tagName().compareToIgnoreCase("strong")==0){
				//カレントキー名の保存
				this._current_strong=i_node.text().trim();
			}else if(i_node.tagName().compareToIgnoreCase("a")==0){
				//URL抽出
				String href=i_node.attr("href");
				if(href==null){
					//hrefアトリビュートなし
					return;
				}
				if(!href.matches("((https?)|(ftp))://.*")){
					return;
				}
				String[] param=this._regexp_table.search(href);
				//URL検索キーで調査
				//正規かフラグの取得
				boolean is_url_normalize=false;
				boolean is_type_normalize=false;
				if(param!=null){
					is_url_normalize=param[2].indexOf("U")>=0;
					is_type_normalize=param[2].indexOf("T")>=0;
				}			
				//キーにヒットしない
				this.add(new String[]{
					is_type_normalize?param[1]:normalizeKeyName(this._current_strong),
					this._current_strong,
					is_url_normalize?param[3]:href});
			}else{
				//階層探査
				Elements els=i_node.children();
				for(Element el:els){
					this.parseNode(el);
				}
			}
		}

		private static int searchKey(String i_value,String[] i_list)
		{
			for(int i=0;i<i_list.length;i++)
			{
				if(i_value.matches("(?i)"+i_list[i])){
					return i;
				}
			}
			return -1;
		}
		/**
		 * キー名の正規化
		 * @param i_value
		 * @return
		 */
		private String normalizeKeyName(String i_value)
		{
			String[] block_exproler={"Block(chain)? ((Crawler)|(Explorers?)).*"};
			String[] website_list={"Website.*"};
			String[] sns_list={"Twitter.*","Facebook.*","Reddit.*","Baidu.*","Google\\+"};
			String[] client_list={"Windows( ((ZIP)|(EXE)))?","Ubuntu","Mac(OS)?","Linux.*",".*Client.*","ios","android"};
			String[] source_list={"((SOURCE)|(Source)).*"};
			String[] pool_list={".*Pools?.*"};
			String[] forum_list={".*Forum"};
			String[] faucet_list={"Faucets?"};
			String[] exchange_list={".*Exchanges?.*","Market","OpenEx","Newchg","Coinex","Cryptsy","Coins-E","Bter","Freshmarket"};
			String[] game_list={"Games?"};
			String[] miner_list={".*Miner.*"};
			if(searchKey(i_value,block_exproler)>=0){
				return "BlockExproler";
			}else if(searchKey(i_value,website_list)>=0){
				return "Website";
			}else if(searchKey(i_value,sns_list)>=0){
				return "Sns";
			}else if(searchKey(i_value,client_list)>=0){
				return "Client";
			}else if(searchKey(i_value,source_list)>=0){
				return "SourceCode";
			}else if(searchKey(i_value,forum_list)>=0){
				return "Forum";
			}else if(searchKey(i_value,pool_list)>=0){
				return "Pool";
			}else if(searchKey(i_value,faucet_list)>=0){
				return "Faucet";
			}else if(searchKey(i_value,exchange_list)>=0){
				return "Exchange";
			}else if(searchKey(i_value,game_list)>=0){
				return "Game";
			}else if(searchKey(i_value,miner_list)>=0){
				return "Miner";
			}
			
			return "Unknown";
		}
	}
	private static void extractCoinUrl(NccDBAppArgHelper ap) throws SdbException
	{
		Logger.log("Start extractCoinUrl");
		CoinMasterTable ctt=null;
		//tableオープン
		SqliteDB cache_db=ap.getHtmlCache();
		SqliteDB db=ap.getNccDB();
		HtmlCacheTable hct=new HtmlCacheTable(cache_db,HtmlCacheTable.NAME);
		HtmlCacheTable.RowIterable rows=hct.getAll();
		CoinMasterTable coin_master=new CoinMasterTable(db);
		ServiceUrlTable service_url=new ServiceUrlTable(db);
		CoinUrlIdPairTable id_pair=new CoinUrlIdPairTable(db);
		try{
			db.beginTransaction();
			//全行取得
			for(HtmlCacheTable.Item row : rows){
				//HTMLを解析してURLリストを生成
				CctThreadLinkList pbl=new CctThreadLinkList(Jsoup.parse(row.html));
				//コインシンボル、コイン名を取得
				String[] k=row.key.split(":");

				//コインを選択
				CoinMasterTable.Item coin=coin_master.getItem(k[0],k[1]);
				if(coin==null){
					//コイン見つからない。
					Logger.log(String.format("[ERROR]%s:%s:coin not found",k[0],k[1]));
					continue;
				}
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
					Logger.log(String.format("[OK]%s:%s:url=%s,id_pair=%s",k[0],k[1],is_add_url?"update":"error",is_add_id_pair?"add":"exist"));
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
			String l=ap.getString("url", null);
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
				for(CryptCoinTankCoinListScraper.Item i:items){
					//既にデータベースにあるコインコードなら無視
					if(csu.isExist(i.symbol,i.name))
					{
						Logger.log("[EXIST]"+i.symbol+":"+i.name);
						continue;
					}
					//
					Document d=httpcl.httpGet(i.href);
					//Information Threadを探す
					Element el_content=d.getElementById("forum_table");
					Elements el_tds=el_content.select("h4 a");
					L1: for(int i3=0;i3<el_tds.size();i3++){
						String s=el_tds.get(i3).text();
						String h=el_tds.get(i3).attr("href");
						//入れ子リストは無視
						if(!s.trim().matches(".+Information( .+)?$")){
							continue;
						}
						if(csu.add(i.symbol,i.name,h)){
							Logger.log("[ADD]"+i.symbol+":"+i.name);
						}else{
							Logger.log("[ERROR]"+i.symbol+":"+i.name);
						}
						continue L1;
					}
					if(csu.add(i.symbol,i.name,null)){
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

	private static void scrapeFromCctThread(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		Logger.log("Start name,symbol,url scraping from CryptocoinTalk Coin thread");
		CoinMasterTable ctt=null;
		//URLリストの構築
		String target_url=ap.getString("url",UrlData.CC_ANNOUNCEMENT_URL);

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
					if(csu.add(items.get(i3).href)){
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
	private static void importCoinList(ArgHelper ap,SqliteDB i_db) throws SdbException
	{
		Logger.log("Start syncryptCoinList");
		CoinMasterTable ctt=null;
		CoinSourceUrlTable csu=null;
		//tableオープン
		i_db.beginTransaction();
		try{
			ctt=new CoinMasterTable(i_db);
			csu=new CoinSourceUrlTable(i_db,CoinSourceUrlTable.NAME);
			//nullスペックのidを取得
			CoinSourceUrlTable.Item csti=csu.getAll();
			for(;;){
				if(ctt.isExist("symbol","name")){
					Logger.log("[EXISTS]"+i.symbol+":"+i.name);
					continue;
				}
				//add
				Logger.log("[ADD]"+i.symbol+":"+i.name);
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
			"-cmd cct_html_cache [-cct_db CCTDB] [-u UA] [-url COIN_LIST_URL] [-cookie COOKIE]\n"+
			"-cmd cct_import_urls [-cct_db DB] [-csv CSV]\n"+
			"-cmd cct_scrape_url_from_list_thread [-cct_db CCTDB] [-u UA] [-url CCT_COIN_LIST_URL] [-cookie COOKIE]\n"+
			"-cmd cct_scrape_url_from_thread [-cct_db CCTDB] [-u UA] [-url CCT_THREAD_URL] [-cookie COOKIE]\n"+
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
			//スレッドからHTMLをキャッシュ
			getInformationCoinThread(args,args.getHtmlCache());
		}else if(i_cmd.compareTo("cct_import_urls")==0){
			//HTMLからコインペアへサービスURLリストをインポート
			extractCoinUrl(args);
		}else if(i_cmd.compareTo("cct_scrape_url_from_list_thread")==0){
			//コインリストからコインソースURLテーブルへデータをスクレイプ
			scrapeFromCoinList(args,args.getHtmlCache());
		}else if(i_cmd.compareTo("cct_scrape_url_from_thread")==0){
			//CryptocoinTalkのスレッドからURLリストをインポート
			scrapeFromCctThread(args,args.getHtmlCache());
		}else{
			return false;
		}
		return true;
	}
}
