package jp.nyatla.nccdb;



import jp.nyatla.nyansat.utils.BaseObject;
import jp.nyatla.nyansat.utils.BasicHttpClient;

import jp.nyatla.nyansat.utils.SdbException;


import java.util.ArrayList;


import org.jsoup.nodes.*;
import org.jsoup.select.*;




/**
 * CryptCoinTankのコインリストスレッドから、コイン名とシンボルを抽出してデータベースへ登録する。
 * 重複は無視する。
 *
 */
public class CryptCoinTankListScraper extends BaseObject
{	
	public class Item
	{
		public String symbol;
		public String name;
		public String href;
		public String toString()
		{
			return this.symbol+":"+this.name;
		}
		Item(String i_symbol,String i_name,String i_href)
		{
			this.symbol=i_symbol;
			this.name=i_name;
			this.href=i_href;
		}
	}
	private BasicHttpClient _httpcl;
	public CryptCoinTankListScraper(String i_ua, String i_cookie) throws SdbException
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
			//入れ子リストは無視
			if(s.matches("DYING.+")){
				continue;
			}
			//Itemの生成
			String[] p=s.split(" ");
			if(p.length==1){
				i_dest.add(new Item(p[0],p[0],h));
			}else{
				String t=p[0];
				for(int i2=1;i2<p.length-1;i2++){
					t+=" "+p[i2];
				}
				i_dest.add(new Item(p[p.length-1],t,h));
			}
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
