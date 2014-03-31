import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import jp.nyatla.nccdb.CryptCoinTankListScraper;
import jp.nyatla.nccdb.CryptCoinTankListScraper.Item;
import jp.nyatla.nccdb.table.CoinMasterTable;
import jp.nyatla.nccdb.table.CoinSpecTable;
import jp.nyatla.nccdb.table.HtmlCacheTable;
import jp.nyatla.nccdb.table.internal.CoinAlgorismTable;
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
		String[] url_list=CoinListCsvIo.CC_URL_LIST;
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
			CryptCoinTankListScraper scp=new CryptCoinTankListScraper(ua,cookie);
			//CoinListの取得
			for(int i2=0;i2<url_list.length;i2++){
				Logger.log("Scan URL="+url_list[i2]);
				ArrayList<CryptCoinTankListScraper.Item> items=scp.parse(url_list[i2]);
				//テーブルを追記
				BasicHttpClient httpcl=new BasicHttpClient();
				httpcl.setSession(ua,cookie);
				for(CryptCoinTankListScraper.Item i:items){
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
	 * タグツリーからStrong値とURLペアを格納するArrayList
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
		private CctThreadLinkList(Document i_doc)
		{
			super();
			try {
				this._regexp_table=new RegExpKeyTable(DICTIONARY_FILE);
			} catch (FileNotFoundException e){
			} catch (IOException e) {
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
				//URL検索キーで調査
				String[] param=this._regexp_table.search(href);
				
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
					is_url_normalize?param[3]:i_node.attr("href")});
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
		HtmlCacheTable hct=new HtmlCacheTable(cache_db,HtmlCacheTable.NAME);
		HtmlCacheTable.RowIterable rows=hct.getAll();
		CoinMasterTable coin_master=new CoinMasterTable(cache_db);
		try{
			//全行取得
			for(HtmlCacheTable.Item row : rows){
				//HTMLを解析してURLリストを生成
				CctThreadLinkList pbl=new CctThreadLinkList(Jsoup.parse(row.html));
				//コインシンボル、コイン名を取得
				String[] k=row.key.split(":");

				//コインデータベースへインポート

				//コインを選択
				coin_master.getItem(k[0],k[1]);
				for(String[] s:pbl){
					//URLをignore existで追加
					//URLを選択
					
					//コインペアをignore existで追加
					Logger.log(
						String.format("%s,%s,%s,%s,%s",k[0],k[1],s[0],s[1].replaceAll(","," "),s[2]));
				}				
			}
		}finally{
			if(ctt!=null){
				ctt.dispose();
			}
			if(hct!=null){
				hct.dispose();
			}
		}
		Logger.log("done.");
	}
	
	public static boolean run(String i_cmd,NccDBAppArgHelper args) throws SdbException
	{
		if(i_cmd.compareTo("cct_html_cache")==0){
			//スレッドからHTMLをキャッシュ
			getInformationCoinThread(args,args.getHtmlCache());
		}else if(i_cmd.compareTo("cct_import_urls")==0){
			//HTMLからコインペアへ
			extractCoinUrl(args,args.getHtmlCache());
		}else{
			return false;
		}
		return true;
	}	
}
