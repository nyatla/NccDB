package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.nyatla.nyansat.db.basic.BasicTableDefinition;
import jp.nyatla.nyansat.db.basic.RowIterable;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.db.basic.table.BaseTable;
import jp.nyatla.nyansat.utils.SdbException;

/**
 * id1=coin_id,id2=url_idのペアテーブル
 */
public class CoinUrlIdPairTable extends BaseTable<CoinUrlIdPairTable.Item>
{
	public static class TableInfo extends BasicTableDefinition<Item>
	{
		public TableInfo(String i_table_name)
		{
			super(i_table_name);
		}
		@Override
		public String[] getElementNames() {
			return new String[]{DN_coin_id,DN_url_id};
		}
		@Override
		public String getCreateStr(){
			return "("+
				DN_coin_id+" integer,"+
				DN_url_id+" integer,"+
				DN_url_type+ " integer,"+
				"unique("+DN_coin_id+","+DN_url_id+','+DN_url_type+"))";
		}
		@Override
		public Item createRowItem(ResultSet rs) throws SdbException {
			return new Item(rs);
		}		
	}	
	private final static String NAME="url_coin_pair";
	private final static String DN_coin_id	="coin_id";
	private final static String DN_url_id	="uri_id";
	private final static String DN_url_type	="url_type";

	public CoinUrlIdPairTable(SqliteDB i_db, String i_table_name) throws SdbException
	{
		super(i_db,new TableInfo(i_table_name));
		try{
			String table_name=this._table_info.getTableName();
			this._ps_insert=this._db.getConnection().prepareStatement(
				String.format(
					"insert or ignore into %s(%s,%s,%s) values(?,?,?);",
					table_name,DN_coin_id,DN_url_id,DN_url_type));
			this._ps_select=this._db.getConnection().prepareStatement(
				String.format("select * from %s where %s=? and %s=? AND %s=?;",
				table_name,DN_coin_id,DN_url_id,DN_url_type));
			this._ps_delete=this._db.getConnection().prepareStatement(
				String.format("delete from %s where %s=? and %s=? AND %s=?;",
				table_name,DN_coin_id,DN_url_id,DN_url_type));
			this._ps_select_id1=this._db.getConnection().prepareStatement(
					String.format("select * from %s where %s=?;",table_name,DN_coin_id));
		}catch (SQLException e){
			throw new SdbException(e);
		}		
	}
	public CoinUrlIdPairTable(SqliteDB i_db) throws SdbException
	{
		this(i_db,NAME);
	}

	@Override
	public void dispose() throws SdbException
	{
		try {
			this._ps_insert.close();
			this._ps_select.close();
			this._ps_delete.close();
			this._ps_select_id1.close();
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}

	private PreparedStatement _ps_delete;
	private PreparedStatement _ps_insert;
	private PreparedStatement _ps_select;
	private PreparedStatement _ps_select_id1;


	public boolean add(int i_coin_id,int i_url_id,int i_type) throws SdbException
	{
		try {
			this._ps_insert.setInt(1,i_coin_id);
			this._ps_insert.setInt(2,i_url_id);
			this._ps_insert.setInt(3,i_type);
			this._ps_insert.execute();
			return this._ps_insert.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	public boolean delete(int i_coin_id,int i_url_id,int i_type) throws SdbException
	{
		try {
			this._ps_delete.setInt(1,i_coin_id);
			this._ps_delete.setInt(2,i_url_id);
			this._ps_delete.setInt(3,i_type);
			this._ps_delete.execute();
			return this._ps_delete.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}

	public Item getItem(int i_coin_id,int i_url_id,int i_type) throws SdbException
	{
		try{
			ResultSet rs=null;
			try{
				this._ps_select.setInt(1,i_coin_id);
				this._ps_select.setInt(2,i_url_id);
				this._ps_select.setInt(3,i_type);
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
	public boolean isExistItem(int i_coin_id,int i_url_id,int i_type) throws SdbException
	{
		return this.getItem(i_coin_id, i_url_id,i_type)!=null;
	}

	public RowIterable<Item> getItemsByCoinId(Integer i_coin_id) throws SdbException
	{
		try {
			this._ps_select_id1.setInt(1,i_coin_id);
			return new RowIterable<Item>(this._ps_select_id1.executeQuery(),this._table_info);
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}	
	/**
	 * {@link #deleteNoReferencedRowByCoinId(String)}をデフォルトテーブル名で実行します。
	 * @return
	 * @throws SdbException
	 */
	public int deleteNoReferencedRowByCoinId() throws SdbException
	{
		return this.deleteNoReferencedRowByCoinId(CoinMasterTable.NAME);
	}
	/**
	 * coin_masterから未参照な行を削除する。
	 * @return
	 * @throws SdbException
	 */
	public int deleteNoReferencedRowByCoinId(String i_coin_master_table_name) throws SdbException
	{
		try {
			PreparedStatement ps=this._db.getConnection().prepareStatement(
				String.format(
				"DELETE FROM "+this._table_info.getTableName()+" WHERE "+CoinUrlIdPairTable.DN_coin_id+" "+
				"IN (SELECT "+CoinUrlIdPairTable.DN_coin_id+" FROM "+CoinUrlIdPairTable.NAME+" AS A "+
				"LEFT OUTER JOIN "+i_coin_master_table_name+" AS B "+
				"ON A."+CoinUrlIdPairTable.DN_coin_id+"=B."+CoinMasterTable.DN_id+" WHERE B."+CoinMasterTable.DN_id+" IS NULL);"));
			int ret=ps.executeUpdate();
			ps.close();
			return ret;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	public int deleteNoReferencedRowByUriId() throws SdbException
	{
		return this.deleteNoReferencedRowByUriId(ServiceUrlTable.NAME);
	}
	/**
	 * Service_uriから未参照IDを検出して削除する。
	 * @return
	 * @throws SdbException
	 */
	public int deleteNoReferencedRowByUriId(String i_url_table_name) throws SdbException
	{
		try {
			PreparedStatement ps=this._db.getConnection().prepareStatement(
				String.format(
				"DELETE FROM "+this._table_info.getTableName()+" WHERE "+CoinUrlIdPairTable.DN_url_id+" "+
				"IN (SELECT "+CoinUrlIdPairTable.DN_url_id+" FROM "+CoinUrlIdPairTable.NAME+" AS A "+
				"LEFT OUTER JOIN "+i_url_table_name+" AS B "+
				"ON A."+CoinUrlIdPairTable.DN_url_id+"=B."+ServiceUrlTable.DN_id+" WHERE B."+ServiceUrlTable.DN_id+" IS NULL);"));
			int ret=ps.executeUpdate();
			ps.close();
			return ret;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	public static class Item
	{
		public Item(ResultSet rs) throws SdbException
		{
			try{
				this.coin_id=rs.getInt(1);
				this.url_id=rs.getInt(2);
				this.url_type=rs.getInt(3);
			}catch(SQLException e){
				throw new SdbException(e);
			}
		}
		public int coin_id;
		public int url_id;
		public int url_type;
	}	
}
