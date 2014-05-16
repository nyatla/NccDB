package jp.nyatla.nccdbtoolkit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import jp.nyatla.nyansat.utils.SdbException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * タグツリーからStrong値とURL解析結果を格納するArrayList。
 * 各行の内容は以下の通り
 * s[0]:正規化カテゴリ
 * s[1]:生カテゴリ
 * s[2]:正規化URL
 * 
 */
@SuppressWarnings("serial")
public class CctLinkListParser extends ArrayList<String[]>
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

	public CctLinkListParser(Document i_doc) throws SdbException
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

