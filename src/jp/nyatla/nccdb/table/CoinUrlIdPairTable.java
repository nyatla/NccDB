package jp.nyatla.nccdb.table;

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
}
