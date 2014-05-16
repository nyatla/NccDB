package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import jp.nyatla.nyansat.db.basic.BasicTableDefinition;
import jp.nyatla.nyansat.db.basic.PsUtils;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.db.basic.table.BaseTable;
import jp.nyatla.nyansat.utils.SdbException;

/**
 * コイン情報のURLとコインシンボル・名前のペアテーブル。
 */
public class CoinSourceUrlTable extends BaseTable<CoinSourceUrlTable.Item>
{
	public final static int DOMAIN_UNKNOWN=0;
	public final static int DOMAIN_CCT=1;
	public final static int DOMAIN_BCT=2;
	public final static String NAME="cct_sourceurl";
	public final static String DN_symbol="symbol";
	public final static String DN_name="name";
	public final static String DN_url="url";
	public final static String DN_domain="domain";

	private static class CoinSourceUrlTableInfo extends BasicTableDefinition<Item>
	{
		public CoinSourceUrlTableInfo(String i_table_name)
		{
			super(i_table_name);
		}
		@Override
		public String[] getElementNames() {
			return new String[]{DN_symbol,DN_name,DN_domain,DN_url};
		}
		@Override
		public String getCreateStr(){
			return "("+
				DN_symbol+" text,"+
				DN_name+" text,"+
				DN_domain+" integer,"+
				DN_url+" text," +
				"unique("+DN_symbol+","+DN_name+"))";
		}
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
			this._ps_select_by_url.close();
		} catch (SQLException e) {
			throw new SdbException(e);
		}finally{
			super.dispose();
		}
	}

	private PreparedStatement _ps_insert;
	private PreparedStatement _ps_select_by_key;
	private PreparedStatement _ps_select_by_url;
	public CoinSourceUrlTable(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,new CoinSourceUrlTableInfo(i_table_name));
		String[] d=this._table_info.getElementNames();
		try{
			String table_name=this._table_info.getTableName();
			this._ps_insert=this._db.getConnection().prepareStatement(
				String.format(
					"insert or ignore into %s(%s,%s,%s,%s) values(?,?,?,?);",
					table_name,d[0],d[1],d[2],d[3]));
			this._ps_select_by_key=this._db.getConnection().prepareStatement(
				String.format("select * from %s where %s=? AND %s=? AND %s=?;",
				table_name,d[0],d[1],d[2]));
			this._ps_select_by_url=this._db.getConnection().prepareStatement(
					String.format("select * from %s where %s=?;",
					table_name,d[3]));
		}catch (SQLException e){
			throw new SdbException(e);
		}
	}
	public boolean add(String i_symbol,String i_name,int i_domain,String i_url) throws SdbException
	{
		try {
			this._ps_insert.setString(1,i_symbol);
			this._ps_insert.setString(2,i_name);
			this._ps_insert.setInt(3,i_domain);
			PsUtils.setNullableString(this._ps_insert,4,i_url);
			this._ps_insert.execute();
			return this._ps_insert.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	public boolean add(int i_domain,String i_url) throws SdbException
	{
		try {
			this._ps_insert.setString(1,Long.toString(((new Date()).getTime())));
			this._ps_insert.setNull(2,java.sql.Types.CHAR);
			this._ps_insert.setInt(3,i_domain);
			this._ps_insert.setString(4,i_url);
			this._ps_insert.execute();
			Thread.sleep(10);
			return this._ps_insert.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		} catch (InterruptedException e) {
			throw new SdbException(e);
		}
	}
	public boolean isExistByUrl(String url) throws SdbException
	{
		try {
			this._ps_select_by_url.setString(1,url);
			ResultSet rs=this._ps_select_by_url.executeQuery();
			boolean ret=rs.next();
			rs.close();
			return ret;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}	
	public boolean isExist(String symbol, String name,int i_domain) throws SdbException
	{
		try {
			this._ps_select_by_key.setString(1,symbol);
			this._ps_select_by_key.setString(2,name);
			this._ps_select_by_key.setInt(3,i_domain);
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
		public Item(ResultSet rs) throws SdbException
		{
			try {
				this.symbol=rs.getString(1);
				this.name=rs.getString(2);
				this.domain=rs.getInt(3);
				this.url=rs.getString(4);
			} catch (SQLException e) {
				throw new SdbException(e);
			}
		}
		public String symbol;
		public String name;
		public int domain;
		public String url;
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
	}*/	
}