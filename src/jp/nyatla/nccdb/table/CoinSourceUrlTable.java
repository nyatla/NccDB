package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import jp.nyatla.nccdb.table.HtmlCacheTable.Item;
import jp.nyatla.nccdb.table.HtmlCacheTable.RowIterable;
import jp.nyatla.nyansat.db.basic.BaseRowIterable;
import jp.nyatla.nyansat.db.basic.BasicTableDefinition;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.db.basic.table.BaseTable;
import jp.nyatla.nyansat.utils.SdbException;

public class CoinSourceUrlTable extends BaseTable
{
	public final static String NAME="cct_cache";

	private static class CoinSourceUrlTableInfo extends BasicTableDefinition
	{
		private final static String id_symbol="symbol";
		private final static String id_name="name";
		private final static String id_url="url";
		public CoinSourceUrlTableInfo(String i_table_name)
		{
			super(i_table_name);
		}
		@Override
		public String[] getElementNames() {
			return new String[]{id_symbol,id_name,id_url};
		}
		@Override
		public String getCreateStr(){
			return "("+
				id_symbol+" text,"+
				id_name+" text,"+
				id_url+" text," +
				"unique("+id_symbol+","+id_name+"))";
		}
	}
	@Override
	public void dispose() throws SdbException
	{
		try {
			this._ps_insert.close();
			this._ps_select_by_key.close();
			this._ps_select_all.close();
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}

//	private PreparedStatement _ps_delete;
	private PreparedStatement _ps_insert;
	private PreparedStatement _ps_select_by_key;
	private PreparedStatement _ps_select_all;
	public CoinSourceUrlTable(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,new CoinSourceUrlTableInfo(i_table_name));
		String[] d=this._table_info.getElementNames();
		try{
			String table_name=this._table_info.getTableName();
			this._ps_insert=this._db.getConnection().prepareStatement(
				String.format(
					"insert or ignore into %s(%s,%s,%s) values(?,?,?);",
					table_name,d[0],d[1],d[2]));
			this._ps_select_by_key=this._db.getConnection().prepareStatement(
				String.format("select * from %s where %s=?;",
				table_name,d[1]));
			this._ps_select_all=this._db.getConnection().prepareStatement(
				String.format("select * from %s;",
				table_name));
		}catch (SQLException e){
			throw new SdbException(e);
		}
	}
	public boolean add(String i_symbol,String i_name,String i_url) throws SdbException
	{
		try {
			this._ps_insert.setString(1,i_symbol);
			this._ps_insert.setString(2,i_name);
			this._ps_insert.setString(3,i_url);
			this._ps_insert.execute();
			return this._ps_insert.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	public boolean add(String i_url) throws SdbException
	{
		try {
			this._ps_insert.setString(1,Long.toString(((new Date()).getTime())));
			this._ps_insert.setNull(2,java.sql.Types.CHAR);
			this._ps_insert.setString(3,i_url);
			this._ps_insert.execute();
			Thread.sleep(10);
			return this._ps_insert.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		} catch (InterruptedException e) {
			throw new SdbException(e);
		}
	}	
	public boolean isExist(String i_id1) throws SdbException
	{
		try {
			this._ps_select_by_key.setString(1,i_id1);
			ResultSet rs=this._ps_select_by_key.executeQuery();
			boolean ret=rs.next();
			rs.close();
			return ret;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}	

	public static class Item
	{
		public Item(ResultSet rs) throws SQLException
		{
			this.date=rs.getLong(1);
			this.key=rs.getString(2);
			this.html=rs.getString(3);
		}
		public long date;
		public String key;
		public String html;
	}
	
	public RowIterable getAll() throws SdbException
	{
		try {
			ResultSet rs=this._ps_select_all.executeQuery();
			return new RowIterable(rs);
		} catch (SQLException e){
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
	
}