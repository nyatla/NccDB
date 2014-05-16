package jp.nyatla.nccdbtoolkit.urlparser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import jp.nyatla.nccdbtoolkit.RegExpKeyTable;
import jp.nyatla.nyansat.utils.SdbException;

public class UrlPickup extends ArrayList<String[]>
{
	private static final long serialVersionUID = 1L;
	private static final String DICTIONARY_FILE="./url_normalize.dat";
	protected RegExpKeyTable _regexp_table;
	public UrlPickup() throws SdbException
	{
		try {
			this._regexp_table=new RegExpKeyTable(DICTIONARY_FILE);
		} catch (FileNotFoundException e){
			throw new SdbException(e);
		} catch (IOException e) {
			throw new SdbException(e);
		}
	};

	protected String[] normalizeURL(String i_name,String i_uri)
	{
		//形式チェック
		if(!i_uri.matches("((https?)|(ftp))://.*")){
			return null;
		}
		//URL正規化
		String[] param=this._regexp_table.search(i_uri);
		//正規かフラグの取得
		boolean is_url_normalize=false;
		boolean is_type_normalize=false;
		if(param!=null){
			is_url_normalize=param[2].indexOf("U")>=0;
			is_type_normalize=param[2].indexOf("T")>=0;
		}
		//Itemを生成して返す。(0:正規化キー名,元の名前,正規化URL)
		return new String[]{
			is_type_normalize?param[1]:normalizeKeyName(i_name),
			i_name,
			is_url_normalize?param[3]:i_uri};
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
	private static String normalizeKeyName(String i_value)
	{
		if(i_value==null){
			return "Unknown";
		}
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
