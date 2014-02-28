package jp.nyatla.nccdb.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import jp.nyatla.nccdb.CoinAlgorismTable;
import jp.nyatla.nyansat.db.basic.table.SqliteDB;
import jp.nyatla.nyansat.db.basic.view.BaseView;
import jp.nyatla.nyansat.utils.CsvWriter;
import jp.nyatla.nyansat.utils.SdbException;

public class CoinInfoView extends BaseView
{
	public final static String NAME="coin_info";	

	protected String createViewDefinisitonStr()
	{
		return
			"select" +
			" t."+CoinTitleTable.DN_symbol+
			",t."+CoinTitleTable.DN_name+
			",s."+CoinSpecTable.DN_start_date+
			",s."+CoinSpecTable.DN_total_coin+
			",s."+CoinSpecTable.DN_premine+
			",s."+CoinSpecTable.DN_id_block_reword_type+
			",s."+CoinSpecTable.DN_id_algorism+
			" from "+
			CoinTitleTable.NAME+" t,"+CoinSpecTable.NAME+" s"+
			" where "+
			"t."+CoinTitleTable.DN_id+"=s."+CoinSpecTable.DN_id_coin_title+";";
	}
	@Override
	public void dispose() throws SdbException
	{
		super.dispose();
	}
	public CoinInfoView(SqliteDB i_db) throws SdbException
	{
		this(i_db,NAME);
	}
	public CoinInfoView(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,i_table_name);
		try {
			this._ps_select_all.close();
			this._ps_select_all=this._db.getConnection().prepareStatement(
				"select * from "+this._view_name +" ORDER BY "+CoinTitleTable.DN_symbol+" ASC;");
		} catch (SQLException e){
			throw new SdbException(e);
		}
	}
	public RowIterable getAll() throws SdbException
	{
		try {
			ResultSet rs=this._ps_select_all.executeQuery();
			return new RowIterable(rs);
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	public class RowIterable implements Iterable<Item>,Iterator<Item>
	{
		private ResultSet _rs;
		public RowIterable(ResultSet i_rs)
		{
			this._rs=i_rs;
		}
		@Override
		public Iterator<Item> iterator() {
			// TODO Auto-generated method stub
			return this;
		}
		@Override
		public boolean hasNext()
		{
			try {
				return this._rs.next();
			} catch (SQLException e) {
				return false;
			}
		}
		@Override
		public Item next()
		{
			try {
				return new Item(this._rs);
			} catch (SQLException e) {
				return null;
			}
		}
		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
		public void dispose()
		{
			try {
				this._rs.close();
			} catch (SQLException e){
			}
			this._rs=null;
		}
	}
	public String[] getColHeader()
	{
		return new String[]{
			CoinTitleTable.DN_symbol,
			CoinTitleTable.DN_name,
			CoinSpecTable.DN_start_date,
			CoinSpecTable.DN_total_coin,
			CoinSpecTable.DN_premine,
			CoinSpecTable.DN_id_block_reword_type,
			CoinSpecTable.DN_id_algorism
		};
	}
	public class Item
	{
		public String symbol;
		public String name;
		public Long start_date;
		public Long total_coin;
		public Long premine;
		public int id_block_reword_type;
		public int id_algorism;
		public Item(ResultSet i_rs) throws SQLException
		{
			this.symbol=i_rs.getString(CoinTitleTable.DN_symbol);
			this.name=i_rs.getString(CoinTitleTable.DN_name);
			this.start_date=i_rs.getLong(CoinSpecTable.DN_start_date);
			if(i_rs.wasNull()){
				this.start_date=null;
			}
			this.total_coin=i_rs.getLong(CoinSpecTable.DN_total_coin);
			if(i_rs.wasNull()){
				this.total_coin=null;
			}
			this.premine=i_rs.getLong(CoinSpecTable.DN_premine);
			if(i_rs.wasNull()){
				this.premine=null;
			}
			i_rs.getInt(CoinSpecTable.DN_id_block_reword_type);
			i_rs.getString(CoinSpecTable.DN_id_algorism);
		}
		public String[] toCsvArrray()
		{
			return new String[]{
				this.symbol,
				this.name,
				this.start_date==null?"":CsvWriter.toCsvDate(this.start_date),
				this.total_coin==null?"":this.total_coin.toString(),
				this.premine==null?"":this.premine.toString(),
				Integer.toString(this.id_block_reword_type),
				CoinAlgorismTable.getSingleton().getString(this.id_algorism)};
		}
	}	

}
