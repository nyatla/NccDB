package jp.nyatla.nccdbtoolkit.urlparser;


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
public class CctUrlPickup extends UrlPickup
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
	private String _current_strong;

	public CctUrlPickup(Document i_doc) throws SdbException
	{
		super();
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
			String[] ret=this.normalizeURL(this._current_strong, href);
			if(ret==null){
				//正規化不可
				return;
			}
			//追加
			this.add(ret);
		}else{
			//階層探査
			Elements els=i_node.children();
			for(Element el:els){
				this.parseNode(el);
			}
		}
	}


}

