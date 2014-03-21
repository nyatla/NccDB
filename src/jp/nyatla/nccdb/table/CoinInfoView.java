package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import jp.nyatla.nccdb.table.ServiceUrlTable.Item;
import jp.nyatla.nccdb.table.internal.CoinAlgorismTable;
import jp.nyatla.nyansat.db.basic.BaseRowIterable;
import jp.nyatla.nyansat.db.basic.BasicViewDefinition;
import jp.nyatla.nyansat.db.basic.RsUtils;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.db.basic.view.BaseView;
import jp.nyatla.nyansat.utils.CsvWriter;
import jp.nyatla.nyansat.utils.SdbException;

public class CoinInfoView extends BaseView
{
	public final static String NAME="coin_info";	
	private static class TableDef extends BasicViewDefinition
	{
		private String[] _cols={
			CoinMasterTable.DN_id,
			CoinMasterTable.DN_symbol,
			CoinMasterTable.DN_name,
			CoinMasterTable.DN_alias_id,
			CoinMasterTable.DN_start_date,
			CoinSpecTable.DN_total_coin,
			CoinSpecTable.DN_premine,
			CoinSpecTable.DN_id_algorism};
		public TableDef(String i_name) {
			super(i_name);
		}
		@Override
		public String getCreateStr(){
			return
				"select" +
				" t."+CoinMasterTable.DN_id+
				",t."+CoinMasterTable.DN_symbol+
				",t."+CoinMasterTable.DN_name+
				",t."+CoinMasterTable.DN_alias_id+
				",t."+CoinMasterTable.DN_start_date+
				",s."+CoinSpecTable.DN_total_coin+
				",s."+CoinSpecTable.DN_premine+
				",s."+CoinSpecTable.DN_id_algorism+
				" from "+
				CoinMasterTable.NAME+" t,"+CoinSpecTable.NAME+" s"+
				" where "+
				"t."+CoinMasterTable.DN_spec_id+"=s."+CoinSpecTable.DN_id+";";
		}
		@Override
		public String[] getElementNames() {
			return this._cols;
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
	
	public CoinInfoView(SqliteDB i_db) throws SdbException
	{
		this(i_db,NAME);
	}
	public CoinInfoView(SqliteDB i_db,String i_table_name) throws SdbException
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
	public RowIterable getAll() throws SdbException
	{
		try {
			ResultSet rs=this._ps_select_all.executeQuery();
			return new RowIterable(rs);
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}

	public final class RowIterable extends BaseRowIterable<Item>
	{
		public RowIterable(ResultSet i_rs)
		{
			super(i_rs);
		}

		@Override
		protected Item createItem(ResultSet i_rs) throws SdbException
		{
			try {
				return new Item(i_rs);
			} catch (SQLException e) {
				return null;
			}
		}
	}	

	public String[] getColHeader()
	{
		return new String[]{
			CoinMasterTable.DN_id,
			CoinMasterTable.DN_symbol,
			CoinMasterTable.DN_name,
			CoinMasterTable.DN_alias_id,
			CoinMasterTable.DN_start_date,
			CoinSpecTable.DN_total_coin,
			CoinSpecTable.DN_premine,
			CoinSpecTable.DN_id_algorism
		};
	}
	public class Item
	{
		public int coin_id;
		public String symbol;
		public String name;
		public Integer alias;
		public Long start_date;
		public Double total_coin;
		public Double premine;
		public int id_algorism;
		public Item(ResultSet i_rs) throws SQLException
		{
			this.coin_id=i_rs.getInt(CoinMasterTable.DN_id);
			this.symbol=i_rs.getString(CoinMasterTable.DN_symbol);
			this.name=i_rs.getString(CoinMasterTable.DN_name);
			this.alias=RsUtils.getNullableInt(i_rs,CoinMasterTable.DN_alias_id);
			this.start_date=RsUtils.getNullableLong(i_rs,CoinMasterTable.DN_start_date);
			this.total_coin=RsUtils.getNullableDouble(i_rs,CoinSpecTable.DN_total_coin);
			this.premine=RsUtils.getNullableDouble(i_rs,CoinSpecTable.DN_premine);
			this.id_algorism=i_rs.getInt(CoinSpecTable.DN_id_algorism);
		}
		public String[] toCsvArrray()
		{
			return new String[]{
				Integer.toString(this.coin_id),
				this.symbol,
				this.name,
				this.alias==null?"":this.alias.toString(),
				this.start_date==null?"":CsvWriter.toCsvDate(this.start_date),
				this.total_coin==null?"":String.format("%f",this.total_coin),
				this.premine==null?"":String.format("%f",this.premine),
				CoinAlgorismTable.getSingleton().getString(this.id_algorism)};
		}
	}	

}
