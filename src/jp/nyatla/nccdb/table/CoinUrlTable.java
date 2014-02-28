package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jp.nyatla.nyansat.db.basic.table.BaseTable;
import jp.nyatla.nyansat.db.basic.table.SqliteDB;
import jp.nyatla.nyansat.utils.SdbException;

/**
 * コインのスペック情報を格納するテーブル。
 * <h2>目的</h2>
 * <h2>API</h2>
 * 
 * <h2>カラム構成</h2>
 * <p>
 * <ul>
 * <li>coin_symbol text - コインのシンボル名 (PK)
 * <li>coin_name text - プロダクト名
 * </ul>
 * unique(cpu_id,processor_number)
 * </p>
 */
public class CoinUrlTable extends BaseTable
{
	public final static String NAME="coin_url";
	public final static String DN_id="id";
	public final static String DN_id_coin_url_type="id_coin_url_type";
	public final static String DN_id_coin_url_status="id_coin_url_status";
	public final static String DN_date="date";	
	public final static String DN_url="url";	
	public final static String DN_description="description";	

	
	protected String createTableDefinisitonStr()
	{
		return "("+
				DN_id+" int,"+
				DN_id_coin_url_type+" int,"+
				DN_id_coin_url_status+" int,"+
				DN_date+" int,"+
				DN_url+" text,"+
				DN_description+" text,"+
				"primary key("+DN_id+")";
	}
	@Override
	public void dispose() throws SdbException
	{
		try {
			this._ps_insert.close();
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}

	private PreparedStatement _ps_insert;
	public CoinUrlTable(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,i_table_name);
		try {
			this._ps_insert=this._db.getConnection().prepareStatement("insert or ignore into "+this._tbl_name+
				"("+DN_id_coin_url_type+","+DN_id_coin_url_status+","+DN_date+","+DN_url+","+DN_description+") values(?,?,?,?,?);");
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	protected boolean add(int i_id_coin_url_type,double id_coin_url_status,Long i_date,String i_url,String i_description) throws SdbException
	{
		try {
			this._ps_insert.setInt(1,i_id_coin_url_type);
			this._ps_insert.setDouble(2,id_coin_url_status);
			if(i_date==null){
				this._ps_insert.setNull(3,java.sql.Types.INTEGER);
			}else{
				this._ps_insert.setLong(3,i_date);
			}
			this._ps_insert.setString(4,i_url);
			if(i_date==null){
				this._ps_insert.setNull(5,java.sql.Types.CHAR);
			}else{
				this._ps_insert.setString(5,i_description);
			}
			this._ps_insert.execute();
			return this._ps_insert.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	public static class Item
	{
		public int id;
		public int id_coin_url_type;
		public int id_coin_url_status;
		public long date;
		public String url;
		public String description;
	}
}