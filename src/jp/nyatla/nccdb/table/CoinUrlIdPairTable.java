package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jp.nyatla.nyansat.db.basic.RowIterable;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.utils.SdbException;

/**
 * id1=coin_id,id2=url_idのペアテーブル
 */
public class CoinUrlIdPairTable extends IdPairTable
{
	private final static String NAME="url_coin_pair";
	private final static String DN_coin_id	="coin_id";
	private final static String DN_url_id	="uri_id";

	public CoinUrlIdPairTable(SqliteDB i_db, String i_table_name) throws SdbException
	{
		super(i_db, i_table_name, DN_coin_id, DN_url_id);
	}
	public CoinUrlIdPairTable(SqliteDB i_db) throws SdbException
	{
		this(i_db,NAME);
	}
	public RowIterable<Item> getItemsByCoinId(Integer i_coin_id) throws SdbException
	{
		return super.getItemsById1(i_coin_id);
	}
	/**
	 * coin_masterから未参照な行を削除する。
	 * @return
	 * @throws SdbException
	 */
	public int deleteNoReferencedRowByCoinId() throws SdbException
	{
		try {
			PreparedStatement ps=this._db.getConnection().prepareStatement(
				String.format(
				"DELETE FROM "+CoinUrlIdPairTable.NAME+" WHERE "+CoinUrlIdPairTable.DN_coin_id+" "+
				"IN (SELECT "+CoinUrlIdPairTable.DN_coin_id+" FROM "+CoinUrlIdPairTable.NAME+" AS A "+
				"LEFT OUTER JOIN "+CoinMasterTable.NAME+" AS B "+
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
		try {
			PreparedStatement ps=this._db.getConnection().prepareStatement(
				String.format(
				"DELETE FROM "+CoinUrlIdPairTable.NAME+" WHERE "+CoinUrlIdPairTable.DN_url_id+" "+
				"IN (SELECT "+CoinUrlIdPairTable.DN_url_id+" FROM "+CoinUrlIdPairTable.NAME+" AS A "+
				"LEFT OUTER JOIN "+ServiceUrlTable.NAME+" AS B "+
				"ON A."+CoinUrlIdPairTable.DN_url_id+"=B."+ServiceUrlTable.DN_id+" WHERE B."+ServiceUrlTable.DN_id+" IS NULL);"));
			int ret=ps.executeUpdate();
			ps.close();
			return ret;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
}
