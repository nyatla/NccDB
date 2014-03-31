import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.nyatla.nyansat.utils.CsvReader;


/**
 * 正規表現をキーに定義値を検索します。
 *
 */
public class RegExpKeyTable
{
	private class MatchItem
	{
		public Pattern pat;
		public String[] params;
		public MatchItem(CsvReader i_reader)
		{
			int l=i_reader.getCols();
			String[] d=new String[l];
			this.pat=Pattern.compile(i_reader.getString(0));
			for(int i=1;i<l;i++){
				d[i]=i_reader.getString(i);
			}
			this.params=d;
		}
	}
	private MatchItem[] _items;
	public RegExpKeyTable(String i_file_path) throws FileNotFoundException, IOException
	{
		this(new FileInputStream(i_file_path));
	}
	/**
	 * CSVファイルから辞書情報を読み出す。
	 * CSVファイルの先頭1文字目はヘッダ。二行目以降はデータです。
	 * ヘッダは読み込み時に無視されます。
	 * 1列目はマッチパターン、2列目移行はマッチした場合の任意文字列です。
	 * 1列目
	 * @param i_dictionary_file
	 * @throws IOException 
	 */
	public RegExpKeyTable(InputStream i_dictionary_file) throws IOException
	{
		CsvReader r=new CsvReader(i_dictionary_file);
		ArrayList<MatchItem> a=new ArrayList<MatchItem>();
		while(r.next()){
			a.add(new MatchItem(r));
		}
		this._items=a.toArray(new MatchItem[a.size()]);
	}
	/**
	 * 正規表現をキーにテーブルからパラメータ列を返します。
	 * @param i_key
	 * @return
	 */
	public String[] search(String i_key)
	{
		for(int i=0;i<this._items.length;i++){
			Matcher m=this._items[i].pat.matcher(i_key);
			if(m.matches()){
				return this._items[i].params;
			}
		}
		return null;
	}
}
