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
public class IdPairTable extends BaseTable<IdPairTable.Item>
{

	private static class IdPairTableInfo extends BasicTableDefinition<Item>
	{
		private String[] _names;
		public IdPairTableInfo(String i_table_name,String i_id_name1,String i_id_name2)
		{
			super(i_table_name);
			this._names=new String[]{i_id_name1,i_id_name2};
		}
		@Override
		public String[] getElementNames() {
			return this._names;
		}
		@Override
		public String getCreateStr(){
			return "("+
				this._names[0]+" integer,"+
				this._names[1]+" integer,"+
				"unique("+this._names[0]+","+this._names[1]+"))";
		}
	}
	@Override
	public void dispose() throws SdbException
	{
		try {
			this._ps_insert.close();
			this._ps_select.close();
			this._ps_delete.close();
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}

	private PreparedStatement _ps_delete;
	private PreparedStatement _ps_insert;
	private PreparedStatement _ps_select;
	public IdPairTable(SqliteDB i_db,String i_table_name,String i_id1,String i_id2) throws SdbException
	{
		super(i_db,new IdPairTableInfo(i_table_name,i_id1,i_id2));
		String[] d=this._table_info.getElementNames();
		try{
			String table_name=this._table_info.getTableName();
			this._ps_insert=this._db.getConnection().prepareStatement(
				String.format(
					"insert or ignore into %s(%s,%s) values(?,?);",
					table_name,d[0],d[1]));
			this._ps_select=this._db.getConnection().prepareStatement(
				String.format("select * from %s where %s=? and %s=?;",
				table_name,d[0],d[1]));
			this._ps_delete=this._db.getConnection().prepareStatement(
				String.format("delete from %s where %s=? and %s=?;",
				table_name,d[0],d[1]));
		}catch (SQLException e){
			throw new SdbException(e);
		}
	}
	public boolean add(int i_id1,int i_id2) throws SdbException
	{
		try {
			this._ps_insert.setInt(1,i_id1);
			this._ps_insert.setInt(2,i_id2);
			this._ps_insert.execute();
			return this._ps_insert.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
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
	}
	public Item getItem(int i_id1,int i_id2) throws SdbException
	{
		try{
			ResultSet rs=null;
			try{
				this._ps_select.setInt(1,i_id1);
				this._ps_select.setInt(2,i_id2);
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
	}
	public static class Item
	{
		public Item(ResultSet rs) throws SQLException
		{
			this.id1=rs.getInt(1);
			this.id2=rs.getInt(2);
		}
		public int id1;
		public int id2;
	}
}