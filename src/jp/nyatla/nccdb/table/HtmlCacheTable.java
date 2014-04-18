package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.nyatla.nyansat.db.basic.BasicTableDefinition;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.db.basic.table.BaseTable;
import jp.nyatla.nyansat.utils.SdbException;

/**
 * IDペアテーブルの基本クラス。
 * {@link #createTableDefinisitonStr}関数を
 * <ul>
 * <li>coin_symbol text - コインのシンボル名 (PK)
 * <li>coin_name text - プロダクト名
 * </ul>
 * unique(cpu_id,processor_number)
 * </p>
 */
public class HtmlCacheTable extends BaseTable<HtmlCacheTable.Item>
{
	public final static String NAME="cct_cache";

	private static class HtmlCacheTableInfo extends BasicTableDefinition<Item>
	{
		private final static String id_date="date";
		private final static String id_key="key";
		private final static String id_html="html";
		public HtmlCacheTableInfo(String i_table_name)
		{
			super(i_table_name);
		}
		@Override
		public String[] getElementNames() {
			return new String[]{id_date,id_key,id_html};
		}
		@Override
		public String getCreateStr(){
			return "("+
				id_date+" integer,"+
				id_key+" text,"+
				id_html+" text,"+
				"unique("+id_key+"))";
		}
		@Override
		public Item createRowItem(ResultSet rs) throws SdbException {
			return new Item(rs);
		}
	}
	@Override
	public void dispose() throws SdbException
	{
		try {
			this._ps_insert.close();
			this._ps_select_by_key.close();
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}

	private PreparedStatement _ps_insert;
	private PreparedStatement _ps_select_by_key;
	public HtmlCacheTable(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,new HtmlCacheTableInfo(i_table_name));
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
		}catch (SQLException e){
			throw new SdbException(e);
		}
	}
	public boolean add(long i_date,String i_id1,String i_id2) throws SdbException
	{
		try {
			this._ps_insert.setLong(1,i_date);
			this._ps_insert.setString(2,i_id1);
			this._ps_insert.setString(3,i_id2);
			this._ps_insert.execute();
			return this._ps_insert.getUpdateCount()>0;
		} catch (SQLException e) {
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
	/*
	public boolean delete(int i_id1,int i_id2) throws SdbException
	{
		try {
			this._ps_delete.setInt(1,i_id1);
			this._ps_delete.setInt(2,i_id2);
			this._ps_delete.execute();
			return this._ps_delete.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}*//*
	public Item getItem(String i_key) throws SdbException
	{
		try{
			ResultSet rs=null;
			try{
				this._ps_select.setString(1,i_id1);
				this._ps_select.setString(2,i_id2);
				rs=this._ps_select.executeQuery();
				if(rs.next()){
					return new Item(rs);
				}else{
					return null;
				}
			}finally{
				if(rs!=null){
					rs.close();
				}
			}
		}catch(SQLException e){
			throw new SdbException(e);
		}
	}*/
	public static class Item
	{
		public Item(ResultSet rs) throws SdbException
		{
			try{
				this.date=rs.getLong(1);
				this.key=rs.getString(2);
				this.html=rs.getString(3);
			}catch(SQLException e){
				throw new SdbException(e);
			}
		}
		public long date;
		public String key;
		public String html;
	}
	/*
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
*/
}