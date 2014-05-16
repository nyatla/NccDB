package jp.nyatla.nccdb;




import jp.nyatla.nyansat.utils.BaseObject;
import jp.nyatla.nyansat.utils.BasicHttpClient;

import jp.nyatla.nyansat.utils.SdbException;


import java.util.ArrayList;


import org.jsoup.nodes.*;
import org.jsoup.select.*;




/**
 * CryptCoinTankのコインリストスレッドから、コイン名とシンボルを抽出するスクレイパーです。
 * 
 *
 */
public class BitCoinTankCoinListScraper extends BaseObject
{	
	public class Item
	{
		public String href;
		Item(String i_href)
		{
			this.href=i_href;
		}
	}
	private int _max_thread_index;
	private BasicHttpClient _httpcl;
	public int getMaxThread()
	{
		return this._max_thread_index;
	}
	public BitCoinTankCoinListScraper(String i_ua, String i_cookie) throws SdbException
	{
		this._httpcl=new BasicHttpClient();
		this._httpcl.setSession(i_ua,i_cookie);
	}

	private boolean append(String i_url,ArrayList<Item> i_dest) throws SdbException
	{
		Document d=this._httpcl.httpGet(i_url);
		if(d==null && this._httpcl.getLastStatus()!=200){
			//HTTPエラー？
			return false;
		}
		//ドキュメントのパース
		Element el_toppage=d.getElementById("toppages");
		//last_indexを得る。
		Elements el_a=el_toppage.select("a");
		this._max_thread_index=Integer.parseInt(el_a.get(el_a.size()-1).text());
		//
		Element el_body=d.getElementById("bodyarea");
		Elements el_tds=el_body.select("table").get(2).select("td.windowbg");
		for(int i=0;i<el_tds.size();i+=3){
			String h=(el_tds.get(i).select("a").get(0).attr("href"));
			i_dest.add(new Item(h));
		}
		return true;
	}
	public ArrayList<Item> parse(String i_url) throws SdbException
	{
		ArrayList<Item> l=new ArrayList<Item>();
		if(!this.append(i_url,l)){
			return null;
		}
		return l;
	}
	public ArrayList<Item> parse(String[] i_url) throws SdbException
	{
		ArrayList<Item> l=new ArrayList<Item>();
		for(int i=i_url.length-1;i>=0;i--){
			if(!this.append(i_url[i],l)){
				return null;
			}
		}
		return l;
	}
}
