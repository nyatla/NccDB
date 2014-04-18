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
public class CryptCoinTankThreadUrlScraper extends BaseObject
{	
	public class Item
	{
		public String href;
		public String title;
		Item(String i_title,String i_href)
		{
			this.title=i_title;
			this.href=i_href;
		}
	}
	private BasicHttpClient _httpcl;
	public CryptCoinTankThreadUrlScraper(String i_ua, String i_cookie) throws SdbException
	{
		this._httpcl=new BasicHttpClient();
		this._httpcl.setSession(i_ua,i_cookie);
	}

	public void append(String i_url,ArrayList<Item> i_dest) throws SdbException
	{
		Document d=this._httpcl.httpGet(i_url);
		//ドキュメントのパース
		Element el_content=d.getElementById("content");
		Elements el_tds=el_content.select("h4 a");
		for(int i=0;i<el_tds.size();i++){
			String s=el_tds.get(i).text();
			String h=el_tds.get(i).attr("href");
			i_dest.add(new Item(s,h));
		}
	}
	public ArrayList<Item> parse(String i_url) throws SdbException
	{
		ArrayList<Item> l=new ArrayList<Item>();
		this.append(i_url,l);
		return l;
	}
	public ArrayList<Item> parse(String[] i_url) throws SdbException
	{
		ArrayList<Item> l=new ArrayList<Item>();
		for(int i=i_url.length-1;i>=0;i--){
			this.append(i_url[i],l);
		}
		return l;
	}
}
