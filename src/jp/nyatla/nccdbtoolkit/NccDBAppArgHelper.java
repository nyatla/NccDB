package jp.nyatla.nccdbtoolkit;

import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.utils.ArgHelper;
import jp.nyatla.nyansat.utils.SdbException;


public class NccDBAppArgHelper extends ArgHelper
{
	public static final String ENV_NCCDB_DB_PATH="NccDB.sqlite3";
	public static final String ENV_HTML_CACHE_DB_PATH="NccDB_html_cache.sqlite3";
	public NccDBAppArgHelper(String[] s) {
		super(s);
	}
	public SqliteDB getHtmlCache() throws SdbException
	{
		String db_path=this.getString("-cct_db",ENV_HTML_CACHE_DB_PATH);
		if(db_path==null){
			throw new SdbException();
		}
		return new SqliteDB(db_path);
	}
	public SqliteDB getNccDB() throws SdbException
	{
		String db_path=this.getString("-db",ENV_NCCDB_DB_PATH);
		if(db_path==null){
			throw new SdbException();
		}
		return new SqliteDB(db_path);
	}

}
