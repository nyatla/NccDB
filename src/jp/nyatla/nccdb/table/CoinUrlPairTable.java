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
public class CoinUrlPairTable extends BaseTable
{
	public final static String NAME="coin_spec";
	public final static String DN_id_coin_title="id_coin_title";
	public final static String DN_id_coin_url="id_coin_url";

	
	protected String createTableDefinisitonStr()
	{
		return "("+
			DN_id_coin_title+" int,"+
			DN_id_coin_url+" text"+
			"unique("+DN_id_coin_title+","+DN_id_coin_url+")";
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
	public CoinUrlPairTable(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,i_table_name);
		try {
			this._ps_insert=this._db.getConnection().prepareStatement("insert or ignore into "+this._tbl_name+
				"("+DN_id_coin_title+","+DN_id_coin_url+") values(?,?);");
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	protected boolean add(int i_id_coin_id,int i_id_coin_url) throws SdbException
	{
		try {
			this._ps_insert.setInt(1,i_id_coin_id);
			this._ps_insert.setInt(2,i_id_coin_url);
			this._ps_insert.execute();
			return this._ps_insert.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	public static class Item
	{
		public int id_coin_id;
		public int id_coin_url;
	}
}