package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.nyatla.nccdb.table.internal.CoinAlgorismTable;
import jp.nyatla.nyansat.db.basic.BasicViewDefinition;
import jp.nyatla.nyansat.db.basic.RowIterable;
import jp.nyatla.nyansat.db.basic.RsUtils;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.db.basic.view.BaseView;
import jp.nyatla.nyansat.utils.CsvWriter;
import jp.nyatla.nyansat.utils.SdbException;

/**
 * coin_masterとcoin_specを内部結合したテーブル。
 * @author nyatla
 *
 */
public class CoinBaseHtmlCacheView extends BaseView<CoinBaseHtmlCacheView.Item>
{
	public final static String NAME="html_cache_coin_pair";	
	private static class TableDef extends BasicViewDefinition<Item>
	{
		private String[] _cols={
			HtmlCacheTable.DN_date,
			CoinSourceUrlTable.DN_symbol,
			CoinSourceUrlTable.DN_name,
			CoinSourceUrlTable.DN_domain,
			HtmlCacheTable.DN_html,
		};
		public TableDef(String i_name) {
			super(i_name);
		}
		@Override
		public String getCreateStr(){
			return
				"select" +
				" t."+HtmlCacheTable.DN_date+
				",s."+CoinSourceUrlTable.DN_symbol+
				",s."+CoinSourceUrlTable.DN_name+
				",s."+CoinSourceUrlTable.DN_domain+
				",t."+HtmlCacheTable.DN_html+
				" from "+
				HtmlCacheTable.NAME+" t,"+CoinSourceUrlTable.NAME+" s"+
				" where "+
				"t."+HtmlCacheTable.DN_url+"=s."+CoinSourceUrlTable.DN_url+";";
		}
		@Override
		public String[] getElementNames() {
			return this._cols;
		}
		@Override
		public Item createRowItem(ResultSet rs) throws SdbException
		{
			return new Item(rs);
		}
	}
	private PreparedStatement _ps_select_all;
	@Override
	public void dispose() throws SdbException
	{
		try {
			this._ps_select_all.close();
		} catch (SQLException e){
			throw new SdbException(e);
		}
		super.dispose();
	}
	
	public CoinBaseHtmlCacheView(SqliteDB i_db) throws SdbException
	{
		this(i_db,NAME);
	}
	public CoinBaseHtmlCacheView(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,new TableDef(i_table_name));
		try {
			String view_name=this._view_info.getTableName();
			this._ps_select_all=this._db.getConnection().prepareStatement(
				"select * from "+view_name +" ORDER BY "+CoinMasterTable.DN_symbol+" ASC;");
		} catch (SQLException e){
			throw new SdbException(e);
		}
	}
	public static class Item
	{
		public Item(ResultSet rs) throws SdbException
		{
			try{
				this.date=rs.getLong(1);
				this.symbol=rs.getString(2);
				this.name=rs.getString(3);
				this.domain=rs.getInt(4);
				this.html=rs.getString(5);
			}catch(SQLException e){
				throw new SdbException(e);
			}
		}
		public String symbol;
		public String name;
		public long date;
		public int domain;
		public String html;
	}

}
